package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class ResolveProjectMojoTest {

  private static final Path PROJECT_DIR = Path.of("src/test/resources/pkl/project");
  private static final Path TARGET_DIR = Path.of("target/tests/pkl/project");

  @Test
  public void testResolve() throws IOException, MojoFailureException, MojoExecutionException {
    final var projectDir = this.copyProject();
    final var depsFile = projectDir.resolve("PklProject.deps.json");
    Files.deleteIfExists(depsFile);

    final var log = new CapturingLog();
    final var mojo = new ResolveProjectMojo();
    mojo.projectDir = projectDir.toString();
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);

    Assertions.assertEquals(
        Files.readString(PROJECT_DIR.resolve("PklProject.deps.json")), Files.readString(depsFile));
    Assertions.assertTrue(
        log.captured().contains("Writing " + depsFile.toAbsolutePath().normalize()));
  }

  @Test
  public void testWithoutProject() throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new ResolveProjectMojo();
    mojo.basedir = Path.of("src/test/resources/pkl/tests").toFile();
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    Assertions.assertTrue(log.captured().contains("No PklProject found"));
  }

  private Path copyProject() throws IOException {
    if (Files.exists(TARGET_DIR)) {
      try (final var files = Files.walk(TARGET_DIR)) {
        for (final var file : files.sorted(Comparator.reverseOrder()).toList()) {
          Files.delete(file);
        }
      }
    }
    try (final var files = Files.walk(PROJECT_DIR)) {
      for (final var file : files.toList()) {
        final var target = TARGET_DIR.resolve(PROJECT_DIR.relativize(file));
        if (Files.isDirectory(file)) {
          Files.createDirectories(target);
        } else {
          Files.copy(file, target);
        }
      }
    }
    return TARGET_DIR;
  }
}
