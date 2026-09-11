package com.skyrush.gameconfig;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.skyrush.shared.GameException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public final class GameConfigService {
  private static final Logger LOG = LoggerFactory.getLogger(GameConfigService.class);
  private final Path path;
  private final ObjectMapper yaml =
      new ObjectMapper(new YAMLFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION))
          .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
          .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
  private Snapshot snapshot;
  private String error;

  public record Snapshot(String version, GameConfiguration configuration) {}

  public GameConfigService(@Value("${skyrush.config-path}") String path) {
    this.path = Path.of(path);
    reload(); // Invalid or missing initial config fails application startup.
  }

  /** Check on every new-round request so a saved file applies immediately. */
  public synchronized Snapshot current() {
    try {
      reload();
    } catch (RuntimeException ex) {
      throw new GameException(
          503, "CONFIG_INVALID", "Game configuration unavailable: " + ex.getMessage());
    }
    return snapshot;
  }

  public synchronized void reload() {
    try {
      byte[] bytes = Files.readAllBytes(path);
      String version = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      if (snapshot == null || !snapshot.version().equals(version)) {
        GameConfiguration candidate = yaml.readValue(bytes, GameConfiguration.class);
        candidate.validate();
        snapshot = new Snapshot(version, candidate);
        LOG.info("Loaded game configuration version {}", version);
      }
      error = null;
    } catch (java.io.IOException | java.security.NoSuchAlgorithmException | RuntimeException ex) {
      String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
      if (!message.equals(error)) LOG.error("Game configuration rejected: {}", message);
      error = message;
      throw new IllegalArgumentException(message, ex);
    }
  }

  public synchronized Snapshot save(String expectedVersion, GameConfiguration candidate) {
    if (!current().version().equals(expectedVersion))
      throw new GameException(
          409, "CONFIG_CONFLICT", "Configuration changed. Reload before saving.");
    try {
      candidate.validate();
    } catch (RuntimeException ex) {
      throw new GameException(
          400,
          "CONFIG_VALIDATION",
          ex.getMessage() == null ? "All configuration fields are required" : ex.getMessage());
    }
    try {
      AtomicConfigWriter.write(path, yaml.writeValueAsBytes(candidate));
    } catch (java.io.IOException | SecurityException ex) {
      throw new GameException(
          503,
          "CONFIG_WRITE_FAILED",
          "Cannot save configuration. Check directory write permissions.");
    }
    return current();
  }

  @Scheduled(fixedDelayString = "${skyrush.config-reload-ms:1000}")
  public void poll() {
    try {
      reload();
    } catch (IllegalArgumentException ignored) {
      /* Logged; existing rounds retain valid snapshots. */
    }
  }
}
