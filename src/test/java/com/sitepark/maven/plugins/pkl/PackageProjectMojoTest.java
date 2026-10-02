package com.sitepark.maven.plugins.pkl;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class PackageProjectMojoTest {

  private static final String DEP_DIR = "src/test/resources/pkl/project/dep";
  private static final Path OUTPUT_DIR = Path.of("target/tests/pkl/packages");

  @Test
  public void testPackage() throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new PackageProjectMojo();
    mojo.projectDir = DEP_DIR;
    mojo.outputPath = OUTPUT_DIR + "/%{name}@%{version}";
    mojo.skipPublishCheck = true;
    mojo.color = false;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);

    final var packageDir = OUTPUT_DIR.resolve("dep@1.0.0");
    Assertions.assertAll(
        () -> Assertions.assertTrue(Files.exists(packageDir.resolve("dep@1.0.0.zip"))),
        () -> Assertions.assertTrue(Files.exists(packageDir.resolve("dep@1.0.0.zip.sha256"))),
        () -> Assertions.assertTrue(Files.exists(packageDir.resolve("dep@1.0.0"))),
        () -> Assertions.assertTrue(Files.exists(packageDir.resolve("dep@1.0.0.sha256"))));
  }

  @Test
  public void testWithoutPackage() throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new PackageProjectMojo();
    mojo.projectDir = "src/test/resources/pkl/project";
    mojo.outputPath = OUTPUT_DIR + "/%{name}@%{version}";
    mojo.skipPublishCheck = true;
    mojo.color = false;
    mojo.setLog(log);
    Assertions.assertThrows(MojoFailureException.class, mojo::execute);
  }

  @Test
  public void testWithoutProject() throws MojoFailureException, MojoExecutionException {
    final var log = new CapturingLog();
    final var mojo = new PackageProjectMojo();
    mojo.basedir = Path.of("src/test/resources/pkl/tests").toFile();
    mojo.color = false;
    mojo.setLog(log);
    Assertions.assertDoesNotThrow(mojo::execute);
    Assertions.assertTrue(log.captured().contains("No PklProject found"));
  }
}
