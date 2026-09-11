package com.skyrush.wallet;

import com.skyrush.rounds.RoundService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletController {
  private final RoundService rounds;

  public WalletController(RoundService rounds) {
    this.rounds = rounds;
  }

  @GetMapping("/api/wallet")
  @Operation(
      summary = "Read demo balance and Sky Fragments",
      description =
          "Settles any due round first. Wallet bonus units are separate from game points and fragments.")
  public RoundService.BalanceView wallet() {
    return rounds.balance();
  }
}
