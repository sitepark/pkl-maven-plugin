package com.sitepark.maven.plugins.pkl;

import java.nio.file.Path;
import org.apache.maven.plugin.logging.Log;

final class ProjectLogger {
  private final Log log;

  public ProjectLogger(final Log log) {
    this.log = log;
  }

  public void executionSkipped() {
    this.log.info("Project resolution is skipped");
  }

  public void noProject() {
    this.log.info("No PklProject found");
  }

  public void usingProject(final Path projectFile) {
    this.log.debug("Using project " + projectFile);
  }

  public void commandOutput(final String line) {
    this.log.debug(line);
  }

  public void packagedFile(final String file) {
    this.log.info("Writing " + file);
  }

  public void writeFile(final Path file) {
    this.log.info("Writing " + file);
  }
}
