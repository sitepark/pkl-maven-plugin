package com.sitepark.maven.plugins.pkl;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class TestMojoTest {

  private static final String PKL_DIR = "src/test/resources/pkl/tests/";

  @Test
  public void testOutputForSuccess() throws MojoFailureException, MojoExecutionException {
    final var expected =
        """
\\[INFO\\]
\\[INFO\\] -------------------------------------------------------
\\[INFO\\]  T E S T S
\\[INFO\\] -------------------------------------------------------
\\[INFO\\] Running src/test/resources/pkl/tests/succeedingTests\\.pkl
\\[INFO\\] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: \\d+[\\.,]\\d+s in com\\.sitepark\\.maven\\.plugins\\.pkl\\.succeedingTests
\\[INFO\\]
\\[INFO\\] Results:
\\[INFO\\]
\\[INFO\\] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.directory = PKL_DIR;
    mojo.files = PKL_DIR + "succeedingTests.pkl";
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }

  @Test
  public void testOutputForFailure() throws MojoFailureException, MojoExecutionException {
    final var expected =
        """
\\[INFO\\]
\\[INFO\\] -------------------------------------------------------
\\[INFO\\]  T E S T S
\\[INFO\\] -------------------------------------------------------
\\[INFO\\] Running src/test/resources/pkl/tests/failingTests\\.pkl
\\[ERROR\\] Tests run: 3, Failures: 4, Errors: 0, Skipped: 0, Time elapsed: \\d+[\\.,]\\d+s <<< FAILURES! - in com\\.sitepark\\.maven\\.plugins\\.pkl\\.failingTests
\\[ERROR\\]   1 == 2 \\(file://.*src/test/resources/pkl/tests/failingTests\\.pkl\\) <<< FAILURE!
\\[INFO\\]
\\[ERROR\\]   2 == 3 \\(file://.*src/test/resources/pkl/tests/failingTests\\.pkl\\) <<< FAILURE!
\\[INFO\\]
\\[ERROR\\]   \\(file://.*src/test/resources/pkl/tests/failingTests\\.pkl\\) <<< FAILURE!
\\[ERROR\\]   Output mismatch: "my non-matching example" exists in actual but not in expected output
\\[INFO\\]
\\[ERROR\\]   \\(file://.*src/test/resources/pkl/tests/failingTests.pkl-expected\\.pcf\\) <<< FAILURE!
\\[ERROR\\]   Output mismatch: "my perfectly matching example" exists in expected but not in actual output
\\[INFO\\]
\\[INFO\\]
\\[INFO\\] Results:
\\[INFO\\]
\\[ERROR\\] Failures:
\\[ERROR\\]   com\\.sitepark\\.maven\\.plugins\\.pkl\\.failingTests#facts\\["this should fail"\\] » 1 == 2
\\[ERROR\\]   com\\.sitepark\\.maven\\.plugins\\.pkl\\.failingTests#facts\\["this should fail"\\] » 2 == 3
\\[ERROR\\]   com\\.sitepark\\.maven\\.plugins\\.pkl\\.failingTests#examples\\["my non-matching example"\\] » Output mismatch: "my non-matching example" exists in actual but not in expected output
\\[ERROR\\]   com\\.sitepark\\.maven\\.plugins\\.pkl.failingTests#examples\\["my perfectly matching example"\\] » Output mismatch: "my perfectly matching example" exists in expected but not in actual output
\\[INFO\\]
\\[ERROR\\] Tests run: 3, Failures: 4, Errors: 0, Skipped: 0
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.directory = PKL_DIR;
    mojo.files = PKL_DIR + "failingTests.pkl";
    mojo.setLog(log);
    Assertions.assertThrows(MojoFailureException.class, mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }
}
