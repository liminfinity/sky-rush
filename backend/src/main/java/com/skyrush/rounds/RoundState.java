package com.skyrush.rounds;

public enum RoundState {
  ACTIVE,
  CASHED_OUT,
  COMPLETED_WIN,
  COMPLETED_LOSS;

  public boolean completed() {
    return this == COMPLETED_WIN || this == COMPLETED_LOSS;
  }
}
