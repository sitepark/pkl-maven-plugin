package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.pkl.formatter.Formatter;
import org.pkl.formatter.GrammarVersion;

abstract class AbstractFormatMojo extends AbstractMojo {
  protected FormatLogger logger;

  /**
   * Paths containing pkl files or directories to format/check recursively.
   */
  @Parameter(required = true)
  Set<String> paths;

  /**
   * The grammar compatibility version to use:
   * 1:      0.25 - 0.29
   * 2:      0.30+
   * latest: 0.30+ (default)
   */
  @Parameter(property = "pkl.format.grammarVersion", defaultValue = "latest")
  String grammarVersion;

  /**
   * Whether to skip execution.
   */
  @Parameter(property = "pkl.format.skip", defaultValue = "false")
  boolean skip;

  private static final int MAX_DEPTH = 8;

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

  protected AbstractFormatMojo() {
    this.logger = new FormatLogger(this.getLog());
  }

  public void execute() throws MojoFailureException, MojoExecutionException {
    if (this.skip) {
      this.logger.executionSkipped();
      return;
    }
    this.logger.beginExecution();

    final var formatter = new Formatter();
    final var grammarVersion =
        switch (this.grammarVersion) {
          case "1" -> GrammarVersion.V1;
          case "2" -> GrammarVersion.V2;
          case "latest" -> GrammarVersion.latest();
          case final String v ->
              throw new MojoFailureException(
                  "Invalid grammar version '" + v + "'. expected '1', '2' or 'latest'");
        };
    final Map<Boolean, List<Path>> results;
    try {
      results =
          this.allFiles()
              .map(file -> this.formatFile(file, formatter, grammarVersion))
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
    final var pathMatcher = FileSystems.getDefault().getPathMatcher("regex:^.+\\.(pkl|pcf)$");
    return this.paths.stream()
        .map(e -> Paths.get(e))
        .flatMap(
            file -> {
              if (!Files.exists(file)) {
                throw new UncheckedMojoExecutionException(
                    new MojoExecutionException("file '" + file + "' does not exist"));
              }
              if (Files.isDirectory(file)) {
                try {
                  return Files.walk(file, MAX_DEPTH)
                      .filter(Files::isRegularFile)
                      .filter(pathMatcher::matches);
                } catch (final IOException exception) {
                  throw new UncheckedMojoExecutionException(
                      new MojoExecutionException(
                          "could not recourse directory '" + file + "'", exception));
                }
              }
              return Stream.of(file);
            });
  }

  private FormattingResult formatFile(
      final Path file, final Formatter formatter, final GrammarVersion grammarVersion) {
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
      formatted = formatter.format(contents, grammarVersion);
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
