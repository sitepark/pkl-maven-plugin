package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.pkl.core.Evaluator;
import org.pkl.core.EvaluatorBuilder;
import org.pkl.core.ModuleSource;
import org.pkl.core.SecurityManagers;
import org.pkl.core.StackFrameTransformers;
import org.pkl.core.TestResults;
import org.pkl.core.module.ModuleKeyFactories;
import org.pkl.core.module.ModulePathResolver;
import org.pkl.core.resource.ResourceReaders;

@Mojo(
    name = "test",
    defaultPhase = LifecyclePhase.TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public sealed class TestMojo extends AbstractMojo permits OverwriteMojo {
  private final boolean overwrite;
  private Logger logger;

  /**
   * A globbed path, relative to ${pkl.directory} matching all pkl files to test.
   */
  @Parameter(required = true)
  private String files;

  /**
   * The base directory to search pkl files in via ${pkl.files}.
   */
  @Parameter(defaultValue = "${basedir}")
  private String directory;

  /**
   * A modulepath to use when executing.
   */
  @Parameter private Set<String> modulepath;

  /**
   * Properties to use when executing.
   */
  @Parameter private Map<String, String> properties = Map.of();

  /**
   * Environment variables to use when executing.
   */
  @Parameter private Map<String, String> environmentVariables = Map.of();

  /**
   * Whether to skip execution.
   */
  @Parameter private boolean skip;

  private static final int MAX_DEPTH = 8;

  public TestMojo() {
    this(false);
  }

  protected TestMojo(final boolean overwrite) {
    this.overwrite = overwrite;
  }

  public void execute() throws MojoFailureException, MojoExecutionException {
    if (this.logger == null) {
      this.logger = new Logger(this.getLog());
    }
    if (this.skip) {
      this.logger.executionSkipped();
      return;
    }
    this.logger.beginExecution();
    final Stats stats;
    try (final var modulePathResolver = this.modulePathResolver();
        final var evaluator = this.evaluator(modulePathResolver)) {
      stats =
          Files.walk(Path.of(this.directory), MAX_DEPTH)
              .filter(FileSystems.getDefault().getPathMatcher("glob:" + this.files)::matches)
              .map(file -> this.runTests(evaluator, file))
              .collect(new Stats.SummingCollector());
    } catch (final IOException exception) {
      throw new MojoExecutionException("Failed to read test files", exception);
    }
    if (stats.testsRun() == 0) {
      throw new MojoFailureException("No tests were executed!");
    }
    this.logger.summary(stats);
    switch (stats.levelOfSuccess()) {
      case FAILED -> throw new MojoFailureException("There are test failures.");
      case ERRED -> throw new MojoFailureException("There are test errors.");
    }
  }

  @Override
  public void setLog(final Log log) {
    super.setLog(log);
    this.logger = new Logger(log);
  }

  private final ModulePathResolver modulePathResolver() {
    final Set<Path> modulepath =
        this.modulepath != null
            ? this.modulepath.stream().map(Path::of).collect(Collectors.toSet())
            : Set.of();
    return new ModulePathResolver(modulepath);
  }

  private final Evaluator evaluator(final ModulePathResolver modulePathResolver) {
    return EvaluatorBuilder.unconfigured()
        .setStackFrameTransformer(StackFrameTransformers.defaultTransformer)
        .setAllowedModules(SecurityManagers.defaultAllowedModules)
        .setAllowedResources(SecurityManagers.defaultAllowedResources)
        .addModuleKeyFactory(ModuleKeyFactories.standardLibrary)
        .addModuleKeyFactory(ModuleKeyFactories.modulePath(modulePathResolver))
        .addModuleKeyFactory(ModuleKeyFactories.file)
        .addModuleKeyFactory(ModuleKeyFactories.http)
        .addModuleKeyFactory(ModuleKeyFactories.pkg)
        .addModuleKeyFactory(ModuleKeyFactories.projectpackage)
        .addModuleKeyFactory(ModuleKeyFactories.genericUrl)
        .addResourceReader(ResourceReaders.file())
        .addResourceReader(ResourceReaders.http())
        .addResourceReader(ResourceReaders.https())
        .addResourceReader(ResourceReaders.pkg())
        .addResourceReader(ResourceReaders.projectpackage())
        .addResourceReader(ResourceReaders.modulePath(modulePathResolver))
        .addResourceReader(ResourceReaders.environmentVariable())
        .addResourceReader(ResourceReaders.externalProperty())
        .addEnvironmentVariables(this.environmentVariables)
        .addExternalProperties(this.properties)
        .build();
  }

  private final Stats runTests(final Evaluator evaluator, final Path file) {
    this.logger.runTest(file.toString());
    final long start = System.currentTimeMillis();
    final var results = evaluator.evaluateTest(ModuleSource.path(file), this.overwrite);
    final double secondsElapsed = ((double) (System.currentTimeMillis() - start)) / 1_000;
    final var stats = this.collectTestResults(results, secondsElapsed);
    this.logger.testResult(results.moduleName(), stats);
    return stats;
  }

  private Stats collectTestResults(final TestResults result, final double secondsElapsed) {
    this.logger.testLogs(result.logs());
    final var stats =
        Stats.builder().setTestsRun(result.totalTests()).setSecondsElapsed(secondsElapsed);
    final var error = result.error();
    if (error != null) {
      stats.addError(
          new Stats.Error(
              new Stats.Scope(result.moduleName(), null, null),
              error.message(),
              Stats.Message.fromException(error.exception())));
    }
    this.collectTestSectionResults(result.facts(), result.moduleName(), stats);
    this.collectTestSectionResults(result.examples(), result.moduleName(), stats);
    return stats.build();
  }

  private void collectTestSectionResults(
      final TestResults.TestSectionResults results,
      final String module,
      final Stats.Builder stats) {
    if (!results.failed() && !results.hasError()) {
      return;
    }
    for (final var result : results.results()) {
      final var scope = new Stats.Scope(module, results.name().toString(), result.name());
      for (final var error : result.errors()) {
        stats.addError(
            new Stats.Error(
                scope, error.message(), Stats.Message.fromException(error.exception())));
      }
      for (final var failure : result.failures()) {
        if ("Example Output Written".equals(failure.kind())) {
          stats.addSkipped(
              new Stats.Skipped(
                  scope, "Example Output Written", Stats.Message.fromString(failure.message())));
        } else {
          stats.addFailure(
              new Stats.Failure(
                  scope, failure.message(), Stats.Message.fromString(failure.message())));
        }
      }
    }
  }
}
