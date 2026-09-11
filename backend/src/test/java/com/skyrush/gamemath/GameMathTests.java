package com.skyrush.gamemath;

import static org.assertj.core.api.Assertions.assertThat;

import com.skyrush.gameconfig.GameConfigService;
import com.skyrush.gameconfig.GameConfiguration;
import com.skyrush.gameconfig.GameTheme;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameMathTests {
  final GameMath math = new GameMath();
  final GameConfiguration config =
      new GameConfigService("../config/game-config.yml").current().configuration();
  final Instant start = Instant.parse("2026-09-11T12:00:00Z");

  @Test
  void deterministicRandomMode() {
    var a = new JavaRandomProvider(1234);
    var b = new JavaRandomProvider(1234);
    for (int i = 0; i < 100; i++) {
      assertThat(math.crashes.generate(config.crashModel(), a))
          .isEqualTo(math.crashes.generate(config.crashModel(), b));
      assertThat(
              math.boosters.generate(
                  3, config.themes().get(GameTheme.GREEN).boosterPositionWeights(), a))
          .isEqualTo(
              math.boosters.generate(
                  3, config.themes().get(GameTheme.GREEN).boosterPositionWeights(), b));
    }
    assertThat(new JavaRandomProvider(1).nextUnit())
        .isNotEqualTo(new JavaRandomProvider(2).nextUnit());
  }

  @Test
  void uniformCrashEndpointsAndPrecision() {
    assertThat(math.crashes.generate(config.crashModel(), () -> BigDecimal.ZERO))
        .isEqualByComparingTo("1.1000");
    assertThat(math.crashes.generate(config.crashModel(), () -> new BigDecimal("0.999999")))
        .isEqualByComparingTo("11.9999");
  }

  @Test
  void weightedBoosterAndX1NoDraw() {
    assertThat(
            math.boosters.generate(
                1,
                List.of(),
                () -> {
                  throw new AssertionError();
                }))
        .isNull();
    assertThat(
            math.boosters.generate(
                2,
                List.of(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO),
                () -> new BigDecimal("0.9")))
        .isEqualTo(2);
    assertThat(
            math.boosters.generate(
                4, List.of(BigDecimal.ONE, BigDecimal.ONE), () -> new BigDecimal("0.5")))
        .isEqualTo(2);
  }

  @Test
  void multiplierAndRoundingAreExact() {
    assertThat(math.multipliers.base(start, start.plusMillis(1234), new BigDecimal("0.25")))
        .isEqualByComparingTo("1.3085");
    assertThat(math.payouts.calculate(new BigDecimal("10.01"), new BigDecimal("1.2345")))
        .isEqualByComparingTo("12.35");
    assertThat(math.multipliers.base(start, start.minusSeconds(10), BigDecimal.ONE))
        .isEqualByComparingTo("1");
  }

  @Test
  void crashWinsTieWithLevelAndBooster() {
    Instant crashAt =
        math.multipliers.crashAt(start, new BigDecimal("1.2"), new BigDecimal("0.25"));
    var f =
        math.evaluate(
            config, GameTheme.GREEN, start, crashAt, new BigDecimal("1.2"), 3, 1, null, crashAt);
    assertThat(f.crashed()).isTrue();
    assertThat(f.level()).isZero();
    assertThat(f.boosterActive()).isFalse();
    assertThat(f.points()).isZero();
  }

  @Test
  void cashoutFreezesBoosterEligibilityButLevelsContinue() {
    var f =
        math.evaluate(
            config,
            GameTheme.GREEN,
            start,
            start.plusSeconds(40),
            new BigDecimal("11"),
            3,
            2,
            start.plusSeconds(1),
            start.plusSeconds(8));
    assertThat(f.boosterActive()).isFalse();
    assertThat(f.level()).isEqualTo(4);
    assertThat(f.multiplier()).isEqualByComparingTo("3");
    assertThat(f.points()).isEqualTo(65);
  }

  @Test
  void rewardConvertsFragmentsIntoBonusWithRemainder() {
    var grant = math.rewards.calculate(true, 4, config.reward());
    assertThat(grant.fragmentsGranted()).isEqualTo(2);
    assertThat(grant.fragmentsRedeemed()).isEqualTo(5);
    assertThat(grant.fragmentsRemaining()).isEqualTo(1);
    assertThat(grant.bonus()).isEqualByComparingTo("5.00");
  }

  @Test
  void allMathSourcesAreIndependentOfFrameworkAndPersistence() throws Exception {
    try (var files =
        java.nio.file.Files.list(java.nio.file.Path.of("src/main/java/com/skyrush/gamemath"))) {
      for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = java.nio.file.Files.readString(file);
        assertThat(source)
            .doesNotContain(
                "import org.springframework",
                "import java.sql",
                "import com.skyrush.rounds",
                "import com.skyrush.wallet",
                "import jakarta.persistence");
      }
    }
  }
}
