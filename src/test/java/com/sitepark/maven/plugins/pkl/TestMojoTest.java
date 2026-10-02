package com.sitepark.maven.plugins.pkl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pkl.core.PklException;

public final class TestMojoTest {

  private static JUnitOptions junit(
      final String reportsDirectory, final boolean aggregate, final String suiteName) {
    final var options = new JUnitOptions();
    options.setReportsDirectory(reportsDirectory);
    options.setAggregate(aggregate);
    options.setSuiteName(suiteName);
    return options;
  }

  private static final String PKL_DIR = "src/test/resources/pkl/tests/";
  private static final String PROJECT_DIR = "src/test/resources/pkl/project/";
  private static final String PROJECT_TEST_DIR = PROJECT_DIR + "tests/";

  @Test
  public void testOutputForSuccess() throws MojoFailureException, MojoExecutionException {
    final var expected =
"""
\\[INFO\\]
\\[INFO\\] -------------------------------------------------------
\\[INFO\\]  T E S T S
\\[INFO\\] -------------------------------------------------------
\\[INFO\\] Running src/test/resources/pkl/tests/succeedingTests\\.pkl
\\[INFO\\] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: \\d+([\\.,]\\d+)?s in com\\.sitepark\\.maven\\.plugins\\.pkl\\.succeedingTests
\\[INFO\\]
\\[INFO\\] Results:
\\[INFO\\]
\\[INFO\\] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.tests = Set.of(PKL_DIR + "succeedingTests.pkl");
    mojo.color = false;
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
\\[ERROR\\] Tests run: 2, Failures: 3, Errors: 0, Skipped: 0, Time elapsed: \\d+([\\.,]\\d+)?s <<< FAILURES! - in com\\.sitepark\\.maven\\.plugins\\.pkl\\.failingTests
\\[ERROR\\]   1 == 2 \\(file://.*src/test/resources/pkl/tests/failingTests\\.pkl\\) <<< FAILURE!
\\[ERROR\\]     │
\\[ERROR\\]     false
\\[INFO\\]
\\[ERROR\\]   2 == 3 \\(file://.*src/test/resources/pkl/tests/failingTests\\.pkl\\) <<< FAILURE!
\\[ERROR\\]     │
\\[ERROR\\]     false
\\[INFO\\]
\\[ERROR\\]   #0: \\(file://.*src/test/resources/pkl/tests/failingTests\\.pkl\\) <<< FAILURE!
\\[ERROR\\]     Expected: \\(file://.*src/test/resources/pkl/tests/failingTests\\.pkl-expected\\.pcf\\)
\\[ERROR\\]     new \\{
\\[ERROR\\]       foo = "baz"
\\[ERROR\\]     \\}
\\[ERROR\\]     Actual: \\(file://.*src/test/resources/pkl/tests/failingTests\\.pkl-actual\\.pcf\\)
\\[ERROR\\]     new \\{
\\[ERROR\\]       foo = "bar"
\\[ERROR\\]     \\}
\\[INFO\\]
\\[INFO\\]
\\[INFO\\] Results:
\\[INFO\\]
\\[ERROR\\] Failures:
\\[ERROR\\]   com\\.sitepark\\.maven\\.plugins\\.pkl\\.failingTests#facts\\["this should fail"\\] » 1 == 2 │ false
\\[ERROR\\]   com\\.sitepark\\.maven\\.plugins\\.pkl\\.failingTests#facts\\["this should fail"\\] » 2 == 3 │ false
\\[ERROR\\]   com\\.sitepark\\.maven\\.plugins\\.pkl\\.failingTests#examples\\["my non-matching example"\\] » #0: Expected: new \\{ foo = "baz" \\} Actual: new \\{ foo = "bar" \\}
\\[INFO\\]
\\[ERROR\\] Tests run: 2, Failures: 3, Errors: 0, Skipped: 0
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.tests = Set.of(PKL_DIR + "failingTests.pkl");
    mojo.color = false;
    mojo.setLog(log);
    Assertions.assertThrows(MojoFailureException.class, mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }

  @Test
  public void testProjectIsFoundBelowBasedir() throws MojoFailureException, MojoExecutionException {
    final var expected =
"""
\\[INFO\\]
\\[INFO\\] -------------------------------------------------------
\\[INFO\\]  T E S T S
\\[INFO\\] -------------------------------------------------------
\\[DEBUG\\] Using project .*/src/test/resources/pkl/project/PklProject
\\[INFO\\] Running src/test/resources/pkl/project/tests/projectTests\\.pkl
\\[INFO\\] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: \\d+([\\.,]\\d+)?s in com\\.sitepark\\.maven\\.plugins\\.pkl\\.projectTests
\\[INFO\\]
\\[INFO\\] Results:
\\[INFO\\]
\\[INFO\\] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
\\[INFO\\]
""";
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.tests = Set.of("tests/projectTests.pkl");
    mojo.basedir = new File(PROJECT_DIR);
    mojo.color = false;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    Assertions.assertLinesMatch(expected.lines(), log.captured().lines());
  }

  @Test
  public void testProjectDirIsUsed() throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.tests = Set.of(PROJECT_TEST_DIR + "projectTests.pkl");
    mojo.projectDir = PROJECT_DIR;
    mojo.color = false;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
  }

  @Test
  public void testWithoutProjectDependenciesCannotBeResolved()
      throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.tests = Set.of("projectTests.pkl");
    mojo.basedir = new File(PROJECT_TEST_DIR);
    mojo.color = false;
    mojo.setLog(log);
    // pkl fails while loading the module, so the exception escapes the test runner
    final var exception = Assertions.assertThrows(PklException.class, mojo::execute);
    Assertions.assertTrue(exception.getMessage().contains("there is no project found"));
  }

  @Test
  public void testMissingProjectDirFails() throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.tests = Set.of(PKL_DIR + "succeedingTests.pkl");
    mojo.projectDir = PKL_DIR;
    mojo.color = false;
    mojo.setLog(log);
    final var exception = Assertions.assertThrows(MojoFailureException.class, mojo::execute);
    Assertions.assertEquals("No PklProject found in '" + PKL_DIR + "'", exception.getMessage());
  }

  @Test
  public void testTestsFromProject() throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.projectDir = PROJECT_DIR;
    mojo.color = false;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    Assertions.assertTrue(log.captured().contains("projectTests.pkl"));
  }

  @Test
  public void testWithoutFilesAndProjectTests()
      throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.color = false;
    mojo.setLog(log);
    final var exception = Assertions.assertThrows(MojoFailureException.class, mojo::execute);
    Assertions.assertEquals(
        "Configure 'tests' or declare 'tests' in a PklProject to select the tests to run.",
        exception.getMessage());
  }

  @Test
  public void testJunitReports() throws Exception {
    final var reports = Path.of("target/tests/pkl/reports");
    Files.deleteIfExists(reports.resolve("com.sitepark.maven.plugins.pkl.succeedingTests.xml"));
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.tests = Set.of(PKL_DIR + "succeedingTests.pkl");
    mojo.junit = junit(reports.toString(), false, "pkl-tests");
    mojo.color = false;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    final var report = reports.resolve("com.sitepark.maven.plugins.pkl.succeedingTests.xml");
    Assertions.assertTrue(Files.exists(report));
    Assertions.assertTrue(
        Files.readString(report)
            .contains(
                "<testsuite name=\"com.sitepark.maven.plugins.pkl.succeedingTests\" tests=\"1\""));
  }

  @Test
  public void testAggregatedJunitReports() throws Exception {
    final var reports = Path.of("target/tests/pkl/aggregated-reports");
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.tests = Set.of(PKL_DIR + "succeedingTests.pkl");
    mojo.junit = junit(reports.toString(), true, "pkl");
    mojo.color = false;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    final var report = reports.resolve("pkl.xml");
    Assertions.assertTrue(Files.exists(report));
    Assertions.assertTrue(Files.readString(report).contains("<testsuites name=\"pkl\""));
  }

  @Test
  public void testWithoutCache() throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new TestMojo();
    mojo.tests = Set.of(PKL_DIR + "succeedingTests.pkl");
    mojo.noCache = true;
    mojo.moduleCacheDir = "target/tests/pkl/cache";
    mojo.color = false;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
  }
}
