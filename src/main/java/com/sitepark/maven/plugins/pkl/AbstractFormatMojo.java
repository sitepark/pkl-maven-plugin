package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.pkl.formatter.Formatter;
import org.pkl.formatter.GrammarVersion;

abstract class AbstractFormatMojo extends AbstractPklMojo {
  protected FormatLogger logger;

  /**
   * The pkl sources to format, as files or directories containing them.
   */
  @Parameter(required = true)
  Set<String> sources;

  /**
   * The patterns excluded from the configured directories.
   */
  @Parameter List<String> excludes = PklFiles.DEFAULT_EXCLUDES;

  /**
   * The grammar compatibility version to use:
   * 1:      0.25 - 0.29
   * 2:      0.30+
   * latest: 0.30+ (default)
   */
  @Parameter(property = "pkl.grammarVersion", defaultValue = "latest")
  String grammarVersion;

  private static final class UncheckedMojoExecutionException extends RuntimeException {
    private final MojoExecutionException exception;

    UncheckedMojoExecutionException(final MojoExecutionException exception) {
      this.exception = exception;
    }

    MojoExecutionException getChecked() {
      return this.exception;
    }
  }

  protected static record FormattingResult(Path file, boolean success) {

    static FormattingResult success(final Path file) {
      return new FormattingResult(file, true);
    }

    static FormattingResult failure(final Path file) {
      return new FormattingResult(file, false);
    }
  }

  /**
   * Tells whether this goal's own skip parameter is set.
   */
  protected abstract boolean isSkipConfigured();

  protected AbstractFormatMojo() {
    this.logger = new FormatLogger(this.getLog());
  }

  @Override
  public void execute() throws MojoFailureException, MojoExecutionException {
    if (this.skipped(this.isSkipConfigured())) {
      this.logger.executionSkipped();
      return;
    }
    this.logger.beginExecution();

    final var grammarVersion =
        switch (this.grammarVersion) {
          case "1" -> GrammarVersion.V1;
          case "2" -> GrammarVersion.V2;
          case "latest" -> GrammarVersion.latest();
          case final String v ->
              throw new MojoFailureException(
                  "Invalid grammar version '" + v + "'. expected '1', '2' or 'latest'");
        };
    final var formatter = new Formatter(grammarVersion);
    final Map<Boolean, List<Path>> results;
    try {
      results =
          this.allFiles()
              .map(file -> this.formatFile(file, formatter))
              .collect(
                  Collectors.groupingBy(
                      FormattingResult::success,
                      Collectors.mapping(FormattingResult::file, Collectors.toList())));
    } catch (final UncheckedMojoExecutionException exception) {
      throw exception.getChecked();
    }

    if (results.containsKey(false)) {
      throw new MojoFailureException("There are formatting errors.");
    }
  }

  @Override
  public void setLog(final Log log) {
    super.setLog(log);
    this.logger = new FormatLogger(log);
  }

  private Stream<Path> allFiles() {
    try {
      return PklFiles.collect(
          this.sources, PklFiles.PKL_FILES_AND_PROJECTS, this.excludes, this.basedirPath())
          .stream();
    } catch (final IOException exception) {
      throw new UncheckedMojoExecutionException(
          new MojoExecutionException("failed to read the pkl sources", exception));
    }
  }

  private FormattingResult formatFile(final Path file, final Formatter formatter) {
    final String contents;
    try {
      contents = Files.readString(file);
    } catch (final IOException exception) {
      throw new UncheckedMojoExecutionException(
          new MojoExecutionException("failed to read '" + file.toAbsolutePath() + "'", exception));
    }
    final String formatted;
    try {
      // can throw (atleast) a NoSuchFileException
      formatted = formatter.format(contents);
    } catch (final Throwable exception) {
      throw new UncheckedMojoExecutionException(
          new MojoExecutionException(
              "error during formatting '" + file.toAbsolutePath() + "'", exception));
    }
    if (formatted.equals(contents)) {
      return FormattingResult.success(file);
    }
    try {
      return this.unformattedFile(file, contents, formatted);
    } catch (final MojoExecutionException exception) {
      throw new UncheckedMojoExecutionException(exception);
    }
  }

  protected abstract FormattingResult unformattedFile(Path file, String contents, String formatted)
      throws MojoExecutionException;
}
