package com.skyrush.shared;

/**
 * Expected rejection, only thrown before command mutations (settlement may already be committed).
 */
public class GameException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final int status;
  private final String code;

  public GameException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }
}
