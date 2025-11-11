package com.sitepark.maven.plugins.pkl;

import difflib.Delta;
import difflib.DiffUtils;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.utils.logging.MessageUtils;

final class FormatLogger {
  private final Log log;

  private static final class DiffCollector
      implements Collector<Delta<String>, DiffCollector.Accumulator, Stream<String>> {
    private final List<String> lines;

    private static final int DIFF_CONTEXT_LINES = 1;

    DiffCollector(final List<String> original) {
      this.lines = original;
    }

    private static sealed interface Line
        permits Line.Context, Line.Added, Line.Removed, Line.Elipse {
      public static final Elipse ELIPSE = new Elipse();

      record Context(int number, String content) implements Line {}

      record Added(int number, String content) implements Line {}

      record Removed(int number, String content) implements Line {}

      record Elipse() implements Line {}
    }

    private final class Accumulator {
      private Stream.Builder<Line> downstream;
      private int lastContextLine = 0;
      private int largestLineNumber = 0;

      Accumulator() {
        this.downstream = Stream.builder();
      }

      public void accumulate(final Delta<String> delta) {
        final var original = delta.getOriginal();
        final var revised = delta.getRevised();
        final var patchSize = original.getLines().size();
        var position = original.getPosition();
        var revision = revised.getPosition();
        this.largestLineNumber = position + patchSize + DIFF_CONTEXT_LINES;
        final var min = Math.max(this.lastContextLine, position - DIFF_CONTEXT_LINES);
        final var max = Math.min(DiffCollector.this.lines.size(), this.largestLineNumber);
        if (this.lastContextLine != 0 && min > this.lastContextLine) {
          this.downstream.add(Line.ELIPSE);
        }
        for (var i = min; i < position; i++) {
          this.downstream.add(new Line.Context(i + 1, DiffCollector.this.lines.get(i)));
        }
        for (final String line : original.getLines()) {
          this.downstream.add(new Line.Removed(++position, line));
        }
        for (final String line : revised.getLines()) {
          this.downstream.add(new Line.Added(++revision, line));
        }
        while (position < max) {
          this.downstream.add(
              new Line.Context(position + 1, DiffCollector.this.lines.get(position++)));
        }
        this.lastContextLine = max;
      }

      private Accumulator combine(final Accumulator other) {
        throw new UnsupportedOperationException();
      }

      private Stream<String> finish() {
        final var format = "%" + this.digits(this.largestLineNumber) + "d ";
        final var contextFormat = format + " %s";
        return this.downstream
            .build()
            .sequential()
            .map(
                e ->
                    switch (e) {
                      case Line.Context(final var number, final var content) ->
                          String.format(contextFormat, number, content);
                      case Line.Added(final var number, final var content) ->
                          MessageUtils.buffer()
                              .a(String.format(format, number))
                              .success('+')
                              .success(content)
                              .build();
                      case Line.Removed(final var number, final var content) ->
                          MessageUtils.buffer()
                              .a(String.format(format, number))
                              .failure('-')
                              .failure(content)
                              .build();
                      case Line.Elipse() -> "...";
                    });
      }

      private int digits(final int number) {
        return number < 100_000
            ? number < 100 ? number < 10 ? 1 : 2 : number < 1_000 ? 3 : number < 10_000 ? 4 : 5
            : number < 10_000_000
                ? number < 1_000_000 ? 6 : 7
                : number < 100_000_000 ? 8 : number < 1_000_000_000 ? 9 : 10;
      }
    }

    @Override
    public Supplier<Accumulator> supplier() {
      return Accumulator::new;
    }

    @Override
    public BiConsumer<Accumulator, Delta<String>> accumulator() {
      return Accumulator::accumulate;
    }

    @Override
    public BinaryOperator<Accumulator> combiner() {
      return Accumulator::combine;
    }

    @Override
    public Function<Accumulator, Stream<String>> finisher() {
      return Accumulator::finish;
    }

    @Override
    public Set<Characteristics> characteristics() {
      return Set.of();
    }
  }

  public FormatLogger(final Log log) {
    this.log = log;
  }

  public void executionSkipped() {
    this.log.info("Formatting skipped");
  }

  public void beginExecution() {}

  public void invalidFile(final Path file, final String original, final String formatted) {
    this.log.error("Error in " + file);
    final var lines = Arrays.asList(original.split("\n"));
    final var diff = DiffUtils.diff(lines, Arrays.asList(formatted.split("\n")));
    diff.getDeltas().stream().collect(new DiffCollector(lines)).forEach(this.log::error);
    this.log.error("");
  }

  public void formattedFile(final Path file, final String original, final String formatted) {
    this.log.info("Formatted " + file);
    final var lines = Arrays.asList(original.split("\n"));
    final var diff = DiffUtils.diff(lines, Arrays.asList(formatted.split("\n")));
    diff.getDeltas().stream().collect(new DiffCollector(lines)).forEach(this.log::info);
    this.log.info("");
  }
}
