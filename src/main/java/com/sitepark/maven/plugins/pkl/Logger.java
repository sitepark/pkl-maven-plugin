package com.sitepark.maven.plugins.pkl;

import java.text.DecimalFormat;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.utils.logging.MessageUtils;

final class Logger {
  private final Log log;

  private static final String MESSAGE_INDENT = "  ";
  private static final DecimalFormat SECONDS_FORMAT = new DecimalFormat("#.###");

  private static final record TestScope(
      /** is either empty ("") or ends with a dot */
      String namespace,
      /** the last segment of the scope (contains no dots) */
      String module) {

    public static TestScope fromString(final String scope) {
      final var lastDot = scope.lastIndexOf('.');
      return new TestScope(scope.substring(0, lastDot + 1), scope.substring(lastDot + 1));
    }
  }

  public Logger(final Log log) {
    this.log = log;
  }

  public void executionSkipped() {
    this.log.info("Tests are skipped");
  }

  public void beginExecution() {
    this.log.info("");
    this.log.info("-------------------------------------------------------");
    this.log.info(" T E S T S");
    this.log.info("-------------------------------------------------------");
  }

  public void runTest(final String test) {
    this.log.info("Running " + test);
  }

  public void testLogs(final String logs) {
    if (logs != null && !logs.isBlank()) {
      this.log.info(logs);
    }
  }

  public void testResult(final String scope, final Stats stats) {
    final var testScope = TestScope.fromString(scope);
    switch (stats.levelOfSuccess()) {
      case SUCCEEDED -> this.successfullTests(testScope, stats);
      case SKIPPED -> this.skippedTests(testScope, stats);
      default -> this.failedTests(testScope, stats);
    }
  }

  public void summary(final Stats stats) {
    this.log.info("");
    this.log.info("Results:");
    this.log.info("");
    if (!stats.skipped().isEmpty()) {
      this.log.warn(MessageUtils.buffer().warning("Skipped:").build());
      for (final var skipped : stats.skipped()) {
        this.log.warn(
            MessageUtils.buffer()
                .a(MESSAGE_INDENT)
                .warning(skipped.scope() + " » " + skipped.shortMessage())
                .build());
      }
    }
    if (!stats.failures().isEmpty()) {
      this.log.error(MessageUtils.buffer().failure("Failures:").build());
      for (final var failure : stats.failures()) {
        this.log.error(
            MessageUtils.buffer()
                .a(MESSAGE_INDENT)
                .failure(failure.scope() + " » " + failure.shortMessage())
                .build());
      }
    }
    if (!stats.errors().isEmpty()) {
      this.log.error(MessageUtils.buffer().failure("Errors:").build());
      for (final var error : stats.errors()) {
        this.log.error(
            MessageUtils.buffer()
                .a(MESSAGE_INDENT)
                .failure(error.scope() + " » " + error.shortMessage())
                .build());
      }
    }
    final var summary =
        String.format(
            "Tests run: %d, Failures: %d, Errors: %d, Skipped: %d",
            stats.testsRun(),
            stats.failures().size(),
            stats.errors().size(),
            stats.skipped().size());
    switch (stats.levelOfSuccess()) {
      case ERRED:
      case FAILED:
        this.log.info("");
        this.log.error(MessageUtils.buffer().failure(summary).build());
        this.log.info("");
        break;
      case SKIPPED:
        this.log.info("");
        this.log.warn(MessageUtils.buffer().warning(summary).build());
        this.log.info("");
        break;
      case SUCCEEDED:
        this.log.info(MessageUtils.buffer().success(summary).build());
        break;
    }
  }

  private void successfullTests(final TestScope scope, final Stats stats) {
    this.log.info(
        MessageUtils.buffer()
            .success("Tests run: " + stats.testsRun())
            .a(", Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ")
            .a(SECONDS_FORMAT.format(stats.secondsElapsed()))
            .a("s in ")
            .a(scope.namespace())
            .strong(scope.module())
            .build());
  }

  private void skippedTests(final TestScope scope, final Stats stats) {
    this.log.warn(
        MessageUtils.buffer()
            .warning("Tests")
            .a(' ')
            .strong("run: " + stats.testsRun())
            .a(", Failures: 0, Errors: 0, ")
            .warning("Skipped: " + stats.skipped().size())
            .a(", Time elapsed: ")
            .a(SECONDS_FORMAT.format(stats.secondsElapsed()))
            .a("s in ")
            .a(scope.namespace())
            .strong(scope.module())
            .build());
    this.skipped(stats.skipped());
  }

  private void failedTests(final TestScope scope, final Stats stats) {
    final var message =
        MessageUtils.buffer().failure("Tests").a(' ').strong("run: " + stats.testsRun());
    if (!stats.failures().isEmpty()) {
      message.a(", ").failure("Failures: " + stats.failures().size()).a(", ");
    } else {
      message.a(", Failures: 0, ");
    }
    if (!stats.errors().isEmpty()) {
      message.failure("Errors: " + stats.errors().size()).a(", ");
    } else {
      message.a("Errors: 0, ");
    }
    if (!stats.skipped().isEmpty()) {
      message.warning("Skipped: " + stats.errors().size());
    } else {
      message.a("Skipped: 0");
    }
    message.a(", Time elapsed: ").a(SECONDS_FORMAT.format(stats.secondsElapsed())).a('s');
    final var errors = stats.errors();
    final var failures = stats.failures();
    if (errors.size() == 1) {
      message.failure(" <<< ERROR!");
    } else if (errors.size() > 1) {
      message.failure(" <<< ERRORS!");
    } else if (failures.size() == 1) {
      message.failure(" <<< FAILURE!");
    } else if (failures.size() > 1) {
      message.failure(" <<< FAILURES!");
    }
    message.a(" - in ").a(scope.namespace()).strong(scope.module());
    this.log.error(message.build());
    this.erred(stats.errors());
    this.failed(stats.failures());
    this.skipped(stats.skipped());
  }

  private void erred(final Iterable<Stats.Error> tests) {
    for (final var test : tests) {
      boolean first = true;
      for (final String line : test.detailedMessage().lines()) {
        if (first) {
          this.log.error(
              MessageUtils.buffer().a(MESSAGE_INDENT).a(line).a(' ').failure("<<< ERROR!").build());
          first = false;
        } else {
          this.log.error(MESSAGE_INDENT + line);
        }
      }
      this.log.info("");
    }
  }

  private void failed(final Iterable<Stats.Failure> tests) {
    for (final var test : tests) {
      boolean first = true;
      for (final String line : test.detailedMessage().lines()) {
        if (first) {
          this.log.error(
              MessageUtils.buffer()
                  .a(MESSAGE_INDENT)
                  .a(line)
                  .a(' ')
                  .failure("<<< FAILURE!")
                  .build());
          first = false;
        } else {
          this.log.error(MESSAGE_INDENT + line);
        }
      }
      this.log.info("");
    }
  }

  private void skipped(final Iterable<Stats.Skipped> tests) {
    for (final var test : tests) {
      boolean first = true;
      for (final String line : test.detailedMessage().lines()) {
        if (first) {
          this.log.warn(
              MessageUtils.buffer()
                  .a(MESSAGE_INDENT)
                  .a(line)
                  .a(' ')
                  .failure("<<< SKIPPED!")
                  .build());
          first = false;
        } else {
          this.log.error(MESSAGE_INDENT + line);
        }
      }
      this.log.info("");
    }
  }
}
