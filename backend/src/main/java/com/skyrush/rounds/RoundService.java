package com.skyrush.rounds;

import com.skyrush.gameconfig.GameConfigService;
import com.skyrush.gamemath.GameMath;
import com.skyrush.gamemath.RandomProvider;
import com.skyrush.history.RoundHistory;
import com.skyrush.rewards.Reward;
import com.skyrush.rewards.RewardService;
import com.skyrush.shared.GameException;
import com.skyrush.users.CurrentUserService;
import com.skyrush.wallet.Wallet;
import com.skyrush.wallet.WalletService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(noRollbackFor = GameException.class)
public class RoundService {
  private final com.skyrush.social.AchievementService achievements;
  private final com.skyrush.social.DailyService daily;
  private final com.skyrush.social.ActivityService activity;
  private final CurrentUserService users;
  private final WalletService wallets;
  private final RewardService rewards;
  private final RoundRepository rounds;
  private final com.skyrush.profile.CollectionService collection;
  private final GameConfigService configs;
  private final GameMath math;
  private final RandomProvider random;
  private final Clock clock;
  private final com.skyrush.competition.CompetitionService competition;
  private final com.skyrush.gameconfig.PrototypeConfigService prototype;

  public RoundService(
      CurrentUserService users,
      WalletService wallets,
      RewardService rewards,
      RoundRepository rounds,
      GameConfigService configs,
      GameMath math,
      RandomProvider random,
      Clock clock,
      com.skyrush.competition.CompetitionService competition,
      com.skyrush.gameconfig.PrototypeConfigService prototype,
      com.skyrush.profile.CollectionService collection,
      com.skyrush.social.AchievementService achievements,
      com.skyrush.social.DailyService daily,
      com.skyrush.social.ActivityService activity) {
    this.achievements = achievements;
    this.daily = daily;
    this.activity = activity;
    this.collection = collection;
    this.users = users;
    this.wallets = wallets;
    this.rewards = rewards;
    this.rounds = rounds;
    this.configs = configs;
    this.math = math;
    this.random = random;
    this.clock = clock;
    this.competition = competition;
    this.prototype = prototype;
  }

  public RoundView start(StartRoundRequest request) {
    UUID userId = users.lock().id();
    Instant now = now(); // Capture after lock acquisition: queued requests cannot use stale time.
    var existing = rounds.request(userId, request.requestId());
    if (existing.isPresent()) {
      GameRound r = existing.get();
      if (r.theme != request.theme() || !r.betOptionId.equals(request.betOptionId()))
        throw new GameException(
            409, "REQUEST_ID_CONFLICT", "Request ID was already used with different selections");
      advance(r, now);
      return view(r, now);
    }
    settleActive(userId, now);
    if (rounds.active(userId).isPresent())
      throw new GameException(
          409,
          "ROUND_IN_PROGRESS",
          "Wait for the current balloon to crash before starting another round");
    if (!prototype.current().configuration().active())
      throw new GameException(
          409,
          "GAME_INACTIVE",
          "New rounds are paused by the evaluator. Existing flights remain playable.");
    var snapshot = configs.current();
    var config = snapshot.configuration();
    if (request.theme() == null || !config.themes().containsKey(request.theme()))
      throw new GameException(400, "INVALID_THEME", "Unknown game theme");
    var bet =
        config.betOptions().stream()
            .filter(b -> b.id().equals(request.betOptionId()))
            .findFirst()
            .orElseThrow(() -> new GameException(400, "INVALID_BET", "Unknown bet option"));
    if (wallets.get(userId).bonusBalance().compareTo(bet.stake()) < 0)
      throw new GameException(
          409, "INSUFFICIENT_BALANCE", "Not enough bonus balance for this stake");
    GameRound r = new GameRound();
    r.id = UUID.randomUUID();
    r.userId = userId;
    r.requestId = request.requestId();
    r.theme = request.theme();
    r.betOptionId = bet.id();
    r.stake = bet.stake();
    r.boosterMultiplier = bet.boosterMultiplier();
    r.configVersion = snapshot.version();
    r.configuration = config;
    // Both draws are made before timing starts and before anything is returned to the client.
    r.crashBase = math.crashes.generate(config.crashModel(), random);
    r.boosterLevel =
        math.boosters.generate(
            r.boosterMultiplier, config.themes().get(r.theme).boosterPositionWeights(), random);
    r.startedAt = now();
    r.crashAt =
        math.multipliers.crashAt(r.startedAt, r.crashBase, config.growth().multiplierPerSecond());
    r.proofSalt = OutcomeProof.salt();
    r.proofHash = OutcomeProof.hash(OutcomeProof.payload(r));
    rounds.insert(r);
    wallets.apply(userId, r.id, "STAKE", r.stake.negate(), r.startedAt);
    return view(r, r.startedAt);
  }

  public RoundView active() {
    UUID userId = users.lock().id();
    Instant now = now();
    return rounds
        .active(userId)
        .map(
            r -> {
              advance(r, now);
              return view(r, now);
            })
        .orElse(null);
  }

  public RoundView cashout(UUID id) {
    UUID userId = users.lock().id();
    GameRound r = required(userId, id);
    Instant now = now();
    advance(r, now);
    if (r.state.completed())
      throw new GameException(409, "ROUND_CRASHED", "Cashout is unavailable after crash");
    if (r.state == RoundState.CASHED_OUT)
      throw new GameException(409, "ALREADY_CASHED_OUT", "This round has already been cashed out");
    if (r.completedLevel < 1)
      throw new GameException(409, "LEVEL_ONE_REQUIRED", "Cashout becomes available after level 1");
    r.cashoutAt = now;
    r.cashoutMultiplier = flight(r, now).multiplier();
    r.payout = math.payouts.calculate(r.stake, r.cashoutMultiplier);
    r.state = RoundState.CASHED_OUT;
    r.earnedPoints = flight(r, now).points();
    wallets.apply(userId, r.id, "PAYOUT", r.payout, now);
    rounds.save(r);
    activity.emit(r.userId, r.id, "CASHOUT", r.cashoutMultiplier.toPlainString(), now);
    return view(r, now);
  }

  public RoundView state(UUID id) {
    UUID userId = users.lock().id();
    GameRound r = required(userId, id);
    Instant now = now();
    advance(r, now);
    return view(r, now);
  }

  public record BalanceView(Wallet wallet, long skyFragments) {}

  public BalanceView balance() {
    UUID userId = users.lock().id();
    settleActive(userId, now());
    return new BalanceView(wallets.get(userId), rewards.fragments(userId));
  }

  public RoundHistory history(int limit, int offset) {
    UUID userId = users.lock().id();
    Instant now = now();
    settleActive(userId, now);
    return new RoundHistory(
        rounds.history(userId, limit, offset).stream().map(r -> view(r, now, null)).toList(),
        limit,
        offset);
  }

  /** Background completion uses exactly the same locked transition as polling. */
  public void settle(UUID id) {
    rounds
        .owner(id)
        .ifPresent(
            owner -> {
              users.lock(owner);
              advance(required(owner, id), now());
            });
  }

  private GameRound required(UUID userId, UUID id) {
    return rounds
        .find(userId, id)
        .orElseThrow(() -> new GameException(404, "ROUND_NOT_FOUND", "Round not found"));
  }

  private void settleActive(UUID userId, Instant now) {
    rounds.active(userId).ifPresent(r -> advance(r, now));
  }

  private void advance(GameRound r, Instant now) {
    if (r.state.completed()) return;
    var flight = flight(r, now);
    boolean boosterActivated = !r.boosterActive && flight.boosterActive();
    boolean changed =
        r.completedLevel != flight.level()
            || r.boosterActive != flight.boosterActive()
            || r.earnedPoints != flight.points();
    r.completedLevel = flight.level();
    r.boosterActive = flight.boosterActive();
    r.earnedPoints = flight.points();
    if (flight.crashed()) {
      boolean win = r.cashoutAt != null;
      r.state = win ? RoundState.COMPLETED_WIN : RoundState.COMPLETED_LOSS;
      r.completedAt = r.crashAt;
      var reward = rewards.grant(r.userId, r.id, win, r.configuration.reward(), now);
      r.rewardFragments = reward.fragmentsGranted();
      r.rewardRedeemed = reward.fragmentsRedeemed();
      r.rewardBonus = reward.bonusBalanceGranted();
      changed = true;
    }
    if (changed) {
      rounds.save(r);
      if (boosterActivated)
        activity.emit(r.userId, r.id, "BOOSTER", Integer.toString(r.boosterMultiplier), now);
      if (r.state.completed()) {
        achievements.complete(r);
        daily.complete(r);
        collection.unlockForRound(r.userId, r.id, now);
      }
    }
  }

  private GameMath.Flight flight(GameRound r, Instant now) {
    return math.evaluate(
        r.configuration,
        r.theme,
        r.startedAt,
        r.crashAt,
        r.crashBase,
        r.boosterMultiplier,
        r.boosterLevel,
        r.cashoutAt,
        now);
  }

  private RoundView view(GameRound r, Instant now) {
    return view(r, now, competition.live(r.userId));
  }

  private RoundView view(
      GameRound r, Instant now, com.skyrush.competition.CompetitionService.Ranking ranking) {
    var f = flight(r, now);
    return new RoundView(
        r.id,
        r.theme,
        r.betOptionId,
        r.stake,
        r.state,
        new Booster(r.boosterMultiplier, r.boosterLevel, r.boosterActive),
        f.multiplier(),
        r.completedLevel,
        r.configuration.themes().get(r.theme).levelThresholds(),
        r.earnedPoints,
        r.state == RoundState.ACTIVE && r.completedLevel >= 1,
        r.payout,
        r.cashoutMultiplier,
        r.state.completed() ? f.multiplier() : null,
        r.state.completed() ? new Reward(r.rewardFragments, r.rewardRedeemed, r.rewardBonus) : null,
        r.startedAt,
        r.cashoutAt,
        r.completedAt,
        now,
        r.configVersion,
        r.configuration.points(),
        OutcomeProof.view(r),
        ranking);
  }

  private Instant now() {
    return clock.instant().truncatedTo(ChronoUnit.MILLIS);
  }
}
