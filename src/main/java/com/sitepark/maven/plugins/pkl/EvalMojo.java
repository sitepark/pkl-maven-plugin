package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
import org.pkl.core.module.ModuleKeyFactories;
import org.pkl.core.module.ModulePathResolver;
import org.pkl.core.resource.ResourceReaders;

@Mojo(
    name = "eval",
    defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public final class EvalMojo extends AbstractMojo {
  private EvalLogger logger;

  /**
   * A globbed path, relative to ${pkl.directory} matching all pkl files to evaluate.
   */
  @Parameter(required = true)
  String files;

  /**
   * The base directory to search pkl files in via ${pkl.files}.
   */
  @Parameter(defaultValue = "${basedir}")
  String directory;

  /**
   * The directory where the resulting files are generated to.
   */
  @Parameter(required = true)
  String output;

  /**
   * Whether to overwrite existing files.
   */
  @Parameter(defaultValue = "true")
  boolean overwrite;

  /**
   * A modulepath to use when executing.
   */
  @Parameter Set<String> modulepath;

  /**
   * Properties to use when executing.
   */
  @Parameter Map<String, String> properties = Map.of();

  /**
   * Environment variables to use when executing.
   */
  @Parameter Map<String, String> environmentVariables = Map.of();

  /**
   * Whether to skip execution.
   */
  @Parameter boolean skip;

  private static final int MAX_DEPTH = 8;

  public EvalMojo() {}

  public void execute() throws MojoFailureException, MojoExecutionException {
    if (this.logger == null) {
      this.logger = new EvalLogger(this.getLog());
    }
    if (this.skip) {
      this.logger.executionSkipped();
      return;
    }
    this.logger.beginExecution();
    final Set<Path> files;
    try {
      final var directory = Path.of(this.directory);
      final var globExpression = "glob:" + directory + "/" + this.files;
      files =
          Files.walk(directory, MAX_DEPTH)
              .filter(FileSystems.getDefault().getPathMatcher(globExpression)::matches)
              .collect(Collectors.toSet());
    } catch (final IOException exception) {
      throw new MojoExecutionException("Failed to read pkl files", exception);
    }
    final var statsBuilder = EvalStats.builder();
    try (final var modulePathResolver = this.modulePathResolver();
        final var evaluator = this.evaluator(modulePathResolver)) {
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
    final var output = Paths.get(this.output);
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
