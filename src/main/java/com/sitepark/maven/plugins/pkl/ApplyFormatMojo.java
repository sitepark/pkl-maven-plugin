package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "apply-format", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public final class ApplyFormatMojo extends AbstractFormatMojo {

  public ApplyFormatMojo() {
    super();
  }

  @Override
  protected FormattingResult unformattedFile(
      final Path file, final String contents, final String formatted)
      throws MojoExecutionException {
    try {
      Files.write(
          file,
          formatted.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.WRITE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (final IOException exception) {
      throw new MojoExecutionException(
          "failed to write to '" + file.toAbsolutePath() + "'", exception);
    }
    this.logger.formattedFile(file, contents, formatted);
    return FormattingResult.success(file);
  }
}
