package com.skyrush.rounds;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rounds")
@Tag(name = "Rounds", description = "Server-authoritative gameplay for the demo user")
public class RoundController {
  private final RoundService service;

  public RoundController(RoundService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(
      summary = "Start a round",
      description =
          "Use a fresh requestId for each round; reuse the same ID to retry safely. Only one flight may be in progress.")
  public ResponseEntity<RoundView> start(@Valid @RequestBody StartRoundRequest request) {
    return response(service.start(request));
  }

  public record ActiveRound(RoundView round) {}

  @GetMapping("/active")
  @Operation(
      summary = "Discover the demo user active flight, or null",
      description = "May return a just-completed flight if caught up by this read.")
  public ActiveRound active() {
    return new ActiveRound(service.active());
  }

  public record CashoutRequest() {}

  @PostMapping("/{id}/cashout")
  @Operation(
      summary = "Fix and credit payout",
      description =
          "Available after level 1, strictly before crash, once only. Balloon flight continues. Send no body or {}.")
  public ResponseEntity<RoundView> cashout(
      @PathVariable UUID id, @RequestBody(required = false) CashoutRequest request) {
    return response(service.cashout(id));
  }

  @GetMapping({"/{id}", "/{id}/state"})
  @Operation(
      summary = "Read authoritative round state",
      description =
          "Poll every 250–500 ms. Crash multiplier stays null until crash. Server time determines all transitions.")
  public ResponseEntity<RoundView> state(@PathVariable UUID id) {
    return response(service.state(id));
  }

  private static ResponseEntity<RoundView> response(RoundView view) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view);
  }
}
