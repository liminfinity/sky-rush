package com.skyrush.competition;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournament")
public class CompetitionController {
  private final CompetitionService service;

  public CompetitionController(CompetitionService service) {
    this.service = service;
  }

  @GetMapping({"", "/current/leaderboard"})
  @Operation(
      summary = "Daily UTC shared tournament",
      description = "Real account scores are persisted game points. No monetary prizes.")
  public CompetitionService.Tournament get(@RequestParam(defaultValue = "true") boolean masked) {
    return service.tournament(masked);
  }
}
