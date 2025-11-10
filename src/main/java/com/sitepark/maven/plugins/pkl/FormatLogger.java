package com.sitepark.maven.plugins.pkl;

import difflib.Delta;
import difflib.DiffUtils;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.utils.logging.MessageUtils;

final class FormatLogger {
  private final Log log;

  private static final int DIFF_CONTEXT_LINES = 1;

  public FormatLogger(final Log log) {
    this.log = log;
  }

  public void executionSkipped() {
    this.log.info("Formatting skipped");
  }

  public void beginExecution() {}

  public void invalidFile(final Path file, final String original, final String formatted) {
    this.log.error("Error in " + file);
    this.diff(original, formatted, this.log::error);
  }

  public void formattedFile(final Path file, final String original, final String formatted) {
    this.log.info("Formatted " + file);
    this.diff(original, formatted, this.log::info);
  }

  private void diff(final String original, final String revised, final Consumer<String> log) {
    final var lines = Arrays.asList(original.split("\n"));
    final var patch = DiffUtils.diff(lines, Arrays.asList(revised.split("\n")));

    final var deltas = patch.getDeltas();
    switch (deltas.size()) {
      case 0 -> {}
      case 1 -> {
        final var delta = deltas.getFirst();
        final var format = "%" + this.digits(this.largestLineNumber(delta)) + "d ";
        this.logPatch(delta, lines, format, log);
      }
      default -> {
        boolean first = true;
        final var format = "%" + this.digits(this.largestLineNumber(deltas.getLast())) + "d ";
        for (final var delta : deltas) {
          if (first) {
            first = false;
          } else {
            log.accept("...");
          }
          this.logPatch(delta, lines, format, log);
        }
      }
    }
    log.accept("");
  }

  private void logPatch(
      final Delta<String> delta,
      final List<String> lines,
      final String format,
      final Consumer<String> log) {
    int position = delta.getOriginal().getPosition();
    int revision = delta.getRevised().getPosition();
    final var patchSize = delta.getOriginal().getLines().size();
    final var min = Math.max(0, position - DIFF_CONTEXT_LINES);
    final var max = Math.min(lines.size(), position + patchSize + DIFF_CONTEXT_LINES);
    final var contextFormat = format + " %s";
    for (int i = min; i < position; i++) {
      log.accept(String.format(contextFormat, i + 1, lines.get(i)));
    }
    for (final String line : delta.getOriginal().getLines()) {
      log.accept(
          MessageUtils.buffer()
              .a(String.format(format, ++position))
              .failure('-')
              .failure(line)
              .build());
    }
    for (final String line : delta.getRevised().getLines()) {
      log.accept(
          MessageUtils.buffer()
              .a(String.format(format, ++revision))
              .success('+')
              .success(line)
              .build());
    }
    while (position < max) {
      log.accept(String.format(contextFormat, position + 1, lines.get(position++)));
    }
  }

  private int largestLineNumber(final Delta<String> delta) {
    final var original = delta.getOriginal();
    return original.getPosition() + original.getLines().size() + DIFF_CONTEXT_LINES;
  }

  private int digits(final int number) {
    return number < 100_000
        ? number < 100 ? number < 10 ? 1 : 2 : number < 1_000 ? 3 : number < 10_000 ? 4 : 5
        : number < 10_000_000
            ? number < 1_000_000 ? 6 : 7
            : number < 100_000_000 ? 8 : number < 1_000_000_000 ? 9 : 10;
  }
}
