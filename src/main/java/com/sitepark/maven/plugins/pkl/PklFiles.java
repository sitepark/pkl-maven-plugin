package com.sitepark.maven.plugins.pkl;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects pkl files from a list of files, directories and glob patterns.
 */
final class PklFiles {
  private PklFiles() {}

  /**
   * What a directory contributes to the goals that evaluate modules.
   */
  public static final List<String> PKL_FILES = List.of("**/*.pkl");

  /**
   * What a directory contributes to the goals that format files.
   */
  public static final List<String> PKL_FILES_AND_PROJECTS = List.of("**/*.pkl", "**/PklProject");

  /**
   * The patterns excluded by default, so that a widely scoped entry does not reach build output.
   */
  public static final List<String> DEFAULT_EXCLUDES = List.of("**/target/**", "**/node_modules/**");

  private static final String GLOB_CHARACTERS = "*?[{";

  /**
   * Resolves the given entries against the base directory and collects the matching files.
   * An entry pointing at a file is taken as it is, a directory contributes the files matching
   * {@code directoryIncludes}, and an entry containing glob characters contributes everything
   * it matches. Both of the latter are reduced by {@code excludes}.
   */
  public static Set<Path> collect(
      final Collection<String> entries,
      final List<String> directoryIncludes,
      final List<String> excludes,
      final Path basedir)
      throws IOException {
    final var excludeMatchers = matchers(excludes != null ? excludes : DEFAULT_EXCLUDES);
    final var files = new LinkedHashSet<Path>();
    for (final var entry : entries) {
      final var resolved = resolve(entry, basedir);
      if (isGlob(resolved)) {
        collectMatching(globRoot(resolved), matchers(List.of(resolved)), excludeMatchers, files);
        continue;
      }
      final var path = Path.of(resolved);
      if (!Files.exists(path)) {
        throw new NoSuchFileException(resolved);
      }
      if (!Files.isDirectory(path)) {
        files.add(path);
        continue;
      }
      // the walk is already rooted at the directory, so the patterns stay unanchored
      collectMatching(path, matchers(directoryIncludes), excludeMatchers, files);
    }
    return files;
  }

  private static void collectMatching(
      final Path root,
      final List<PathMatcher> includeMatchers,
      final List<PathMatcher> excludeMatchers,
      final Set<Path> files)
      throws IOException {
    if (!Files.exists(root)) {
      throw new NoSuchFileException(root.toString());
    }
    try (final var walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile)
          .filter(file -> matches(file, includeMatchers) && !matches(file, excludeMatchers))
          .forEach(files::add);
    }
  }

  /**
   * Resolves relative entries against the base directory as a string, so that glob characters
   * do not have to pass as a path. An empty base directory leaves the entry relative.
   */
  private static String resolve(final String entry, final Path basedir) {
    final var normalized = entry.replace('\\', '/');
    if (normalized.startsWith("/") || basedir == null || basedir.toString().isEmpty()) {
      return normalized;
    }
    final var prefix = basedir.toString();
    return prefix.endsWith("/") ? prefix + normalized : prefix + "/" + normalized;
  }

  private static boolean isGlob(final String entry) {
    return entry.chars().anyMatch(character -> GLOB_CHARACTERS.indexOf(character) >= 0);
  }

  /**
   * The longest leading part of the pattern without glob characters, where the walk starts.
   */
  private static Path globRoot(final String pattern) {
    final var segments = pattern.split("/");
    final var root = new StringBuilder();
    for (final var segment : segments) {
      if (isGlob(segment)) {
        break;
      }
      root.append(segment).append('/');
    }
    return Path.of(root.length() > 0 ? root.toString() : "/");
  }

  private static List<PathMatcher> matchers(final List<String> patterns) {
    return patterns.stream()
        .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
        .toList();
  }

  private static boolean matches(final Path file, final List<PathMatcher> matchers) {
    return matchers.stream().anyMatch(matcher -> matcher.matches(file));
  }
}
