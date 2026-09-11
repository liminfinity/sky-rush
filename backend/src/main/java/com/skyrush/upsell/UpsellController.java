package com.skyrush.upsell;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/upsell")
public class UpsellController {
  private final UpsellService service;

  public UpsellController(UpsellService service) {
    this.service = service;
  }

  public record OfferRequest(@NotNull UUID sessionId, @NotNull UUID roundId) {}

  public record Decision(@NotNull UUID sessionId, @NotNull Boolean accept) {}

  @PostMapping("/offers")
  @Operation(
      summary = "Request one qualifying simulated offer per browser session",
      description =
          "No client prices, quantities or payout accepted. An offer is persisted before display; retrying cannot extend its deadline.")
  public UpsellService.View offer(@Valid @RequestBody OfferRequest body) {
    return service.offer(body.sessionId(), body.roundId());
  }

  @PostMapping("/offers/{id}/decision")
  @Operation(
      summary = "Accept or decline simulated tickets",
      description =
          "Purchased tickets are stored, without real draw/payment integration. Purchase retry is idempotent.")
  public UpsellService.View decide(@PathVariable UUID id, @Valid @RequestBody Decision body) {
    return service.decide(id, body.sessionId(), body.accept());
  }

  @GetMapping("/tickets")
  @Operation(summary = "Read persisted simulated ticket inventory")
  public UpsellService.View tickets() {
    return service.inventory();
  }
}
