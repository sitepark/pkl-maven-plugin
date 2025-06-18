package com.sitepark.maven.plugins.pkl;

import org.apache.maven.plugin.logging.Log;

public final class CapturingLog implements Log {
  private final StringBuilder buffer;

  private enum Level {
    DEBUG,
    INFO,
    WARN,
    ERROR;

    private final String label;

    private Level() {
      this.label = "[" + this.name() + "]";
    }

    public String label() {
      return this.label;
    }
  }

  public CapturingLog() {
    this.buffer = new StringBuilder();
  }

  public CapturingLog(final int capacity) {
    this.buffer = new StringBuilder(capacity);
  }

  public boolean isDebugEnabled() {
    return true;
  }

  public boolean isInfoEnabled() {
    return true;
  }

  public boolean isWarnEnabled() {
    return true;
  }

  public boolean isErrorEnabled() {
    return true;
  }

  public void debug(final CharSequence content) {
    if (content.isEmpty()) {
      this.newline(Level.DEBUG);
    } else {
      this.log(Level.DEBUG, content);
    }
  }

  public void debug(final CharSequence content, final Throwable error) {
    if (content.isEmpty()) {
      this.log(Level.DEBUG, error);
    } else {
      this.log(Level.DEBUG, content, error);
    }
  }

  public void debug(final Throwable error) {
    this.log(Level.DEBUG, error);
  }

  public void info(final CharSequence content) {
    if (content.isEmpty()) {
      this.newline(Level.INFO);
    } else {
      this.log(Level.INFO, content);
    }
  }

  public void info(final CharSequence content, final Throwable error) {
    if (content.isEmpty()) {
      this.log(Level.INFO, error);
    } else {
      this.log(Level.INFO, content, error);
    }
  }

  public void info(final Throwable error) {
    this.log(Level.INFO, error);
  }

  public void warn(final CharSequence content) {
    if (content.isEmpty()) {
      this.newline(Level.WARN);
    } else {
      this.log(Level.WARN, content);
    }
  }

  public void warn(final CharSequence content, final Throwable error) {
    if (content.isEmpty()) {
      this.log(Level.WARN, error);
    } else {
      this.log(Level.WARN, content, error);
    }
  }

  public void warn(final Throwable error) {
    this.log(Level.WARN, error);
  }

  public void error(final CharSequence content) {
    if (content.isEmpty()) {
      this.newline(Level.ERROR);
    } else {
      this.log(Level.ERROR, content);
    }
  }

  public void error(final CharSequence content, final Throwable error) {
    if (content.isEmpty()) {
      this.log(Level.ERROR, error);
    } else {
      this.log(Level.ERROR, content, error);
    }
  }

  public void error(final Throwable error) {
    this.log(Level.ERROR, error);
  }

  public String captured() {
    return this.buffer.toString();
  }

  private void newline(final Level level) {
    this.buffer.append(level.label()).append('\n');
  }

  private void log(final Level level, final CharSequence content) {
    this.buffer.append(level.label()).append(' ').append(content).append('\n');
  }

  private void log(final Level level, final Throwable error) {
    this.buffer.append(level.label()).append(' ').append(error.getMessage()).append('\n');
  }

  private void log(final Level level, final CharSequence content, final Throwable error) {
    this.buffer
        .append(level.label())
        .append(' ')
        .append(content)
        .append('\n')
        .append(level.label())
        .append(' ')
        .append(error.getMessage())
        .append('\n');
  }
}
