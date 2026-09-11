package com.skyrush.gameconfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Same-directory rename: readers never see a partially written admin save. */
final class AtomicConfigWriter {
  static void write(Path path, byte[] bytes) throws IOException {
    Path target = path.toAbsolutePath();
    Path parent = target.getParent();
    if (parent == null)
      throw new IOException("Configuration must be a file, not a filesystem root");
    Path temp = Files.createTempFile(parent, ".skyrush-", ".tmp");
    try {
      Files.write(temp, bytes);
      Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temp);
    }
  }
}
