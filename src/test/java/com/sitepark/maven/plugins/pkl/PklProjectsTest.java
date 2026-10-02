package com.sitepark.maven.plugins.pkl;

import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class PklProjectsTest {

  private static final Path PROJECT_DIR = Path.of("src/test/resources/pkl/project");

  @Test
  public void testFindsProjectInDirectory() {
    final var found = PklProjects.find(PROJECT_DIR, PROJECT_DIR);
    Assertions.assertEquals(
        PROJECT_DIR.resolve("PklProject").toAbsolutePath().normalize(), found.orElseThrow());
  }

  @Test
  public void testWalksUpToBasedir() {
    final var found = PklProjects.find(PROJECT_DIR.resolve("tests"), PROJECT_DIR);
    Assertions.assertEquals(
        PROJECT_DIR.resolve("PklProject").toAbsolutePath().normalize(), found.orElseThrow());
  }

  @Test
  public void testDoesNotWalkUpBeyondBasedir() {
    final var found = PklProjects.find(PROJECT_DIR.resolve("tests"), PROJECT_DIR.resolve("tests"));
    Assertions.assertTrue(found.isEmpty());
  }

  @Test
  public void testDoesNotWalkUpWithoutBasedir() {
    final var found = PklProjects.find(PROJECT_DIR.resolve("tests"), null);
    Assertions.assertTrue(found.isEmpty());
  }

  @Test
  public void testWithoutProject() {
    final var directory = Path.of("src/test/resources/pkl/tests");
    Assertions.assertTrue(PklProjects.find(directory, directory).isEmpty());
  }

  @Test
  public void testProjectFileOfResolvesAgainstTheGivenDirectory() throws Exception {
    final var projectFile = PklProjects.projectFileOf(PROJECT_DIR.toAbsolutePath());
    Assertions.assertEquals(PROJECT_DIR.toAbsolutePath().resolve("PklProject"), projectFile);
  }
}
