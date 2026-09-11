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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public final class PrototypeConfigService {
  private final Path path;
  private final ObjectMapper yaml =
      new ObjectMapper(new YAMLFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION))
          .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
          .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
          .enable(
              DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
              DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
              DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);

  public record Snapshot(String version, PrototypeConfiguration configuration) {}

  public PrototypeConfigService(
      @Value("${skyrush.prototype-config-path:../config/prototype-config.yml}") String path) {
    this.path = Path.of(path);
    current();
  }

  public synchronized Snapshot current() {
    try {
      byte[] bytes = Files.readAllBytes(path);
      var c = yaml.readValue(bytes, PrototypeConfiguration.class);
      c.validate();
      return new Snapshot(
          HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)), c);
    } catch (java.io.IOException | java.security.NoSuchAlgorithmException | RuntimeException ex) {
      throw new GameException(503, "CONFIG_INVALID", "Prototype configuration: " + ex.getMessage());
    }
  }

  public synchronized Snapshot save(String version, PrototypeConfiguration c) {
    if (!current().version().equals(version))
      throw new GameException(
          409, "CONFIG_CONFLICT", "Prototype settings changed. Reload before saving.");
    try {
      c.validate();
    } catch (RuntimeException ex) {
      throw new GameException(
          400,
          "CONFIG_VALIDATION",
          ex.getMessage() == null ? "All prototype fields are required" : ex.getMessage());
    }
    try {
      AtomicConfigWriter.write(path, yaml.writeValueAsBytes(c));
    } catch (java.io.IOException | SecurityException ex) {
      throw new GameException(
          503,
          "CONFIG_WRITE_FAILED",
          "Cannot save prototype configuration. Check directory permissions.");
    }
    return current();
  }
}
