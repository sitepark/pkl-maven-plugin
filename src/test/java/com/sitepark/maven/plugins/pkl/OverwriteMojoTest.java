package com.sitepark.maven.plugins.pkl;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class OverwriteMojoTest {

  private static final String PKL_DIR = "src/test/resources/pkl/tests/";

  @Test
  public void testOutputForSuccess() throws MojoFailureException, MojoExecutionException {
    final var expected =
        """
\\[INFO\\]
\\[INFO\\] -------------------------------------------------------
\\[INFO\\]  T E S T S
\\[INFO\\] -------------------------------------------------------
\\[INFO\\] Running src/test/resources/pkl/tests/writingTests\\.pkl
\\[WARN\\] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: \\d+[\\.,]\\d+s in com\\.sitepark\\.maven\\.plugins\\.pkl\\.writingTests
\\[WARN\\]   \\(file://.*test/resources/pkl/tests/writingTests\\.pkl\\) <<< SKIPPED!
\\[ERROR\\]   Wrote expected output for test this should be written
\\[INFO\\]
\\[INFO\\]
\\[INFO\\] Results:
\\[INFO\\]
\\[WARN\\] Skipped:
\\[WARN\\]   com\\.sitepark\\.maven\\.plugins\\.pkl\\.writingTests#examples\\["this should be written"\\] » Example Output Written
\\[INFO\\]
\\[WARN\\] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new OverwriteMojo();
    mojo.directory = PKL_DIR;
    mojo.files = PKL_DIR + "writingTests.pkl";
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }
}
