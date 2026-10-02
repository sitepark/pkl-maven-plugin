package com.sitepark.maven.plugins.pkl;

/**
 * The JUnit XML report options of the test and overwrite goals.
 */
public final class JUnitOptions {

  /**
   * The directory to write the reports to. No reports are written if unset.
   */
  String reportsDirectory;

  /**
   * Whether to write a single aggregated report instead of one per test module.
   */
  boolean aggregate;

  /**
   * The name of the aggregated test suite.
   */
  String suiteName = "pkl-tests";

  public JUnitOptions() {}

  public void setReportsDirectory(final String reportsDirectory) {
    this.reportsDirectory = reportsDirectory;
  }

  public void setAggregate(final boolean aggregate) {
    this.aggregate = aggregate;
  }

  public void setSuiteName(final String suiteName) {
    this.suiteName = suiteName;
  }
}
