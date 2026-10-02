package com.sitepark.maven.plugins.pkl;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.pkl.core.Evaluator;
import org.pkl.core.EvaluatorBuilder;
import org.pkl.core.SecurityManagers;
import org.pkl.core.StackFrameTransformers;
import org.pkl.core.module.ModuleKeyFactories;
import org.pkl.core.module.ModulePathResolver;
import org.pkl.core.project.Project;
import org.pkl.core.resource.ResourceReaders;

/**
 * Builds the evaluator shared by the eval and test goals.
 */
final class PklEvaluators {
  private PklEvaluators() {}

  /**
   * Creates the resolver for the {@code modulepath:} scheme, resolving relative entries against
   * the base directory. A configured modulepath takes
   * precedence over the one declared in the evaluator settings of the given project, just like
   * the CLI's {@code --module-path} takes precedence over the project's.
   */
  public static ModulePathResolver modulePathResolver(
      final Set<String> modulepath, final Project project, final Path basedir) {
    if (modulepath != null && !modulepath.isEmpty()) {
      return new ModulePathResolver(
          modulepath.stream()
              .map(entry -> basedir.resolve(entry).normalize())
              .collect(Collectors.toSet()));
    }
    if (project != null) {
      final var projectModulePath = project.getResolvedEvaluatorSettings().modulePath();
      if (projectModulePath != null) {
        return new ModulePathResolver(List.copyOf(projectModulePath));
      }
    }
    return new ModulePathResolver(Set.of());
  }

  /**
   * Creates an evaluator. If a project is given, its dependencies and evaluator settings are
   * applied, but any explicitly configured parameter wins over the project's settings.
   */
  public static Evaluator create(
      final ModulePathResolver modulePathResolver,
      final Project project,
      final Map<String, String> environmentVariables,
      final Map<String, String> properties,
      final Path moduleCacheDir,
      final boolean noCache,
      final boolean color) {
    final var builder =
        EvaluatorBuilder.unconfigured()
            .setStackFrameTransformer(StackFrameTransformers.defaultTransformer)
            // registered before the project is applied, as the first matching factory of a
            // scheme wins and applying a project registers a modulepath factory of its own
            .addModuleKeyFactory(ModuleKeyFactories.standardLibrary)
            .addModuleKeyFactory(ModuleKeyFactories.modulePath(modulePathResolver))
            .addResourceReader(ResourceReaders.modulePath(modulePathResolver))
            .setAllowedModules(SecurityManagers.defaultAllowedModules)
            .setAllowedResources(SecurityManagers.defaultAllowedResources);
    if (project != null) {
      builder.applyFromProject(project);
    }
    // applied after the project, so that an explicitly configured cache wins over its settings
    if (noCache) {
      builder.setModuleCacheDir(null);
    } else if (moduleCacheDir != null) {
      builder.setModuleCacheDir(moduleCacheDir);
    }
    return builder
        .addModuleKeyFactory(ModuleKeyFactories.file)
        .addModuleKeyFactory(ModuleKeyFactories.http)
        .addModuleKeyFactory(ModuleKeyFactories.pkg)
        .addModuleKeyFactory(ModuleKeyFactories.projectpackage)
        .addModuleKeyFactory(ModuleKeyFactories.genericUrl)
        .addResourceReader(ResourceReaders.file())
        .addResourceReader(ResourceReaders.http())
        .addResourceReader(ResourceReaders.https())
        .addResourceReader(ResourceReaders.pkg())
        .addResourceReader(ResourceReaders.projectpackage())
        .addResourceReader(ResourceReaders.environmentVariable())
        .addResourceReader(ResourceReaders.externalProperty())
        .addEnvironmentVariables(environmentVariables)
        .addExternalProperties(properties)
        .setPowerAssertionsEnabled(true)
        .setColor(color)
        .build();
  }
}
