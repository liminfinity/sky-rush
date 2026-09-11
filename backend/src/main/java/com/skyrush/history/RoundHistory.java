package com.skyrush.history;

import com.skyrush.rounds.RoundView;
import java.util.List;

/** Completed-round projection; game_rounds is the single source of history. */
public record RoundHistory(List<RoundView> rounds, int limit, int offset) {
  public RoundHistory {
    rounds = List.copyOf(rounds);
  }
}
