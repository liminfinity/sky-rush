package com.skyrush.rounds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyrush.PostgresSupport;
import com.skyrush.gameconfig.AdminController;
import com.skyrush.gameconfig.GameConfigService;
import com.skyrush.gameconfig.GameConfiguration;
import com.skyrush.gameconfig.GameTheme;
import com.skyrush.gameconfig.PrototypeConfigService;
import com.skyrush.gameconfig.PrototypeConfiguration;
import com.skyrush.gamemath.RandomProvider;
import com.skyrush.shared.GameException;
import com.skyrush.users.DemoUserService;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameplayIntegrationTests extends PostgresSupport {
  static final Path CONFIG = configFile();
  static final Path PROTOTYPE = prototypeFile();

  static Path prototypeFile() {
    try {
      return Files.copy(
          Path.of("../config/prototype-config.yml"),
          Files.createTempDirectory("skyrush-prototype-test").resolve("prototype.yml"));
    } catch (Exception ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  static Path configFile() {
    try {
      return Files.copy(
          Path.of("../config/game-config.yml"),
          Files.createTempDirectory("skyrush-config-test").resolve("game.yml"));
    } catch (Exception ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @DynamicPropertySource
  static void config(DynamicPropertyRegistry registry) {
    registry.add("skyrush.config-path", CONFIG::toString);
    registry.add("skyrush.prototype-config-path", PROTOTYPE::toString);
  }

  static final Instant START = Instant.parse("2026-09-11T12:00:00Z");
  @Autowired com.skyrush.upsell.UpsellService upsell;
  @Autowired com.skyrush.competition.CompetitionService competition;
  @Autowired PrototypeConfigService prototype;
  @Autowired RoundService service;
  @Autowired RoundRepository repository;
  @Autowired JdbcTemplate jdbc;
  @Autowired MutableClock clock;
  @Autowired ScriptedRandom random;
  @Autowired GameConfigService configs;
  @Autowired MockMvc mvc;
  @Autowired org.springframework.web.context.WebApplicationContext web;

  @AfterEach
  void clearAuthentication() {
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
  }

  @Autowired ObjectMapper json;
  @Autowired PlatformTransactionManager transactions;

  @TestConfiguration
  static class Controls {
    @Bean
    @Primary
    MutableClock controlledClock() {
      return new MutableClock();
    }

    @Bean
    @Primary
    ScriptedRandom controlledRandom() {
      return new ScriptedRandom();
    }
  }

  static class MutableClock extends Clock {
    final AtomicReference<Instant> time = new AtomicReference<>(START);

    void at(long millis) {
      time.set(START.plusMillis(millis));
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return time.get();
    }
  }

  static class ScriptedRandom implements RandomProvider {
    final Queue<BigDecimal> values = new ConcurrentLinkedQueue<>();

    void set(String... samples) {
      values.clear();
      for (String sample : samples) values.add(new BigDecimal(sample));
    }

    @Override
    public BigDecimal nextUnit() {
      var value = values.poll();
      return value == null ? new BigDecimal("0.9") : value;
    }
  }

  @BeforeEach
  void reset() throws Exception {
    var auth =
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken
            .authenticated(
                DemoUserService.DEMO_ID.toString(),
                null,
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList(
                    "ROLE_EVALUATOR"));
    org.springframework.security.core.context.SecurityContextHolder.getContext()
        .setAuthentication(auth);
    mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web)
            .apply(
                org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                    .springSecurity())
            .alwaysDo(result -> authenticateDemo())
            .defaultRequest(
                get("/")
                    .with(
                        org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.user(
                                DemoUserService.DEMO_ID.toString())
                            .roles("EVALUATOR"))
                    .with(
                        org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.csrf()))
            .build();
    jdbc.update("DELETE FROM skyrush.wallet_entries");
    jdbc.update("DELETE FROM skyrush.game_rounds");
    jdbc.update("DELETE FROM skyrush.daily_challenge_progress");
    jdbc.update("DELETE FROM skyrush.user_achievements");
    jdbc.update("DELETE FROM skyrush.player_activity_events");
    jdbc.update("UPDATE skyrush.wallets SET balance = 1000.00");
    jdbc.update("UPDATE skyrush.reward_progress SET fragments = 0");
    Files.writeString(CONFIG, Files.readString(Path.of("../config/game-config.yml")));
    Files.writeString(PROTOTYPE, Files.readString(Path.of("../config/prototype-config.yml")));
    configs.reload();
    clock.at(0);
    random.set("0.9", "0");
  }

  RoundService authenticatedService() {
    authenticateDemo();
    return service;
  }

  void authenticateDemo() {
    org.springframework.security.core.context.SecurityContextHolder.getContext()
        .setAuthentication(
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(
                    DemoUserService.DEMO_ID.toString(),
                    null,
                    org.springframework.security.core.authority.AuthorityUtils.createAuthorityList(
                        "ROLE_EVALUATOR")));
  }

  StartRoundRequest request(String bet) {
    return new StartRoundRequest(UUID.randomUUID(), GameTheme.GREEN, bet);
  }

  RoundView start(String bet) {
    return authenticatedService().start(request(bet));
  }

  BigDecimal balance() {
    return authenticatedService().balance().wallet().bonusBalance();
  }

  void code(Runnable action, String expected) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            GameException.class, ex -> assertThat(ex.code()).isEqualTo(expected));
  }

  @Test
  void creationDeductsStakeAndHidesCrashAndSnapshot() throws Exception {
    var r = start("DOUBLE");
    assertThat(r.state()).isEqualTo(RoundState.ACTIVE);
    assertThat(r.booster().level()).isEqualTo(1);
    assertThat(r.currentMultiplier()).isEqualByComparingTo("1");
    assertThat(balance()).isEqualByComparingTo("980");
    assertThat(r.crashMultiplier()).isNull();
    assertThat(r.completedAt()).isNull();
    mvc.perform(get("/api/rounds/" + r.id() + "/state"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.crashMultiplier").isEmpty())
        .andExpect(jsonPath("$.crashAt").doesNotExist())
        .andExpect(jsonPath("$.crashBase").doesNotExist())
        .andExpect(jsonPath("$.configuration").doesNotExist());
  }

  @Test
  void insufficientBalanceDoesNotCreateOrDeduct() {
    jdbc.update("UPDATE skyrush.wallets SET balance = 1");
    code(() -> start("BASIC"), "INSUFFICIENT_BALANCE");
    assertThat(balance()).isEqualByComparingTo("1");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM skyrush.game_rounds", Integer.class))
        .isZero();
  }

  @Test
  void startRetryIsIdempotentAndConflictingReuseRejected() {
    var command = request("BASIC");
    var first = authenticatedService().start(command);
    assertThat(authenticatedService().start(command).id()).isEqualTo(first.id());
    assertThat(balance()).isEqualByComparingTo("990");
    code(
        () ->
            authenticatedService()
                .start(new StartRoundRequest(command.requestId(), GameTheme.RED, "BASIC")),
        "REQUEST_ID_CONFLICT");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM skyrush.wallet_entries WHERE kind='STAKE'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void onlyOneFlightEvenAfterCashout() {
    var r = start("BASIC");
    code(() -> start("BASIC"), "ROUND_IN_PROGRESS");
    clock.at(1000);
    authenticatedService().cashout(r.id());
    code(() -> start("DOUBLE"), "ROUND_IN_PROGRESS");
  }

  @Test
  void themeLevelCountsAndFourBets() throws Exception {
    mvc.perform(get("/api/game/config/public"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.themes.RED.levels").value(12))
        .andExpect(jsonPath("$.themes.GREEN.levels").value(9))
        .andExpect(jsonPath("$.betOptions.length()").value(4))
        .andExpect(jsonPath("$.randomSeed").doesNotExist());
    assertThat(
            authenticatedService()
                .start(new StartRoundRequest(UUID.randomUUID(), GameTheme.RED, "BASIC"))
                .levelThresholds())
        .hasSize(12);
  }

  @Test
  void cashoutRequiresFirstLevel() {
    var r = start("BASIC");
    clock.at(799);
    code(() -> authenticatedService().cashout(r.id()), "LEVEL_ONE_REQUIRED");
    clock.at(800);
    assertThat(authenticatedService().cashout(r.id()).state()).isEqualTo(RoundState.CASHED_OUT);
  }

  @Test
  void successfulCashoutCreditsOnceAndFlightContinues() {
    var r = start("BASIC");
    clock.at(1000);
    var cashout = authenticatedService().cashout(r.id());
    assertThat(cashout.cashoutMultiplier()).isEqualByComparingTo("1.25");
    assertThat(cashout.payout()).isEqualByComparingTo("12.50");
    assertThat(balance()).isEqualByComparingTo("1002.50");
    assertThat(cashout.earnedPoints()).isEqualTo(35);
    code(() -> authenticatedService().cashout(r.id()), "ALREADY_CASHED_OUT");
    clock.at(4000);
    var flying = authenticatedService().state(r.id());
    assertThat(flying.state()).isEqualTo(RoundState.CASHED_OUT);
    assertThat(flying.currentMultiplier()).isEqualByComparingTo("2");
    assertThat(flying.payout()).isEqualByComparingTo("12.50");
    assertThat(flying.cashoutMultiplier()).isEqualByComparingTo("1.25");
    assertThat(flying.crashMultiplier()).isNull();
    assertThat(authenticatedService().history(20, 0).rounds()).isEmpty();
    clock.at(60000);
    var done = authenticatedService().state(r.id());
    assertThat(done.state()).isEqualTo(RoundState.COMPLETED_WIN);
    assertThat(done.payout()).isEqualByComparingTo("12.50");
    assertThat(balance()).isEqualByComparingTo("1002.50");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM skyrush.wallet_entries WHERE kind='PAYOUT'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void crashRejectsCashoutAndPersistsLossDespiteError() {
    random.set("0");
    var r = start("BASIC");
    clock.at(400);
    code(() -> authenticatedService().cashout(r.id()), "ROUND_CRASHED");
    assertThat(
            jdbc.queryForObject(
                "SELECT state FROM skyrush.game_rounds WHERE id=?", String.class, r.id()))
        .isEqualTo("COMPLETED_LOSS");
    var done = authenticatedService().state(r.id());
    assertThat(done.crashMultiplier()).isEqualByComparingTo("1.1");
    assertThat(done.earnedPoints()).isZero();
    assertThat(done.payout()).isZero();
    assertThat(balance()).isEqualByComparingTo("990");
  }

  @Test
  void lossKeepsEarnedPointsAndGrantsReward() {
    var r = start("BASIC");
    clock.at(60000);
    var done = authenticatedService().state(r.id());
    assertThat(done.state()).isEqualTo(RoundState.COMPLETED_LOSS);
    assertThat(done.earnedPoints()).isEqualTo(90);
    assertThat(done.reward().fragmentsGranted()).isEqualTo(1);
    assertThat(authenticatedService().balance().skyFragments()).isEqualTo(1);
  }

  @Test
  void boosterActivationBeforeCashoutAffectsPayoutAndPoints() {
    var r = start("TRIPLE");
    clock.at(1000);
    var cashout = authenticatedService().cashout(r.id());
    assertThat(cashout.booster().active()).isTrue();
    assertThat(cashout.cashoutMultiplier()).isEqualByComparingTo("3.75");
    assertThat(cashout.payout()).isEqualByComparingTo("112.50");
    assertThat(cashout.earnedPoints()).isEqualTo(55);
    clock.at(60000);
    assertThat(authenticatedService().state(r.id()).booster().active()).isTrue();
  }

  @Test
  void boosterNeverActivatesAfterEarlyCashout() {
    random.set("0.9", "0.2");
    var r = start("TRIPLE");
    assertThat(r.booster().level()).isEqualTo(2);
    clock.at(1000);
    var cashout = authenticatedService().cashout(r.id());
    assertThat(cashout.booster().active()).isFalse();
    assertThat(cashout.payout()).isEqualByComparingTo("37.50");
    clock.at(60000);
    var done = authenticatedService().state(r.id());
    assertThat(done.booster().active()).isFalse();
    assertThat(done.payout()).isEqualByComparingTo("37.50");
    assertThat(done.earnedPoints()).isEqualTo(115);
  }

  @Test
  void pollingDoesNotDuplicatePointsOrRewards() {
    var r = start("DOUBLE");
    clock.at(1000);
    for (int i = 0; i < 10; i++)
      assertThat(authenticatedService().state(r.id()).earnedPoints()).isEqualTo(20);
    clock.at(60000);
    for (int i = 0; i < 10; i++)
      assertThat(authenticatedService().state(r.id()).earnedPoints()).isEqualTo(100);
    assertThat(authenticatedService().balance().skyFragments()).isEqualTo(1);
  }

  @Test
  void fragmentProgressionCreditsBonusOnce() {
    jdbc.update("UPDATE skyrush.reward_progress SET fragments=4");
    var r = start("BASIC");
    clock.at(60000);
    var done = authenticatedService().state(r.id());
    assertThat(done.reward().fragmentsRedeemed()).isEqualTo(5);
    assertThat(done.reward().bonusBalanceGranted()).isEqualByComparingTo("5");
    for (int i = 0; i < 5; i++) authenticatedService().state(r.id());
    assertThat(balance()).isEqualByComparingTo("995");
    assertThat(authenticatedService().balance().skyFragments()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM skyrush.wallet_entries WHERE kind='FRAGMENT_BONUS'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void historyFinalizesUnpolledRoundsAndIncludesRequiredFields() throws Exception {
    var r = start("BASIC");
    clock.at(60000);
    mvc.perform(get("/api/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rounds[0].id").value(r.id().toString()))
        .andExpect(jsonPath("$.rounds[0].state").value("COMPLETED_LOSS"))
        .andExpect(jsonPath("$.rounds[0].crashMultiplier").isNumber())
        .andExpect(jsonPath("$.rounds[0].reward.fragmentsGranted").value(1));
  }

  @Test
  void hotReloadAffectsOnlyNewRoundSnapshotsEvenAcrossRepositoryReads() throws Exception {
    var old = start("BASIC");
    Files.writeString(
        CONFIG, Files.readString(CONFIG).replace("points_per_level: 10", "points_per_level: 37"));
    configs.poll();
    clock.at(1000);
    assertThat(authenticatedService().state(old.id()).earnedPoints()).isEqualTo(10);
    clock.at(60000);
    authenticatedService().state(old.id());
    random.set("0.9");
    var next = start("BASIC");
    assertThat(next.configVersion()).isNotEqualTo(old.configVersion());
    assertThat(next.pointsRules().pointsPerLevel()).isEqualTo(37);
    clock.at(61000);
    assertThat(authenticatedService().state(next.id()).earnedPoints()).isEqualTo(37);
    assertThat(authenticatedService().state(old.id()).pointsRules().pointsPerLevel()).isEqualTo(10);
  }

  @Test
  void invalidReloadDoesNotBreakExistingRoundAndBlocksNewStakes() throws Exception {
    var r = start("BASIC");
    Files.writeString(CONFIG, "invalid: [");
    clock.at(1000);
    assertThat(authenticatedService().cashout(r.id()).state()).isEqualTo(RoundState.CASHED_OUT);
    clock.at(60000);
    authenticatedService().state(r.id());
    var before = balance();
    code(() -> start("BASIC"), "CONFIG_INVALID");
    assertThat(balance()).isEqualByComparingTo(before);
  }

  @Test
  void maliciousCalculatedFieldsAndTimestampsAreRejected() throws Exception {
    for (String field :
        List.of(
            "currentMultiplier",
            "crashPoint",
            "boosterPosition",
            "points",
            "payout",
            "timestamp",
            "userId",
            "reward",
            "completedLevel")) {
      String body =
          "{\"requestId\":\""
              + UUID.randomUUID()
              + "\",\"theme\":\"GREEN\",\"betOptionId\":\"BASIC\",\""
              + field
              + "\":99}";
      mvc.perform(post("/api/rounds").contentType("application/json").content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
    var r = start("BASIC");
    clock.at(1000);
    mvc.perform(
            post("/api/rounds/" + r.id() + "/cashout")
                .contentType("application/json")
                .content("{\"payout\":1000000}"))
        .andExpect(status().isBadRequest());
    assertThat(authenticatedService().state(r.id()).state()).isEqualTo(RoundState.ACTIVE);
  }

  @Test
  void validationAndNotFoundAreConsistent() throws Exception {
    mvc.perform(post("/api/rounds").contentType("application/json").content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/rounds/not-a-uuid")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/rounds/" + UUID.randomUUID())).andExpect(status().isNotFound());
    mvc.perform(get("/api/history?limit=101")).andExpect(status().isBadRequest());
    code(() -> start("UNKNOWN"), "INVALID_BET");
  }

  @Test
  void swaggerDescribesThePlayableApi() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/rounds'].post").exists())
        .andExpect(jsonPath("$.paths['/api/rounds/{id}/cashout'].post").exists())
        .andExpect(jsonPath("$.paths['/api/history'].get").exists())
        .andExpect(
            jsonPath("$.components.schemas.StartRoundRequest.properties.requestId.example")
                .exists())
        .andExpect(jsonPath("$.components.schemas.ApiError.properties.code").exists());
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  void concurrentStartRetriesDeductExactlyOnce() throws Exception {
    var request = request("DOUBLE");
    var results = concurrent(12, () -> authenticatedService().start(request));
    assertThat(results.stream().map(RoundView::id).distinct()).hasSize(1);
    assertThat(balance()).isEqualByComparingTo("980");
  }

  @Test
  void concurrentDistinctStartsOnlyCreateOneRound() throws Exception {
    var results =
        concurrent(
            12,
            () -> {
              try {
                authenticatedService().start(request("BASIC"));
                return true;
              } catch (GameException ex) {
                assertThat(ex.code()).isEqualTo("ROUND_IN_PROGRESS");
                return false;
              }
            });
    assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
    assertThat(balance()).isEqualByComparingTo("990");
  }

  @Test
  void concurrentCashoutCreditsOnlyOnce() throws Exception {
    var r = start("BASIC");
    clock.at(1000);
    var results =
        concurrent(
            12,
            () -> {
              try {
                authenticatedService().cashout(r.id());
                return true;
              } catch (GameException ex) {
                assertThat(ex.code()).isEqualTo("ALREADY_CASHED_OUT");
                return false;
              }
            });
    assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
    assertThat(balance()).isEqualByComparingTo("1002.50");
  }

  @Test
  void concurrentSettlementCreditsRewardOnce() throws Exception {
    jdbc.update("UPDATE skyrush.reward_progress SET fragments=4");
    var r = start("BASIC");
    clock.at(60000);
    concurrent(12, () -> authenticatedService().state(r.id()));
    assertThat(balance()).isEqualByComparingTo("995");
    assertThat(authenticatedService().balance().skyFragments()).isZero();
  }

  @Test
  void databaseFailureRollsBackCashoutAndWalletLedgerTogether() {
    var r = start("BASIC");
    clock.at(1000);
    jdbc.execute(
        "ALTER TABLE skyrush.wallet_entries ADD CONSTRAINT test_reject_payout CHECK (kind <> 'PAYOUT')");
    try {
      assertThatThrownBy(() -> authenticatedService().cashout(r.id()))
          .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    } finally {
      jdbc.execute("ALTER TABLE skyrush.wallet_entries DROP CONSTRAINT test_reject_payout");
    }
    assertThat(authenticatedService().state(r.id()).state()).isEqualTo(RoundState.ACTIVE);
    assertThat(balance()).isEqualByComparingTo("990");
    assertThat(authenticatedService().cashout(r.id()).payout()).isEqualByComparingTo("12.50");
  }

  @Test
  void backgroundCompletionDoesNotRequireAnOpenClient() {
    var r = start("BASIC");
    clock.at(60000);
    new RoundCompletionJob(repository, service, clock).completeDueRounds();
    assertThat(
            jdbc.queryForObject(
                "SELECT state FROM skyrush.game_rounds WHERE id=?", String.class, r.id()))
        .isEqualTo("COMPLETED_LOSS");
  }

  @Test
  void demoSeedingDoesNotResetBalance() {
    start("BASIC");
    var seed = new DemoUserService(jdbc, clock, true, new BigDecimal("1000.00"));
    new TransactionTemplate(transactions).executeWithoutResult(s -> seed.run(null));
    assertThat(balance()).isEqualByComparingTo("990");
  }

  @Test
  void delayedCashoutUsesClockAfterObtainingLock() throws Exception {
    random.set("0.1");
    var r = start("BASIC");
    clock.at(1000);
    try (var pool = Executors.newSingleThreadExecutor()) {
      var future = new AtomicReference<Future<?>>();
      new TransactionTemplate(transactions)
          .executeWithoutResult(
              status -> {
                jdbc.queryForObject(
                    "SELECT id FROM skyrush.users WHERE id=? FOR UPDATE",
                    UUID.class,
                    DemoUserService.DEMO_ID);
                future.set(
                    pool.submit(
                        new org.springframework.security.concurrent
                            .DelegatingSecurityContextRunnable(
                            () ->
                                code(
                                    () -> authenticatedService().cashout(r.id()),
                                    "ROUND_CRASHED"))));
                // The held user lock forces processing to wait; then advance the authoritative
                // clock past crash.
                clock.at(60000);
              });
      future.get().get(10, TimeUnit.SECONDS);
    }
    assertThat(balance()).isEqualByComparingTo("990");
  }

  @Test
  void commitmentHiddenUntilCrashAndVerifiableIncludingBooster() throws Exception {
    var r = start("TRIPLE");
    assertThat(r.integrity().commitment()).hasSize(64);
    assertThat(r.integrity().reveal()).isNull();
    clock.at(1000);
    var paid = authenticatedService().cashout(r.id());
    assertThat(paid.integrity().commitment()).isEqualTo(r.integrity().commitment());
    clock.at(60000);
    var done = authenticatedService().state(r.id());
    assertThat(OutcomeProof.hash(done.integrity().reveal())).isEqualTo(r.integrity().commitment());
    assertThat(OutcomeProof.hash(done.integrity().reveal() + "tamper"))
        .isNotEqualTo(r.integrity().commitment());
    assertThat(done.integrity().reveal()).contains("|GREEN|").isNotNull();
  }

  @Test
  void independentProofSaltDoesNotConsumeSeededGameplayDraws() {
    random.set("0.2", "0.4");
    var first = start("TRIPLE");
    clock.at(60000);
    authenticatedService().state(first.id());
    random.set("0.2", "0.4");
    var second = start("TRIPLE");
    clock.at(120000);
    var done = authenticatedService().state(second.id());
    assertThat(second.booster().level()).isEqualTo(first.booster().level());
    assertThat(second.integrity().commitment()).isNotEqualTo(first.integrity().commitment());
    assertThat(done.crashMultiplier())
        .isEqualByComparingTo(authenticatedService().state(first.id()).crashMultiplier());
  }

  @Test
  void activeDiscoveryFindsFlightAndClearsAfterSettlement() throws Exception {
    mvc.perform(get("/api/rounds/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.round").isEmpty());
    var r = start("BASIC");
    mvc.perform(get("/api/rounds/active"))
        .andExpect(jsonPath("$.round.id").value(r.id().toString()));
    clock.at(60000);
    assertThat(authenticatedService().active().state()).isEqualTo(RoundState.COMPLETED_LOSS);
    assertThat(authenticatedService().active()).isNull();
  }

  @Test
  void liveRankingUsesAuthoritativePointsAndDescendingOrder() {
    var r = start("TRIPLE");
    clock.at(1000);
    var next = authenticatedService().state(r.id());
    assertThat(
            next.ranking().entries().stream()
                .filter(e -> e.currentPlayer())
                .findFirst()
                .orElseThrow()
                .points())
        .isEqualTo(next.earnedPoints());
    assertThat(next.ranking().entries().stream().map(e -> e.points()).toList())
        .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(next.ranking().entries())
        .allMatch(
            e ->
                jdbc.queryForObject(
                        "SELECT count(*) FROM skyrush.users WHERE id=? AND password_hash<>'*'",
                        Long.class,
                        UUID.fromString(e.id()))
                    == 1);
  }

  @Test
  void tournamentContainsCurrentPlayerAndMasksDemoNames() {
    var r = start("BASIC");
    clock.at(1000);
    authenticatedService().state(r.id());
    var board = competition.tournament(true);
    assertThat(board.currentPlayer().points()).isEqualTo(10);
    assertThat(board.remainingSeconds()).isPositive();
    assertThat(board.entries().stream().map(e -> e.points()).toList())
        .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(
            board.entries().stream()
                .filter(e -> !e.currentPlayer())
                .allMatch(e -> e.name().endsWith("***")))
        .isTrue();
    assertThat(
            competition.tournament(false).entries().stream()
                .filter(e -> !e.currentPlayer())
                .noneMatch(e -> e.name().contains("*")))
        .isTrue();
  }

  RoundView qualifyingWin() {
    var r = start("TRIPLE");
    clock.at(1000);
    authenticatedService().cashout(r.id());
    clock.at(60000);
    return authenticatedService().state(r.id());
  }

  @Test
  void upsellOnlyQualifyingCompletedWins() {
    UUID session = UUID.randomUUID();
    var active = start("BASIC");
    assertThat(upsell.offer(session, active.id()).offer()).isNull();
    clock.at(60000);
    authenticatedService().state(active.id());
    assertThat(upsell.offer(session, active.id()).offer()).isNull();
    clock.at(0);
    random.set("0.9");
    var small = start("BASIC");
    clock.at(1000);
    authenticatedService().cashout(small.id());
    clock.at(60000);
    authenticatedService().state(small.id());
    assertThat(upsell.offer(session, small.id()).offer()).isNull();
  }

  @Test
  void ticketPurchaseDebitsOnceAndPersistsInventory() {
    var win = qualifyingWin();
    UUID session = UUID.randomUUID();
    var offer = upsell.offer(session, win.id()).offer();
    var before = balance();
    assertThat(offer.quantity()).isEqualTo(2);
    assertThat(offer.total()).isEqualByComparingTo("10");
    var purchased = upsell.decide(offer.id(), session, true);
    assertThat(purchased.tickets()).isEqualTo(2);
    upsell.decide(offer.id(), session, true);
    assertThat(balance()).isEqualByComparingTo(before.subtract(offer.total()));
    assertThat(authenticatedService().state(win.id()).payout()).isEqualByComparingTo(win.payout());
  }

  @Test
  void concurrentTicketPurchaseDebitsOnlyOnce() throws Exception {
    var win = qualifyingWin();
    UUID session = UUID.randomUUID();
    var offer = upsell.offer(session, win.id()).offer();
    var before = balance();
    concurrent(8, () -> upsell.decide(offer.id(), session, true));
    assertThat(balance()).isEqualByComparingTo(before.subtract(offer.total()));
    assertThat(upsell.inventory().tickets()).isEqualTo(offer.quantity());
  }

  @Test
  void ticketStorageFailureRollsBackWalletDebit() {
    var win = qualifyingWin();
    UUID session = UUID.randomUUID();
    var offer = upsell.offer(session, win.id()).offer();
    var before = balance();
    jdbc.execute(
        "ALTER TABLE skyrush.ticket_offers ADD CONSTRAINT reject_test_purchase CHECK(status<>'PURCHASED')");
    try {
      assertThatThrownBy(() -> upsell.decide(offer.id(), session, true))
          .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    } finally {
      jdbc.execute("ALTER TABLE skyrush.ticket_offers DROP CONSTRAINT reject_test_purchase");
    }
    assertThat(balance()).isEqualByComparingTo(before);
    assertThat(upsell.inventory().tickets()).isZero();
  }

  @Test
  void insufficientTicketBalanceDoesNotCreditInventory() {
    var win = qualifyingWin();
    UUID session = UUID.randomUUID();
    var offer = upsell.offer(session, win.id()).offer();
    jdbc.update("UPDATE skyrush.wallets SET balance=1");
    code(() -> upsell.decide(offer.id(), session, true), "INSUFFICIENT_BALANCE");
    assertThat(balance()).isEqualByComparingTo("1");
    assertThat(upsell.inventory().tickets()).isZero();
  }

  @Test
  void offerExpiryDeclineSessionAndDuplicateRoundAreProtected() {
    var win = qualifyingWin();
    UUID session = UUID.randomUUID();
    var offer = upsell.offer(session, win.id()).offer();
    code(() -> upsell.decide(offer.id(), UUID.randomUUID(), true), "OFFER_SESSION");
    upsell.decide(offer.id(), session, false);
    assertThat(upsell.offer(session, win.id()).offer().status()).isEqualTo("DECLINED");
    code(() -> upsell.decide(offer.id(), session, true), "OFFER_CLOSED");
    UUID second = UUID.randomUUID();
    var expired = upsell.offer(second, win.id()).offer();
    clock.at(71000);
    code(() -> upsell.decide(expired.id(), second, true), "OFFER_CLOSED");
    UUID third = UUID.randomUUID();
    var buy = upsell.offer(third, win.id()).offer();
    upsell.decide(buy.id(), third, true);
    assertThat(upsell.offer(UUID.randomUUID(), win.id()).offer()).isNull();
    clock.at(0);
    var next = qualifyingWin();
    assertThat(upsell.offer(session, next.id()).offer()).isNull();
  }

  @Test
  void adminSaveValidatesVersionAndRetainsActiveSnapshot() throws Exception {
    var r = start("BASIC");
    var old = configs.current();
    var c = old.configuration();
    var changed =
        new GameConfiguration(
            c.themes(),
            c.betOptions(),
            c.crashModel(),
            c.growth(),
            new GameConfiguration.Points(37, 25, c.points().boosterActivationBonuses()),
            c.reward());
    mvc.perform(
            put("/api/admin/game-config")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(new AdminController.GameSave(old.version(), changed))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.configuration.points.pointsPerLevel").value(37));
    code(() -> configs.save(old.version(), c), "CONFIG_CONFLICT");
    clock.at(1000);
    assertThat(authenticatedService().state(r.id()).earnedPoints()).isEqualTo(10);
    clock.at(60000);
    authenticatedService().state(r.id());
    assertThat(start("BASIC").pointsRules().pointsPerLevel()).isEqualTo(37);
  }

  @Test
  void adminRejectsInvalidValuesAndCalculatedFields() throws Exception {
    var old = configs.current();
    var tree = json.valueToTree(new AdminController.GameSave(old.version(), old.configuration()));
    ((com.fasterxml.jackson.databind.node.ObjectNode) tree.path("configuration").path("points"))
        .put("pointsPerLevel", -1);
    mvc.perform(
            put("/api/admin/game-config").contentType("application/json").content(tree.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CONFIG_VALIDATION"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) tree.path("configuration").path("points"))
        .put("pointsPerLevel", 1.5);
    mvc.perform(
            put("/api/admin/game-config").contentType("application/json").content(tree.toString()))
        .andExpect(status().isBadRequest());
    assertThat(configs.current().version()).isEqualTo(old.version());
    mvc.perform(
            post("/api/upsell/offers")
                .contentType("application/json")
                .content(
                    "{\"sessionId\":\""
                        + UUID.randomUUID()
                        + "\",\"roundId\":\""
                        + UUID.randomUUID()
                        + "\",\"quantity\":100}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/tournament").contentType("application/json").content("{\"points\":999}"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  void inactiveGamePreservesCurrentCashoutAndPrototypeValidation() {
    var r = start("BASIC");
    var old = prototype.current();
    var c = old.configuration();
    prototype.save(
        old.version(),
        new PrototypeConfiguration(c.gameId(), c.name(), c.type(), false, c.upsell()));
    clock.at(1000);
    authenticatedService().cashout(r.id());
    clock.at(60000);
    authenticatedService().state(r.id());
    code(() -> start("BASIC"), "GAME_INACTIVE");
    var current = prototype.current();
    code(
        () ->
            prototype.save(
                current.version(),
                new PrototypeConfiguration("", c.name(), c.type(), true, c.upsell())),
        "CONFIG_VALIDATION");
  }

  static <T> List<T> concurrent(int count, Callable<T> work) throws Exception {
    try (var pool = Executors.newFixedThreadPool(count)) {
      CountDownLatch gate = new CountDownLatch(1);
      var tasks = new ArrayList<Future<T>>();
      for (int i = 0; i < count; i++)
        tasks.add(
            pool.submit(
                new org.springframework.security.concurrent.DelegatingSecurityContextCallable<>(
                    () -> {
                      gate.await();
                      return work.call();
                    })));
      gate.countDown();
      var values = new ArrayList<T>();
      for (var task : tasks) values.add(task.get(15, TimeUnit.SECONDS));
      return values;
    }
  }
}
