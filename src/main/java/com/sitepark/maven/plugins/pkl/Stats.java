package com.sitepark.maven.plugins.pkl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.pkl.core.PklException;

final record Stats(
    int testsRun,
    List<Failure> failures,
    List<Error> errors,
    List<Skipped> skipped,
    double secondsElapsed) {

  public static final class Builder {
    private int testsRun;
    private final List<Failure> failures;
    private final List<Error> errors;
    private List<Skipped> skipped;
    private double secondsElapsed;

    private Builder() {
      this.testsRun = 0;
      this.failures = new ArrayList<>();
      this.errors = new ArrayList<>();
      this.skipped = new ArrayList<>();
    }

    public Builder setTestsRun(final int amount) {
      this.testsRun = amount;
      return this;
    }

    public Builder addTestsRun(final int amount) {
      this.testsRun += amount;
      return this;
    }

    public Builder addFailure(final Failure failure) {
      this.failures.add(failure);
      return this;
    }

    public Builder addFailures(final Collection<? extends Failure> failures) {
      this.failures.addAll(failures);
      return this;
    }

    public Builder addError(final Error error) {
      this.errors.add(error);
      return this;
    }

    public Builder addErrors(final Collection<? extends Error> errors) {
      this.errors.addAll(errors);
      return this;
    }

    public Builder addSkipped(final Skipped skipped) {
      this.skipped.add(skipped);
      return this;
    }

    public Builder addSkipped(final Collection<? extends Skipped> skipped) {
      this.skipped.addAll(skipped);
      return this;
    }

    public Builder setSecondsElapsed(final double seconds) {
      this.secondsElapsed = seconds;
      return this;
    }

    public Builder addSecondsElapsed(final double seconds) {
      this.secondsElapsed += seconds;
      return this;
    }

    public Builder addAll(final Stats other) {
      this.addTestsRun(other.testsRun())
          .addFailures(other.failures())
          .addErrors(other.errors())
          .addSkipped(other.skipped())
          .addSecondsElapsed(other.secondsElapsed());
      return this;
    }

    public Stats build() {
      return new Stats(
          this.testsRun,
          List.copyOf(this.failures),
          List.copyOf(this.errors),
          List.copyOf(this.skipped),
          this.secondsElapsed);
    }
  }

  public static final class Message {
    private final List<String> lines;

    private Message(final List<String> lines) {
      this.lines = lines;
    }

    public static Message fromString(final String string) {
      return new Message(Arrays.stream(string.split("\n")).toList());
    }

    public static Message fromException(final PklException exception) {
      return new Message(
          Arrays.stream(exception.getMessage().split("\n"))
              .filter(Predicate.isEqual("–– Pkl Error ––").negate())
              .toList());
    }

    public List<String> lines() {
      return Collections.unmodifiableList(this.lines);
    }
  }

  public static record Scope(
      String module,
      /** nullable */
      String section,
      /** nullable */
      String test) {

    @Override
    public String toString() {
      final StringBuilder builder = new StringBuilder();
      builder.append(this.module);
      if (this.section != null) {
        builder.append('#');
        builder.append(this.section);
        if (this.test != null) {
          builder.append("[\"");
          builder.append(this.test);
          builder.append("\"]");
        }
      }
      return builder.toString();
    }
  }

  public static record Failure(Scope scope, String shortMessage, Message detailedMessage) {}

  public static record Error(Scope scope, String shortMessage, Message detailedMessage) {}

  public static record Skipped(Scope scope, String shortMessage, Message detailedMessage) {}

  public static final class SummingCollector
      implements Collector<Stats, SummingCollector.Accumulator, Stats> {

    private static final Set<Characteristics> CHARACTERISTICS = Set.of(Characteristics.CONCURRENT);

    private static final class Accumulator {
      private final Builder builder;

      public Accumulator() {
        this.builder = new Builder();
      }

      public void accumulate(final Stats stats) {
        this.builder.addAll(stats);
      }

      public Accumulator combine(final Accumulator other) {
        this.builder
            .addTestsRun(other.builder.testsRun)
            .addFailures(other.builder.failures)
            .addErrors(other.builder.errors)
            .addSkipped(other.builder.skipped);
        return this;
      }

      public Stats finish() {
        return this.builder.build();
      }
    }

    @Override
    public BiConsumer<Accumulator, Stats> accumulator() {
      return Accumulator::accumulate;
    }

    @Override
    public Set<Characteristics> characteristics() {
      return SummingCollector.CHARACTERISTICS;
    }

    @Override
    public BinaryOperator<Accumulator> combiner() {
      return Accumulator::combine;
    }

    @Override
    public Function<Accumulator, Stats> finisher() {
      return Accumulator::finish;
    }

    @Override
    public Supplier<Accumulator> supplier() {
      return Accumulator::new;
    }
  }

  public enum LevelOfSuccess {
    SUCCEEDED,
    SKIPPED,
    FAILED,
    ERRED;
  }

  public static Builder builder() {
    return new Builder();
  }

  public LevelOfSuccess levelOfSuccess() {
    if (!this.errors.isEmpty()) {
      return LevelOfSuccess.ERRED;
    }
    if (!this.failures.isEmpty()) {
      return LevelOfSuccess.FAILED;
    }
    if (!this.skipped.isEmpty()) {
      return LevelOfSuccess.SKIPPED;
    }
    return LevelOfSuccess.SUCCEEDED;
  }
}
