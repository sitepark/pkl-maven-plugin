package com.sitepark.maven.plugins.pkl;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class EvalMojoTest {

  private static final String PKL_DIR = "src/test/resources/pkl/tests/";
  private static final String OUTPUT_DIR = "target/tests/pkl/evaluated/";
  private static final String PROJECT_DIR = "src/test/resources/pkl/project/";

  @Test
  public void testSingleOutputFile() throws MojoFailureException, MojoExecutionException {
    final var expected =
"""
\\[DEBUG\\] Evaluating src/test/resources/pkl/tests/singleOutputFile\\.pkl
\\[INFO\\] Writing target/tests/pkl/evaluated/servers\\.json
\\[INFO\\] Files evaluated: 1, Files created: 1, Time elapsed: \\d+([\\.,]\\d+)?s
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new EvalMojo();
    mojo.modules = Set.of(PKL_DIR + "singleOutputFile.pkl");
    mojo.outputDirectory = OUTPUT_DIR;
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
\\[INFO\\] Files evaluated: 1, Files created: 2, Time elapsed: \\d+([\\.,]\\d+)?s
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new EvalMojo();
    mojo.modules = Set.of(PKL_DIR + "multipleOutputFiles.pkl");
    mojo.outputDirectory = OUTPUT_DIR;
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
    mojo.modules = Set.of(PKL_DIR + "noOutputFiles.pkl");
    mojo.outputDirectory = OUTPUT_DIR;
    mojo.overwrite = true;
    mojo.setLog(log);
    Assertions.assertThrows(MojoFailureException.class, mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }

  @Test
  public void testProjectDependency() throws MojoFailureException, MojoExecutionException {
    final var expected =
"""
\\[DEBUG\\] Using project .*/src/test/resources/pkl/project/PklProject
\\[DEBUG\\] Evaluating src/test/resources/pkl/project/eval/ids\\.pkl
\\[INFO\\] Writing .*/target/tests/pkl/evaluated/ids\\.json
\\[INFO\\] Files evaluated: 1, Files created: 1, Time elapsed: \\d+([\\.,]\\d+)?s
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new EvalMojo();
    mojo.modules = Set.of("eval/ids.pkl");
    mojo.basedir = new File(PROJECT_DIR);
    // absolute, so that the relative basedir of this test does not redirect the output
    mojo.outputDirectory = Path.of(OUTPUT_DIR).toAbsolutePath().toString();
    mojo.overwrite = true;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }
}
