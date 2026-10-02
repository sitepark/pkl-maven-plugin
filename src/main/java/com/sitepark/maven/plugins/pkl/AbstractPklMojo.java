package com.sitepark.maven.plugins.pkl;

import java.io.File;
import java.nio.file.Path;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Base class of all goals, holding what every goal needs to know about the maven project.
 */
abstract class AbstractPklMojo extends AbstractMojo {

  /**
   * The base directory of the maven project. Relative paths are resolved against it.
   */
  @Parameter(defaultValue = "${basedir}", readonly = true)
  File basedir;

  /**
   * Whether to skip the execution of every pkl goal.
   */
  @Parameter(property = "pkl.skip", defaultValue = "false")
  boolean skipAll;

  protected final Path basedirPath() {
    return this.basedir != null ? this.basedir.toPath() : Path.of("");
  }

  /**
   * Resolves a configured path against the base directory. Absolute paths are kept as they are.
   */
  protected final Path resolve(final String path) {
    return this.basedirPath().resolve(path).normalize();
  }

  protected final boolean skipped(final boolean goalSkip) {
    return goalSkip || this.skipAll;
  }
}
