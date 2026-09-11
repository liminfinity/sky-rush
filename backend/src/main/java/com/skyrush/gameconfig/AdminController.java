package com.skyrush.gameconfig;

import com.skyrush.users.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final GameConfigService game;
  private final PrototypeConfigService prototype;
  private final CurrentUserService users;

  public AdminController(
      GameConfigService game, PrototypeConfigService prototype, CurrentUserService users) {
    this.game = game;
    this.prototype = prototype;
    this.users = users;
  }

  public record GameSave(@NotNull String version, @NotNull GameConfiguration configuration) {}

  public record PrototypeSave(
      @NotNull String version, @NotNull PrototypeConfiguration configuration) {}

  @GetMapping("/game-config")
  @Operation(summary = "Demo evaluator: read complete game configuration (no random seed)")
  public GameConfigService.Snapshot game() {
    users.get();
    return game.current();
  }

  @PutMapping("/game-config")
  @Operation(
      summary = "Demo evaluator: validate and atomically save game YAML",
      description =
          "Evaluator session required. Expected version prevents overwriting a concurrent admin save. Active round snapshots are unchanged.")
  public GameConfigService.Snapshot save(@Valid @RequestBody GameSave body) {
    users.get();
    return game.save(body.version(), body.configuration());
  }

  @GetMapping("/prototype-config")
  @Operation(summary = "Demo evaluator: read general and simulated-ticket rules")
  public PrototypeConfigService.Snapshot prototype() {
    users.get();
    return prototype.current();
  }

  @PutMapping("/prototype-config")
  @Operation(
      summary = "Demo evaluator: save general and ticket rules; existing offers retain prices")
  public PrototypeConfigService.Snapshot savePrototype(@Valid @RequestBody PrototypeSave body) {
    users.get();
    return prototype.save(body.version(), body.configuration());
  }
}
