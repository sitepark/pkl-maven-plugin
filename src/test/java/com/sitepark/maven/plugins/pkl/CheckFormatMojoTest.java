package com.sitepark.maven.plugins.pkl;

import java.nio.file.Paths;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class CheckFormatMojoTest {

  private static final String PKL_DIR = "src/test/resources/pkl/tests/";

  @Test
  public void testUnformatted() throws MojoFailureException, MojoExecutionException {
    final var expected =
        """
\\[ERROR\\] Error in src/test/resources/pkl/tests/unformatted.pkl
\\[ERROR\\]  2 [ ]
\\[ERROR\\]  3 -        local example = new Dynamic \\{
\\[ERROR\\]  4 -foo = "bar"
\\[ERROR\\]  3 \\+local example = new Dynamic \\{
\\[ERROR\\]  4 \\+  foo = "bar"
\\[ERROR\\]  5  \\}
\\[ERROR\\] \\.\\.\\.
\\[ERROR\\] 11  local example3 = new Dynamic \\{
\\[ERROR\\] 12 -  foo =
\\[ERROR\\] 13 -    "bar"
\\[ERROR\\] 12 \\+  foo = "bar"
\\[ERROR\\] 14  \\}
\\[ERROR\\]
""";
    final var log = new CapturingLog();
    final var mojo = new CheckFormatMojo();
    mojo.grammarVersion = "latest";
    mojo.paths = Set.of(Paths.get(PKL_DIR).resolve("unformatted.pkl").toString());
    mojo.setLog(log);
    Assertions.assertThrows(MojoFailureException.class, mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }

  @Test
  public void testFormatted() throws MojoFailureException, MojoExecutionException {
    final var expected = "";
    final var log = new CapturingLog();
    final var mojo = new CheckFormatMojo();
    mojo.grammarVersion = "latest";
    mojo.paths = Set.of(Paths.get(PKL_DIR).resolve("formatted.pkl").toString());
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }

  @Test
  public void testNonExistent() throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new CheckFormatMojo();
    mojo.grammarVersion = "latest";
    mojo.paths = Set.of(Paths.get(PKL_DIR).resolve("nonexistent.pkl").toString());
    mojo.setLog(log);
    Assertions.assertThrows(MojoExecutionException.class, mojo::execute);
  }
}
