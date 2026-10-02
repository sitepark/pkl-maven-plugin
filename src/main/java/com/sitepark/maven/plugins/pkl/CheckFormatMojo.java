package com.sitepark.maven.plugins.pkl;

import java.nio.file.Path;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "check-format", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public final class CheckFormatMojo extends AbstractFormatMojo {

  /**
   * Whether to skip execution.
   */
  @Parameter(property = "pkl.check-format.skip", defaultValue = "false")
  boolean skip;

  public CheckFormatMojo() {
    super();
  }

  @Override
  protected boolean isSkipConfigured() {
    return this.skip;
  }

  @Override
  protected FormattingResult unformattedFile(
      final Path file, final String contents, final String formatted) {
    this.logger.invalidFile(file, contents, formatted);
    return FormattingResult.failure(file);
  }
}
