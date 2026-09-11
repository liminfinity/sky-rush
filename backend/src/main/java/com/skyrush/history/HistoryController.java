package com.skyrush.history;

import com.skyrush.rounds.RoundService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class HistoryController {
  private final RoundService rounds;

  public HistoryController(RoundService rounds) {
    this.rounds = rounds;
  }

  @GetMapping("/api/history")
  @Operation(
      summary = "Read completed rounds",
      description =
          "Newest first. Includes wins and losses, fixed payouts, points, and rewards. In-progress cashouts are not yet history.")
  public RoundHistory history(
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int offset) {
    return rounds.history(limit, offset);
  }
}
