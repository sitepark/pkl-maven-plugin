package com.sitepark.maven.plugins.pkl;

import java.nio.file.Path;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.pkl.core.project.Project;

/**
 * Base class of the goals that evaluate against a {@code PklProject}.
 */
abstract class AbstractProjectMojo extends AbstractPklMojo {

  /**
   * The directory containing the PklProject to use. Defaults to the closest PklProject found
   * between the given search directory and the base directory of the maven project.
   */
  @Parameter(property = "pkl.projectDir")
  String projectDir;

  /**
   * Reports the project in use, so that each goal can log it through its own logger.
   */
  protected abstract void logProject(Path projectFile);

  /**
   * Loads the configured project, or the closest one found from {@code searchStart} upwards.
   * Returns null if there is none.
   */
  protected final Project project(final Path searchStart) throws MojoFailureException {
    final Path projectFile;
    if (this.projectDir != null) {
      projectFile = PklProjects.projectFileOf(this.resolve(this.projectDir));
    } else {
      final var found = PklProjects.find(searchStart, this.basedirPath());
      if (found.isEmpty()) {
        return null;
      }
      projectFile = found.get();
    }
    this.logProject(projectFile);
    return PklProjects.load(projectFile);
  }
}
