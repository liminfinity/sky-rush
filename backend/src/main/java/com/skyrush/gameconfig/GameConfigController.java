package com.skyrush.gameconfig;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameConfigController {
  private final GameConfigService configs;

  public GameConfigController(GameConfigService configs) {
    this.configs = configs;
  }

  public record PublicTheme(List<java.math.BigDecimal> levelThresholds, int levels) {
    public PublicTheme {
      levelThresholds = List.copyOf(levelThresholds);
    }
  }

  public record PublicConfig(
      String version,
      Map<GameTheme, PublicTheme> themes,
      List<BetOption> betOptions,
      GameConfiguration.Points points,
      GameConfiguration.RewardRules reward) {
    public PublicConfig {
      betOptions = List.copyOf(betOptions);
      themes = Map.copyOf(themes);
    }
  }

  @GetMapping("/config/public")
  @Operation(
      summary = "Read current public game rules",
      description =
          "Existing rounds retain their original rules. Random seeds and hidden round outcomes are never exposed.")
  public PublicConfig config() {
    var snapshot = configs.current();
    var c = snapshot.configuration();
    var themes = new java.util.EnumMap<GameTheme, PublicTheme>(GameTheme.class);
    c.themes()
        .forEach(
            (key, value) ->
                themes.put(
                    key, new PublicTheme(value.levelThresholds(), value.levelThresholds().size())));
    return new PublicConfig(snapshot.version(), themes, c.betOptions(), c.points(), c.reward());
  }

  @GetMapping("/bet-options")
  @Operation(summary = "Read the four available bets")
  public List<BetOption> bets() {
    return configs.current().configuration().betOptions();
  }
}
