# pkl-maven-plugin

A Maven plugin for working with [Pkl](https://pkl-lang.org/) configuration files.

---

## Quick Start

```xml
<plugin>
    <groupId>com.sitepark.maven.plugins</groupId>
    <artifactId>pkl-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <sources>
            <source>src/main/pkl</source>
            <source>src/test/pkl</source>
        </sources>
        <tests>
            <test>src/test/pkl</test>
        </tests>
    </configuration>
</plugin>
```

---

## Goals

### `check-format`
Validate Pkl files against the [Pkl formatter](https://pkl-lang.org/main/current/release-notes/0.30.html#formatter) standard. The build fails if violations are found.

### `apply-format`
Automatically format Pkl files according to the [Pkl formatter](https://pkl-lang.org/main/current/release-notes/0.30.html#formatter) standard.

### `eval`
Evaluate Pkl files and output results to specified files. The build fails if evaluation produces no output.

### `test`
Run Pkl test files and report results. The build fails if tests error, fail or none are executed. Without `files`, the tests declared by the `PklProject` are run, like `pkl test` without file arguments does.

### `overwrite`
Run Pkl test files, report results while overwriting expected outputs with actual results. The build fails if tests error, fail or none are executed.

### `package-project`
Create the package of a `PklProject`, like `pkl project package` does: a zip, its metadata and the checksums of both. Does nothing if the project has no `PklProject`, and fails if that project declares no `package`.

### `resolve-project`
Resolve the dependencies of a `PklProject` and write them to `PklProject.deps.json`, like `pkl project resolve` does. Does nothing if the project has no `PklProject`.

### `help`
Display plugin usage information.

---

## Configuration

### What a goal works on

Every goal is told what to work on by one parameter, and the name says what kind
of thing it is - formatting touches **sources**, testing uses **tests** and
evaluating take entry **modules**, everything they import comes along by itself:

| Goal                           | Parameter | Required | Description                                                                   |
| :----------------------------- | :-------- | :------- | :---------------------------------------------------------------------------- |
| `apply-format`, `check-format` | `sources` | ✓        | The pkl sources to format                                                     |
| `test`, `overwrite`            | `tests`   | —        | The test modules to run; defaults to the `tests` declared by the `PklProject` |
| `eval`                         | `modules` | ✓        | The modules to evaluate                                                       |

Each entry is a file, a directory or a glob pattern; relative entries are resolved against
`${basedir}`:

| Entry | Contributes |
| :--- | :--- |
| a file | that file |
| a directory | the `*.pkl` files below it (plus `PklProject` files when formatting) |
| a glob pattern | everything it matches, for example `src/test/pkl/**/*Test.pkl` |

Directories and patterns are reduced by `excludes`:

| Parameter  | Default                              | Description                                       |
| :--------- | :----------------------------------- | :------------------------------------------------ |
| `excludes` | `**/target/**`, `**/node_modules/**` | Patterns left out of directories and glob matches |

A pattern that matches nothing is allowed; a missing file or directory — including the part of
a pattern before the first glob character — fails the build.

```xml
<sources>
  <source>src/main/webapp/WEB-INF/config</source>
  <source>src/test/pkl</source>
</sources>
<tests>
  <test>src/test/pkl/**/*.pkl</test>
</tests>
```

### Evaluator Settings
*For `eval`, `test` and `overwrite` goals*

These are named after the `evaluatorSettings` of a `PklProject` and take precedence over them if specified:

| Parameter            | Default            | Description                                      |
| :------------------- | :----------------- | :----------------------------------------------- |
| `modulePath`         | project's setting  | Module path for the `modulepath:` scheme         |
| `externalProperties` | —                  | Properties read via the `prop:` scheme           |
| `env`                | —                  | Environment variables read via the `env:` scheme |
| `moduleCacheDir`     | pkl's module cache | Directory to cache downloaded packages in        |
| `noCache`            | `false`            | Disable the module cache                         |

```xml
<modulePath>
  <path>${project.build.directory}/dependency/config</path>
  <path>src/main/webapp/WEB-INF/config</path>
</modulePath>
```

### Global Parameters
*For all goals*

| Parameter    | Property          | Default | Description                                      |
| :----------- | :---------------- | :------ | :----------------------------------------------- |
| `projectDir` | `pkl.projectDir`  | —       | Directory of the `PklProject` to use (see below) |
| `skip`       | `pkl.<goal>.skip` | `false` | Skip this goal                                   |
| —            | `pkl.skip`        | `false` | Skip every pkl goal                              |

### Test-Specific Parameters
*For `test` and `overwrite` goals*

| Parameter | Description                                                 |
| :-------- | :---------------------------------------------------------- |
| `junit`   | JUnit XML report options; no reports are written without it |

| `junit` child      | Default     | Description                                          |
| :----------------- | :---------- | :--------------------------------------------------- |
| `reportsDirectory` | —           | Directory to write the reports to                    |
| `aggregate`        | `false`     | Write a single report instead of one per test module |
| `suiteName`        | `pkl-tests` | Name of the aggregated test suite and of its file    |

```xml
<junit>
  <reportsDirectory>${project.build.directory}/pkl-reports</reportsDirectory>
</junit>
```

Keep the reports apart from `surefire-reports` so that the Java and Pkl reports do not mix.

### Eval-Specific Parameters
*For `eval` goal*

| Parameter         | Required | Default | Description                          |
| :---------------- | :------- | :------ | :----------------------------------- |
| `outputDirectory` | ✓        | —       | Output directory for generated files |
| `overwrite`       | —        | `true`  | Overwrite existing output files      |

### Format-Specific Parameters
*For `check-format` and `apply-format` goals*

| Parameter        | Property             | Default  | Description                                                           |
| :--------------- | :------------------- | :------- | :-------------------------------------------------------------------- |
| `grammarVersion` | `pkl.grammarVersion` | `latest` | Grammar compatibility: `1` (0.25-0.29), `2` (0.30+), `latest` (0.30+) |

### Package-Specific Parameters
*For `package-project` goal*

| Parameter          | Default                                                      | Description                                                         |
| :----------------- | :----------------------------------------------------------- | :------------------------------------------------------------------ |
| `outputPath`       | `${project.build.directory}/pkl-packages/%{name}@%{version}` | Where to write the package; supports `%{name}` and `%{version}`     |
| `skipPublishCheck` | `false`                                                      | Skip checking whether the package was already published differently |

The publish check requests the package's `baseUri`, so set `skipPublishCheck` for packages that
only ever exist locally - otherwise the goal fails with a connection error.

### Project-Resolve-Specific Parameters
*For `resolve-project` goal*

| Parameter        | Default            | Description                               |
| :--------------- | :----------------- | :---------------------------------------- |
| `moduleCacheDir` | pkl's module cache | Directory to cache downloaded packages in |

### Pkl Projects

The `eval`, `test` and `overwrite` goals evaluate against a [Pkl project](https://pkl-lang.org/main/current/language-reference/index.html#projects) if one is found, which is what makes dependency notation imports such as `import*("@myDependency/*.pkl")` resolve.

The `PklProject` is searched for from the first configured entry upwards, up to and including the base directory of the Maven project. Set `projectDir` to point at a specific project instead; the goal then fails if that directory contains no `PklProject`.

Anything configured here takes precedence over the project: `modulePath` over its
`evaluatorSettings.modulePath`, `tests` over its `tests`. Leaving them out keeps the
`PklProject` the single source of truth.

Dependency resolution requires a `PklProject.deps.json` next to the `PklProject`. Create it with the `resolve-project` goal (or `pkl project resolve`); The file is static and can be committed.

## Usage Examples

### Format and test your Pkl files

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.sitepark.maven.plugins</groupId>
            <artifactId>pkl-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <id>apply-pkl-format</id>
                    <goals>
                        <goal>apply-format</goal>
                    </goals>
                </execution>
                <execution>
                    <id>test-pkl</id>
                    <goals>
                        <goal>test</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <sources>
                    <source>src/main/webapp/WEB-INF/config</source>
                    <source>src/test/pkl</source>
                </sources>
                <tests>
                    <test>src/test/pkl</test>
                </tests>
                <modulePath>
                    <path>src/main/webapp/WEB-INF/config</path>
                </modulePath>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Test against a Pkl project

With a `PklProject` in the base directory that declares the configuration as a local dependency:

```pkl
amends "pkl:Project"

dependencies {
  ["config"] = import("src/main/webapp/WEB-INF/config/PklProject")
}
```

This can also be usefull to glob over source files, which is not possible with the `modulepath:` scheme:

```pkl
local files = import*("@config/*.pkl")
```

```bash
mvn pkl:resolve-project                               # PklProject in root dir
mvn pkl:resolve-project -Dpkl.projectDir=src/main/pkl # specific PklProject
```

### Using SNAPSHOT version

If using a SNAPSHOT version, add this to your `pom.xml`:

```xml
<pluginRepositories>
    <pluginRepository>
        <id>central-portal-snapshots</id>
        <name>Central Portal Snapshots</name>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </pluginRepository>
</pluginRepositories>
```
