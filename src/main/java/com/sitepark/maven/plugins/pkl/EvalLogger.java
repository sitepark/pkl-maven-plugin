package com.sitepark.maven.plugins.pkl;

import java.nio.file.Path;
import java.text.DecimalFormat;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.utils.logging.MessageUtils;

final class EvalLogger {
  private final Log log;

  private static final DecimalFormat SECONDS_FORMAT = new DecimalFormat("#.###");

  public EvalLogger(final Log log) {
    this.log = log;
  }

  public void executionSkipped() {
    this.log.info("Evaluation is skipped");
  }

  public void beginExecution() {}

  public void evalFile(final Path file) {
    this.log.debug("Evaluating " + file);
  }

  public void writeFile(final Path file) {
    this.log.info("Writing " + file);
  }

  public void writeFileSkipped(final Path file) {
    this.log.info("Skip writing existing " + file);
  }

  public void noFilesWritten(final Path file) {
    this.log.warn(MessageUtils.buffer().warning("No output files defined in " + file).build());
  }

  public void summary(final EvalStats evalStats) {
    this.log.info(
        MessageUtils.buffer()
            .success("Files evaluated: " + evalStats.filesEvaluated())
            .a(", ")
            .success("Files created: " + evalStats.filesCreated())
            .a(", Time elapsed: ")
            .a(SECONDS_FORMAT.format(evalStats.secondsElapsed()))
            .a("s")
            .build());
    this.log.info("");
  }
}
