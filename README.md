# pkl-maven-plugin

## goals

### test

Execute pkl-test files and report the results. Fails the build if tests err or
fail.

### override

Execute pkl-test files and report the results. Fails the build if tests err or
fail. This overrides the expected output files for `example` tests with the
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

### skip

Whether to skip execution.

## example configuration

```xml
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
```

