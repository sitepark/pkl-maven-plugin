package com.sitepark.maven.plugins.pkl;

import java.net.URI;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class PklEvaluatorsTest {

  private static final Path BASEDIR = Path.of("src/test/resources/pkl/project").toAbsolutePath();

  @Test
  public void testResolvesRelativeModulePathAgainstBasedir() {
    // the entry is relative to the base directory, not to the working directory
    try (final var resolver = PklEvaluators.modulePathResolver(Set.of("dep"), null, BASEDIR)) {
      Assertions.assertTrue(resolver.hasElement(URI.create("modulepath:/a.pkl")));
    }
  }

  @Test
  public void testKeepsAbsoluteModulePathEntries() {
    try (final var resolver =
        PklEvaluators.modulePathResolver(
            Set.of(BASEDIR.resolve("dep").toString()), null, Path.of(""))) {
      Assertions.assertTrue(resolver.hasElement(URI.create("modulepath:/b.pkl")));
    }
  }
}
