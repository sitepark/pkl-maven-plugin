# pkl-maven-plugin

## goals

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
                        <id>test-pkl</id>
                        <phase>test</phase>
                        <goals>
                            <goal>test</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
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
