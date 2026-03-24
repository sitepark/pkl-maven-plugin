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
        <paths>
            <path>${basedir}/src/main/pkl/</path>
        </paths>
        <directory>${baseDir}/src/test/pkl</directory>
        <files>**/*.pkl</files>
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
Run Pkl test files and report results. The build fails if tests error, fail or none are executed.

### `overwrite`
Run Pkl test files, report results while overwriting expected outputs with actual results. The build fails if tests error, fail or none are executed.

### `help`
Display plugin usage information.

---

## Configuration

### Common Parameters

| Parameter              | Required | Default      | Description                                                          |
| :--------------------- | :------- | :----------- | :------------------------------------------------------------------- |
| `directory`            | —        | `${basedir}` | Base directory for searching Pkl files via `files` glob pattern      |
| `files`                | ✓        | —            | Glob pattern (relative to `directory`) matching Pkl files to process |
| `modulepath`           | —        | —            | Module path for Pkl execution                                        |
| `properties`           | —        | —            | External properties passed during execution                          |
| `environmentVariables` | —        | —            | Environment variables passed during execution                        |
| `skip`                 | —        | `false`      | Skip goal execution                                                  |

### Format-Specific Parameters
*For `check-format` and `apply-format` goals*

| Parameter        | Default  | Description                                                              |
| :--------------- | :------- | :----------------------------------------------------------------------- |
| `paths`          | —        | Paths/directories containing Pkl files to format (processed recursively) |
| `grammarVersion` | `latest` | Grammar compatibility: `1` (0.25-0.29), `2` (0.30+), `latest` (0.30+)    |

### Eval-Specific Parameters
*For `eval` goal*

| Parameter   | Required | Default | Description                          |
| :---------- | :------- | :------ | :----------------------------------- |
| `output`    | ✓        | —       | Output directory for generated files |
| `overwrite` | —        | `true`  | Overwrite existing output files      |

---

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
                <!-- Formatting -->
                <paths>
                    <path>${basedir}/src/main/webapp/WEB-INF/config/</path>
                    <path>${basedir}/src/test/pkl/</path>
                </paths>

                <!-- Testing -->
                <directory>${baseDir}/src/test/pkl</directory>
                <files>**/*.pkl</files>
                <modulepath>
                    <modulepath>${baseDir}/src/main/webapp/WEB-INF/config/</modulepath>
                </modulepath>
            </configuration>
        </plugin>
    </plugins>
</build>
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
