package com.sitepark.maven.plugins.pkl;

final record EvalStats(int filesEvaluated, int filesCreated, double secondsElapsed) {

  public static final class Builder {
    private int filesEvaluated;
    private int filesCreated;
    private double secondsElapsed;

    private Builder() {
      this.filesEvaluated = 0;
      this.filesCreated = 0;
    }

    public Builder setFilesEvaluated(final int amount) {
      this.filesEvaluated = amount;
      return this;
    }

    public Builder addFilesEvaluated(final int amount) {
      this.filesEvaluated += amount;
      return this;
    }

    public Builder setFilesCreated(final int amount) {
      this.filesCreated = amount;
      return this;
    }

    public Builder addFilesCreated(final int amount) {
      this.filesCreated += amount;
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

    public Builder addAll(final EvalStats other) {
      this.addFilesEvaluated(other.filesEvaluated())
          .addFilesCreated(other.filesCreated())
          .addSecondsElapsed(other.secondsElapsed());
      return this;
    }

    public EvalStats build() {
      return new EvalStats(this.filesEvaluated, this.filesCreated, this.secondsElapsed);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
