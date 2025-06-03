package com.sitepark.maven.plugins.pkl;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "overwrite", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public final class OverwriteMojo extends TestMojo {

  public OverwriteMojo() {
    super(true);
  }
}
