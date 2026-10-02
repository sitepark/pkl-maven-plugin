package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.pkl.core.Evaluator;
import org.pkl.core.module.ModulePathResolver;
import org.pkl.core.project.Project;

/**
 * Base class of the goals that evaluate pkl modules.
 */
abstract class AbstractEvaluatingMojo extends AbstractProjectMojo {

  /**
   * The patterns excluded from the configured directories.
   */
  @Parameter List<String> excludes = PklFiles.DEFAULT_EXCLUDES;

  /**
   * A modulepath to use when executing. Takes precedence over the modulePath declared by the
   * evaluator settings of the PklProject.
   */
  @Parameter Set<String> modulePath;

  /**
   * External properties to use when executing, read via the `prop:` scheme.
   */
  @Parameter Map<String, String> externalProperties = Map.of();

  /**
   * Environment variables to use when executing, read via the `env:` scheme.
   */
  @Parameter Map<String, String> env = Map.of();

  /**
   * The directory to cache downloaded packages in. Defaults to pkl's module cache directory.
   */
  @Parameter String moduleCacheDir;

  /**
   * Whether to disable the module cache.
   */
  @Parameter(defaultValue = "false")
  boolean noCache;

  /**
   * Exists only to be disabled by tests.
   */
  boolean color = true;

  /**
   * The directory to start the search for a PklProject at: the first configured entry, or the
   * base directory if there is none.
   */
  protected final Path searchStart(final Collection<String> entries) {
    if (entries == null || entries.isEmpty()) {
      return this.basedirPath();
    }
    final var first = this.basedirPath().resolve(entries.iterator().next()).normalize();
    if (java.nio.file.Files.isDirectory(first)) {
      return first;
    }
    return first.getParent() != null ? first.getParent() : this.basedirPath();
  }

  protected final Set<Path> collect(final Collection<String> entries, final String what)
      throws MojoExecutionException {
    try {
      return PklFiles.collect(entries, PklFiles.PKL_FILES, this.excludes, this.basedirPath());
    } catch (final IOException exception) {
      throw new MojoExecutionException("Failed to read " + what, exception);
    }
  }

  protected final ModulePathResolver modulePathResolver(final Project project) {
    return PklEvaluators.modulePathResolver(this.modulePath, project, this.basedirPath());
  }

  protected final Evaluator evaluator(
      final ModulePathResolver modulePathResolver, final Project project) {
    return PklEvaluators.create(
        modulePathResolver,
        project,
        this.env,
        this.externalProperties,
        this.moduleCacheDir != null ? this.resolve(this.moduleCacheDir) : null,
        this.noCache,
        this.color);
  }
}
