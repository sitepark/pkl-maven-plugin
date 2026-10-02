package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.pkl.core.Evaluator;
import org.pkl.core.ModuleSource;
import org.pkl.core.TestResults;
import org.pkl.core.project.Project;
import org.pkl.core.stdlib.test.report.JUnitReporter;

@Mojo(
    name = "test",
    defaultPhase = LifecyclePhase.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public sealed class TestMojo extends AbstractEvaluatingMojo permits OverwriteMojo {
  private final boolean overwrite;
  private TestLogger logger;

  /**
   * The test modules to run, as files or directories containing them. Defaults to the tests
   * declared by the PklProject.
   */
  @Parameter Set<String> tests;

  /**
   * The JUnit XML report options.
   */
  @Parameter JUnitOptions junit;

  /**
   * Whether to skip execution.
   */
  @Parameter(property = "pkl.test.skip", defaultValue = "false")
  boolean skip;

  public TestMojo() {
    this(false);
  }

  protected TestMojo(final boolean overwrite) {
    this.overwrite = overwrite;
  }

  @Override
  public void execute() throws MojoFailureException, MojoExecutionException {
    if (this.logger == null) {
      this.logger = new TestLogger(this.getLog());
    }
    if (this.skipped(this.skip)) {
      this.logger.executionSkipped();
      return;
    }
    this.logger.beginExecution();
    final var project = this.project(this.searchStart(this.tests));
    // searching files and running tests cannot be done in the same stream as
    // the tests may delete `mytest.pkl-actual.pcf` files.
    final var files = this.testFiles(project);
    final var results = new ArrayList<TestResults>();
    final TestStats stats;
    try (final var modulePathResolver = this.modulePathResolver(project);
        final var evaluator = this.evaluator(modulePathResolver, project)) {
      stats =
          files.stream()
              .map(file -> this.runTests(evaluator, file, results))
              .collect(new TestStats.SummingCollector());
    }
    this.writeJunitReports(results);
    if (stats.testsRun() == 0) {
      throw new MojoFailureException("No tests were executed!");
    }
    this.logger.summary(stats);
    switch (stats.levelOfSuccess()) {
      case FAILED -> throw new MojoFailureException("There are test failures.");
      case ERRED -> throw new MojoFailureException("There are test errors.");
      default -> {}
    }
  }

  @Override
  public void setLog(final Log log) {
    super.setLog(log);
    this.logger = new TestLogger(log);
  }

  @Override
  protected void logProject(final Path projectFile) {
    this.logger.usingProject(projectFile);
  }

  private Set<Path> testFiles(final Project project)
      throws MojoExecutionException, MojoFailureException {
    if (this.tests != null && !this.tests.isEmpty()) {
      return this.collect(this.tests, "test files");
    }
    final var declared = project != null ? project.getTests() : List.<Path>of();
    if (declared.isEmpty()) {
      throw new MojoFailureException(
          "Configure 'tests' or declare 'tests' in a PklProject to select the tests to run.");
    }
    return new LinkedHashSet<>(declared);
  }

  private void writeJunitReports(final List<TestResults> results) throws MojoExecutionException {
    if (this.junit == null || this.junit.reportsDirectory == null) {
      return;
    }
    final var reporter = new JUnitReporter(this.junit.suiteName);
    final var directory = Path.of(this.junit.reportsDirectory);
    try {
      Files.createDirectories(directory);
      if (this.junit.aggregate) {
        final var file = directory.resolve(this.junit.suiteName + ".xml");
        try (final var writer = Files.newBufferedWriter(file)) {
          reporter.summarize(results, writer);
        }
        this.logger.writeReport(file);
      } else {
        for (final var result : results) {
          final var file = directory.resolve(result.moduleName() + ".xml");
          try (final var writer = Files.newBufferedWriter(file)) {
            reporter.report(result, writer);
          }
          this.logger.writeReport(file);
        }
      }
    } catch (final IOException exception) {
      throw new MojoExecutionException("Failed to write junit reports to " + directory, exception);
    }
  }

  private final TestStats runTests(
      final Evaluator evaluator, final Path file, final List<TestResults> collected) {
    this.logger.runTest(file.toString());
    final long start = System.currentTimeMillis();
    final var results = evaluator.evaluateTest(ModuleSource.path(file), this.overwrite);
    collected.add(results);
    final double secondsElapsed = ((double) (System.currentTimeMillis() - start)) / 1_000;
    final var stats = this.collectTestResults(results, secondsElapsed);
    this.logger.testResult(results.moduleName(), stats);
    return stats;
  }

  private TestStats collectTestResults(final TestResults result, final double secondsElapsed) {
    this.logger.testLogs(result.logs());
    final var stats =
        TestStats.builder().setTestsRun(result.totalTests()).setSecondsElapsed(secondsElapsed);
    final var error = result.error();
    if (error != null) {
      stats.addError(
          new TestStats.Error(
              new TestStats.Scope(result.moduleName(), null, null),
              error.message(),
              TestStats.Message.fromException(error.exception())));
    }
    this.collectTestSectionResults(result.facts(), result.moduleName(), stats);
    this.collectTestSectionResults(result.examples(), result.moduleName(), stats);
    return stats.build();
  }

  private void collectTestSectionResults(
      final TestResults.TestSectionResults results,
      final String module,
      final TestStats.Builder stats) {
    if (!results.failed() && !results.hasError()) {
      return;
    }
    for (final var result : results.results()) {
      final var scope = new TestStats.Scope(module, results.name().toString(), result.name());
      for (final var error : result.errors()) {
        final var message = TestStats.Message.fromException(error.exception());
        final var description =
            Optional.ofNullable(error.message())
                .map(this::formatFailureMessage)
                .orElseGet(message::firstLine);
        stats.addError(new TestStats.Error(scope, description, message));
      }
      for (final var failure : result.failures()) {
        if ("Example Output Written".equals(failure.kind())) {
          stats.addSkipped(
              new TestStats.Skipped(
                  scope,
                  "Example Output Written",
                  TestStats.Message.fromString(failure.message())));
        } else {
          stats.addFailure(
              new TestStats.Failure(
                  scope,
                  this.formatFailureMessage(failure.message()),
                  TestStats.Message.fromString(failure.message())));
        }
      }
    }
  }

  private String formatFailureMessage(final String message) {
    return message
        .lines()
        .map(
            line ->
                // remove ANSI escape sequences and the file location
                line.replaceAll("\\x1B\\[[\\d;]{1,5}m", "")
                    .replaceFirst("\\s*\\(file://[^\\)]+\\)\\s*", " ")
                    .trim())
        .filter(Predicate.not(String::isEmpty))
        .collect(Collectors.joining(" "));
  }
}
