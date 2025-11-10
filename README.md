# pkl-maven-plugin

## goals

### check-format

Check pkl files against the format defined by
[pkl-lang](https://pkl-lang.org/main/current/release-notes/0.30.html#formatter).
Fails if any violations are found.

### apply-format

Apply the format defined by
[pkl-lang](https://pkl-lang.org/main/current/release-notes/0.30.html#formatter)
to pkl files.

### eval

Evalute pkl files and write the results to the specified output-files. Fails the
build if no results were written.

### test

Execute pkl-test files and report the results. Fails the build if tests err or
fail.

### overwrite

Execute pkl-test files and report the results. Fails the build if tests err or
fail. This overwrites the expected output files for `example` tests with the
actual results.

### help

Outputs usage information about the plugin.

## parameter

### directory

Default: `${basedir}` \
The base directory to search pkl files in via [${pkl.files}](#files).

### files

__Required__ \
A globbed path, relative to [${pkl.directory}](#directory) matching all pkl
files to test.

### modulepath

A modulepath to use when executing.

### properties

External properties to use when executing.

### environmentVariables

Environment variables to use when executing.

### skip

Whether to skip execution.

### paths

*only for [check-format](#check-format) and [apply-format](#apply-format)*

Paths containing pkl files or directories to format/check recursively.

### grammarVersion

*only for [check-format](#check-format) and [apply-format](#apply-format)*

The grammar compatibility version to use: \
`1`:      0.25 - 0.29 \
`2`:      0.30+ \
`latest`: 0.30+ (default)

### output

*only for [eval](#eval)*

__Required__ \
The directory where files should be generated into.

### overwrite

*only for [eval](#eval)*

Default: `true` \
The directory where files should be generated into.

## example configuration

```xml
<project>
    <!-- this is required to use the SNAPSHOT version -->
    <pluginRepositories>
        <pluginRepository>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
            <id>central-portal-snapshots</id>
            <name>Central Portal Snapshots</name>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        </pluginRepository>
    </pluginRepositories>

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
                <!-- configurations are combined such that invocations like
                     `mvn pkl:apply-format`
                     do not require additional `-D` flags -->
                <configuration>
                    <!-- formatting -->
                    <paths>
                        <path>${basedir}/src/main/webapp/WEB-INF/config/</path>
                        <path>${basedir}/src/test/pkl/</path>
                    </paths>

                    <!-- testing -->
                    <directory>${baseDir}/src/test/pkl</directory>
                    <files>**/*.pkl</files>
                    <modulepath>
                        <modulepath>${baseDir}/src/main/webapp/WEB-INF/config/</modulepath>
                    </modulepath>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```
