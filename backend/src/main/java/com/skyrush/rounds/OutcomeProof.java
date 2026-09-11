package com.skyrush.rounds;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Commitment only, not a fairness certification. Salt never uses the seeded gameplay RNG. */
public final class OutcomeProof {
  private static final SecureRandom SALTS = new SecureRandom();

  private OutcomeProof() {}

  public record View(String algorithm, String commitment, String reveal) {}

  public static String salt() {
    byte[] bytes = new byte[32];
    SALTS.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  public static String payload(GameRound r) {
    return "skyrush-v1|"
        + r.id
        + "|"
        + r.theme
        + "|"
        + r.crashBase.setScale(4).toPlainString()
        + "|"
        + r.boosterMultiplier
        + "|"
        + (r.boosterLevel == null ? "none" : r.boosterLevel)
        + "|"
        + r.configVersion
        + "|"
        + r.startedAt
        + "|"
        + r.proofSalt;
  }

  public static String hash(String payload) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static View view(GameRound r) {
    return r.proofHash == null
        ? null
        : new View("SHA-256", r.proofHash, r.state.completed() ? payload(r) : null);
  }
}
