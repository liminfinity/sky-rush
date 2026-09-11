package com.skyrush.gameconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameConfigTests {
  @TempDir Path directory;

  String source() throws Exception {
    return Files.readString(Path.of("../config/game-config.yml"));
  }

  @Test
  void rehearsalPresetIsValidAndDoesNotReplaceDefaults() {
    var demo = new GameConfigService("../config/game-demo.yml").current().configuration();
    assertThat(demo.crashModel().minMultiplier()).isEqualByComparingTo("10.00");
    assertThat(demo.themes().get(GameTheme.RED).boosterPositionWeights().getFirst())
        .isEqualByComparingTo("1");
    var normal = new GameConfigService("../config/game-config.yml").current().configuration();
    assertThat(normal.crashModel().minMultiplier()).isEqualByComparingTo("1.10");
  }

  @Test
  void validConfigAndImmutableThemes() throws Exception {
    Path file = directory.resolve("game.yml");
    Files.writeString(file, source());
    var config = new GameConfigService(file.toString()).current().configuration();
    assertThat(config.themes().get(GameTheme.RED).levelThresholds()).hasSize(12);
    assertThat(config.themes().get(GameTheme.GREEN).levelThresholds()).hasSize(9);
    assertThat(config.betOptions()).hasSize(4);
    assertThatThrownBy(() -> config.betOptions().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void invalidConfigurationsFailClearly() throws Exception {
    for (String invalid :
        new String[] {
          source().replace("points_per_level: 10", "points_per_level: -1"),
          source().replace("points_per_level: 10", "points_per_level: 1.5"),
          source().replace("points_per_level: 10", "points_per_leveel: 10"),
          source().replace("multiplier_per_second: 0.25", "multiplier_per_second: 0"),
          source().replace("min_multiplier: 1.10", "min_multiplier: 15"),
          source().replace("fragments_per_bonus: 5", "fragments_per_bonus: 0"),
          source().replace("stake: 10.00", "stake: 10.001"),
          source().replace("booster_multiplier: 4", "booster_multiplier: 8"),
          source().replace("1.2, 1.5, 1.8", "1.2, 1.1, 1.8"),
          source().replace("1.2, 1.5, 1.8,", "1.2, 1.5,"),
          source().replace("points_per_level: 10", "points_per_level: null"),
          source().replace("  points_per_level: 10\n", ""),
          source() + "\nunknown: value\n"
        }) {
      Path file = directory.resolve("invalid.yml");
      Files.writeString(file, invalid);
      assertThatThrownBy(() -> new GameConfigService(file.toString()))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void reloadIsAtomicAndRecoverable() throws Exception {
    Path file = directory.resolve("game.yml");
    Files.writeString(file, source());
    var service = new GameConfigService(file.toString());
    var original = service.current();
    Files.writeString(file, source().replace("points_per_level: 10", "points_per_level: 37"));
    try (var pool = Executors.newFixedThreadPool(8)) {
      var calls =
          java.util.stream.IntStream.range(0, 30)
              .mapToObj(i -> pool.submit(service::current))
              .toList();
      for (var call : calls)
        assertThat(call.get().configuration().points().pointsPerLevel()).isEqualTo(37);
    }
    assertThat(original.configuration().points().pointsPerLevel()).isEqualTo(10);
    assertThat(service.current().version()).isNotEqualTo(original.version());
    Files.writeString(file, "broken: [");
    service.poll();
    assertThatThrownBy(service::current).isInstanceOf(com.skyrush.shared.GameException.class);
    Files.writeString(file, source());
    assertThat(service.current().version()).isEqualTo(original.version());
  }
}
