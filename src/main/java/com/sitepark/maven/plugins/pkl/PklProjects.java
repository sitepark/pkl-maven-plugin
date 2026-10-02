package com.sitepark.maven.plugins.pkl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.plugin.MojoFailureException;
import org.pkl.core.PklException;
import org.pkl.core.module.ProjectDependenciesManager;
import org.pkl.core.project.Project;

/**
 * Locates and loads {@code PklProject} files, mirroring how the pkl CLI resolves a project.
 */
final class PklProjects {
  private PklProjects() {}

  /**
   * Searches for a {@code PklProject} file, starting at {@code directory} and walking up to
   * {@code basedir}. Only {@code directory} itself is examined if {@code basedir} is null or is
   * not an ancestor of {@code directory}.
   */
  public static Optional<Path> find(final Path directory, final Path basedir) {
    final var start = directory.toAbsolutePath().normalize();
    final var boundary = basedir != null ? basedir.toAbsolutePath().normalize() : null;
    for (var candidate = start; candidate != null; candidate = candidate.getParent()) {
      final var projectFile = candidate.resolve(ProjectDependenciesManager.PKL_PROJECT_FILENAME);
      if (Files.isRegularFile(projectFile)) {
        return Optional.of(projectFile);
      }
      if (boundary == null || candidate.equals(boundary) || !candidate.startsWith(boundary)) {
        break;
      }
    }
    return Optional.empty();
  }

  /**
   * Loads the given {@code PklProject} file.
   */
  public static Project load(final Path projectFile) throws MojoFailureException {
    try {
      return Project.loadFromPath(projectFile);
    } catch (final PklException exception) {
      throw new MojoFailureException("Failed to load " + projectFile, exception);
    }
  }

  /**
   * Returns the {@code PklProject} file of the given project directory, which has to exist.
   */
  public static Path projectFileOf(final String projectDir) throws MojoFailureException {
    final var projectFile =
        Path.of(projectDir).resolve(ProjectDependenciesManager.PKL_PROJECT_FILENAME);
    if (!Files.isRegularFile(projectFile)) {
      throw new MojoFailureException(
          "No "
              + ProjectDependenciesManager.PKL_PROJECT_FILENAME
              + " found in '"
              + projectDir
              + "'");
    }
    return projectFile;
  }
}
