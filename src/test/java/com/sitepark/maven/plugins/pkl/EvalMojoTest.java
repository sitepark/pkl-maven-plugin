package com.sitepark.maven.plugins.pkl;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class EvalMojoTest {

  private static final String PKL_DIR = "src/test/resources/pkl/tests/";
  private static final String OUTPUT_DIR = "target/tests/pkl/evaluated/";

  @Test
  public void testSingleOutputFile() throws MojoFailureException, MojoExecutionException {
    final var expected =
        """
\\[DEBUG\\] Evaluating src/test/resources/pkl/tests/singleOutputFile\\.pkl
\\[INFO\\] Writing target/tests/pkl/evaluated/servers\\.json
\\[INFO\\] Files evaluated: 1, Files created: 1, Time elapsed: \\d+[\\.,]\\d+s
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new EvalMojo();
    mojo.directory = PKL_DIR;
    mojo.files = "singleOutputFile.pkl";
    mojo.output = OUTPUT_DIR;
    mojo.overwrite = true;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }

  @Test
  public void testMultipleOutputFiles() throws MojoFailureException, MojoExecutionException {
    final var expected =
        """
\\[DEBUG\\] Evaluating src/test/resources/pkl/tests/multipleOutputFiles\\.pkl
\\[INFO\\] Writing target/tests/pkl/evaluated/servers\\.yaml
\\[INFO\\] Writing target/tests/pkl/evaluated/servers\\.xml
\\[INFO\\] Files evaluated: 1, Files created: 2, Time elapsed: \\d+[\\.,]\\d+s
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new EvalMojo();
    mojo.directory = PKL_DIR;
    mojo.files = "multipleOutputFiles.pkl";
    mojo.output = OUTPUT_DIR;
    mojo.overwrite = true;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }

  @Test
  public void testNoOutputFiles() throws MojoFailureException, MojoExecutionException {
    final var expected =
        """
\\[DEBUG\\] Evaluating src/test/resources/pkl/tests/noOutputFiles\\.pkl
\\[WARN\\] No output files defined in src/test/resources/pkl/tests/noOutputFiles.pkl
""";
    final var log = new CapturingLog();
    final var mojo = new EvalMojo();
    mojo.directory = PKL_DIR;
    mojo.files = "noOutputFiles.pkl";
    mojo.output = OUTPUT_DIR;
    mojo.overwrite = true;
    mojo.setLog(log);
    Assertions.assertThrows(MojoFailureException.class, mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }
}
