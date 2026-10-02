package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.pkl.core.PklException;
import org.pkl.core.SecurityManagers;
import org.pkl.core.StackFrameTransformers;
import org.pkl.core.http.HttpClient;
import org.pkl.core.module.ProjectDependenciesManager;
import org.pkl.core.packages.PackageLoadError;
import org.pkl.core.project.ProjectPackager;

@Mojo(name = "package-project", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public final class PackageProjectMojo extends AbstractProjectMojo {
  private ProjectLogger logger;

  /**
   * The path to write the package to. Supports the placeholders %{name} and %{version}.
   */
  @Parameter(defaultValue = "${project.build.directory}/pkl-packages/%{name}@%{version}")
  String outputPath;

  /**
   * Whether to skip checking if the package has already been published with different contents.
   */
  @Parameter(defaultValue = "false")
  boolean skipPublishCheck;

  /**
   * Whether to skip execution.
   */
  @Parameter(property = "pkl.package-project.skip", defaultValue = "false")
  boolean skip;

  /**
   * Exists only to be disabled by tests.
   */
  boolean color = true;

  public PackageProjectMojo() {}

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
    final var projectFile =
        project.getProjectDir().resolve(ProjectDependenciesManager.PKL_PROJECT_FILENAME);
    final var output = new StringWriter();
    try (final var httpClient = HttpClient.builder().build()) {
      new ProjectPackager(
              List.of(project),
              this.basedirPath(),
              this.outputPath,
              StackFrameTransformers.defaultTransformer,
              this.color,
              SecurityManagers.defaultManager,
              httpClient,
              this.skipPublishCheck,
              output)
          .createPackages();
    } catch (final IOException exception) {
      throw new MojoExecutionException("Failed to create the package", exception);
    } catch (final PklException | PackageLoadError exception) {
      throw new MojoFailureException("Failed to package " + projectFile, exception);
    } finally {
      output.toString().lines().forEach(this.logger()::packagedFile);
    }
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
