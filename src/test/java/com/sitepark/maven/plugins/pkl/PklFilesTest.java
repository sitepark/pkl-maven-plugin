package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class PklFilesTest {

  private static final Path BASEDIR = Path.of("").toAbsolutePath();
  private static final String PROJECT_DIR = "src/test/resources/pkl/project";

  @Test
  public void testCollectsDirectoryRecursively() throws IOException {
    final var files = collect(PROJECT_DIR + "/dep");
    Assertions.assertEquals(
        Set.of(
            BASEDIR.resolve(PROJECT_DIR + "/dep/a.pkl"),
            BASEDIR.resolve(PROJECT_DIR + "/dep/b.pkl")),
        files);
  }

  @Test
  public void testCollectsPklProjectsOfADirectoryWhenFormatting() throws IOException {
    final var files =
        PklFiles.collect(
            Set.of(PROJECT_DIR),
            PklFiles.PKL_FILES_AND_PROJECTS,
            PklFiles.DEFAULT_EXCLUDES,
            BASEDIR);
    Assertions.assertTrue(files.contains(BASEDIR.resolve(PROJECT_DIR + "/PklProject")));
    Assertions.assertTrue(files.contains(BASEDIR.resolve(PROJECT_DIR + "/dep/PklProject")));
  }

  @Test
  public void testCollectsSingleFileRegardlessOfTheIncludedPatterns() throws IOException {
    final var files = collect(PROJECT_DIR + "/PklProject");
    Assertions.assertEquals(Set.of(BASEDIR.resolve(PROJECT_DIR + "/PklProject")), files);
  }

  @Test
  public void testResolvesAbsoluteEntries() throws IOException {
    final var files = collect(BASEDIR.resolve(PROJECT_DIR + "/dep").toString());
    Assertions.assertEquals(2, files.size());
  }

  @Test
  public void testCollectsGlobPattern() throws IOException {
    final var files = collect(PROJECT_DIR + "/**/*.pkl");
    Assertions.assertEquals(
        Set.of(
            BASEDIR.resolve(PROJECT_DIR + "/dep/a.pkl"),
            BASEDIR.resolve(PROJECT_DIR + "/dep/b.pkl"),
            BASEDIR.resolve(PROJECT_DIR + "/eval/ids.pkl"),
            BASEDIR.resolve(PROJECT_DIR + "/tests/projectTests.pkl")),
        files);
  }

  @Test
  public void testGlobPatternWithoutMatches() throws IOException {
    Assertions.assertTrue(collect(PROJECT_DIR + "/**/*Test.pkl").isEmpty());
  }

  @Test
  public void testGlobPatternWithMissingRoot() {
    Assertions.assertThrows(
        NoSuchFileException.class, () -> collect(PROJECT_DIR + "/nonexistent/**/*.pkl"));
  }

  @Test
  public void testExcludes() throws IOException {
    final var files =
        PklFiles.collect(
            Set.of(PROJECT_DIR + "/**/*.pkl"), PklFiles.PKL_FILES, List.of("**/dep/**"), BASEDIR);
    Assertions.assertEquals(
        Set.of(
            BASEDIR.resolve(PROJECT_DIR + "/eval/ids.pkl"),
            BASEDIR.resolve(PROJECT_DIR + "/tests/projectTests.pkl")),
        files);
  }

  @Test
  public void testMissingEntry() {
    Assertions.assertThrows(NoSuchFileException.class, () -> collect(PROJECT_DIR + "/nonexistent"));
  }

  private static Set<Path> collect(final String entry) throws IOException {
    return PklFiles.collect(Set.of(entry), PklFiles.PKL_FILES, PklFiles.DEFAULT_EXCLUDES, BASEDIR);
  }
}
