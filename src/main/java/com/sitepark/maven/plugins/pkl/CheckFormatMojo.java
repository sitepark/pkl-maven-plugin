package com.sitepark.maven.plugins.pkl;

import java.nio.file.Path;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "check-format", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public final class CheckFormatMojo extends AbstractFormatMojo {

  public CheckFormatMojo() {
    super();
  }

  @Override
  protected FormattingResult unformattedFile(
      final Path file, final String contents, final String formatted) {
    this.logger.invalidFile(file, contents, formatted);
    return FormattingResult.failure(file);
  }
}
