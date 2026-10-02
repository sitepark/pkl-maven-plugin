package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.pkl.core.Evaluator;
import org.pkl.core.ModuleSource;

@Mojo(
    name = "eval",
    defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public final class EvalMojo extends AbstractEvaluatingMojo {
  private EvalLogger logger;

  /**
   * The modules to evaluate, as files or directories containing them.
   */
  @Parameter(required = true)
  Set<String> modules;

  /**
   * The directory where the resulting files are generated to.
   */
  @Parameter(required = true)
  String outputDirectory;

  /**
   * Whether to overwrite existing files.
   */
  @Parameter(defaultValue = "true")
  boolean overwrite;

  /**
   * Whether to skip execution.
   */
  @Parameter(property = "pkl.eval.skip", defaultValue = "false")
  boolean skip;

  public EvalMojo() {}

  @Override
  public void execute() throws MojoFailureException, MojoExecutionException {
    if (this.logger == null) {
      this.logger = new EvalLogger(this.getLog());
    }
    if (this.skipped(this.skip)) {
      this.logger.executionSkipped();
      return;
    }
    this.logger.beginExecution();
    final var project = this.project(this.searchStart(this.modules));
    final var files = this.collect(this.modules, "pkl files");
    final var statsBuilder = EvalStats.builder();
    try (final var modulePathResolver = this.modulePathResolver(project);
        final var evaluator = this.evaluator(modulePathResolver, project)) {
      for (final var file : files) {
        statsBuilder.addAll(this.evalFile(evaluator, file));
      }
    }
    final var stats = statsBuilder.build();
    if (stats.filesCreated() == 0) {
      throw new MojoFailureException("No files were evaluated!");
    }
    this.logger.summary(stats);
  }

  @Override
  public void setLog(final Log log) {
    super.setLog(log);
    this.logger = new EvalLogger(log);
  }

  @Override
  protected void logProject(final Path projectFile) {
    this.logger.usingProject(projectFile);
  }

  private final EvalStats evalFile(final Evaluator evaluator, final Path file)
      throws MojoExecutionException {
    this.logger.evalFile(file);
    final long start = System.currentTimeMillis();
    final var results = evaluator.evaluateOutputFiles(ModuleSource.path(file));
    if (results.isEmpty()) {
      final double secondsElapsed = ((double) (System.currentTimeMillis() - start)) / 1_000;
      this.logger.noFilesWritten(file);
      return EvalStats.builder()
          .setFilesEvaluated(1)
          .setFilesCreated(0)
          .setSecondsElapsed(secondsElapsed)
          .build();
    }
    final var output = this.resolve(this.outputDirectory);
    for (final var result : results.entrySet()) {
      final var outputFile = output.resolve(result.getKey());
      try {
        this.writeFile(outputFile, result.getValue().getText());
      } catch (final IOException exception) {
        throw new MojoExecutionException("Failed to write " + outputFile, exception);
      }
    }
    final double secondsElapsed = ((double) (System.currentTimeMillis() - start)) / 1_000;
    return EvalStats.builder()
        .setFilesEvaluated(1)
        .setFilesCreated(results.size())
        .setSecondsElapsed(secondsElapsed)
        .build();
  }

  private void writeFile(final Path file, final String text) throws IOException {
    if (Files.exists(file) && !this.overwrite) {
      this.logger.writeFileSkipped(file);
      return;
    }
    this.logger.writeFile(file);
    final var parent = file.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.write(
        file,
        text.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE);
  }
}
