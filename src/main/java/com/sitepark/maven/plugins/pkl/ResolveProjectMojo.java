package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.pkl.core.SecurityManagers;
import org.pkl.core.http.HttpClient;
import org.pkl.core.module.ProjectDependenciesManager;
import org.pkl.core.packages.PackageResolver;
import org.pkl.core.project.ProjectDependenciesResolver;
import org.pkl.core.util.IoUtils;

@Mojo(name = "resolve-project", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public final class ResolveProjectMojo extends AbstractProjectMojo {
  private ProjectLogger logger;

  /**
   * The directory to cache downloaded packages in. Defaults to pkl's module cache directory.
   */
  @Parameter String moduleCacheDir;

  /**
   * Whether to skip execution.
   */
  @Parameter(property = "pkl.resolve-project.skip", defaultValue = "false")
  boolean skip;

  @Override
  public void execute() throws MojoFailureException, MojoExecutionException {
    if (this.skipped(this.skip)) {
      this.logger().executionSkipped();
      return;
    }
    final var project = this.project(this.basedirPath());
    if (project == null) {
      this.logger().noProject();
      return;
    }
    final var cacheDir =
        this.moduleCacheDir != null
            ? Path.of(this.moduleCacheDir)
            : IoUtils.getDefaultModuleCacheDir();
    final var output = new StringWriter();
    final var depsFile =
        project.getProjectDir().resolve(ProjectDependenciesManager.PKL_PROJECT_DEPS_FILENAME);
    try (final var httpClient = HttpClient.builder().build();
        final var packageResolver =
            PackageResolver.getInstance(SecurityManagers.defaultManager, httpClient, cacheDir)) {
      final var dependencies =
          new ProjectDependenciesResolver(project, packageResolver, output).resolve();
      try (final var out = Files.newOutputStream(depsFile)) {
        dependencies.writeTo(out);
      }
    } catch (final IOException exception) {
      throw new MojoExecutionException("Failed to write " + depsFile, exception);
    } finally {
      output.toString().lines().forEach(this.logger()::commandOutput);
    }
    this.logger().writeFile(depsFile);
  }

  @Override
  protected void logProject(final Path projectFile) {
    this.logger().usingProject(projectFile);
  }

  private ProjectLogger logger() {
    if (this.logger == null) {
      this.logger = new ProjectLogger(this.getLog());
    }
    return this.logger;
  }

  @Override
  public void setLog(final Log log) {
    super.setLog(log);
    this.logger = new ProjectLogger(log);
  }
}
