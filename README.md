## Gradle Thrift Plugin

Gradle Thrift Plugin uses thrift compiler to compile Thrift IDL files. 
This project is forked from [jruyi/thrift-gradle-plugin](https://github.com/jruyi/thrift-gradle-plugin)

### Usage

To use this plugin, add the following to your build script.

```groovy
plugins {
    id "com.linecorp.thrift-gradle-plugin" version "0.5.0"
}
```

Or using legacy plugin application.

```groovy
buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("com.linecorp.thrift-gradle-plugin:com.linecorp.thrift-gradle-plugin.gradle.plugin:0.5.0")
    }
}

apply plugin: "com.linecorp.thrift-gradle-plugin"
```

### Example

See the `examples/test2` directory for a very simple example.

### Implicitly Applied Plugins

None.

### Tasks

The Thrift plugin adds compileThrift task which compiles Thrift IDL files using thrift compiler.

##### Table-1 Task Properties of compileThrift

| Task Property     | Type                | Default Value                                      |
|-------------------|---------------------|----------------------------------------------------|
| thriftExecutable  | String              | thrift                                             |
| sourceDir         | File                | _projectDir_/src/main/thrift                       |
| sourceItems       | Object...           | _projectDir_/src/main/thrift                       |
| outputDir         | File                | _buildDir_/generated-sources/thrift                |
| includeDirs       | Set<File>           | []                                                 |
| generators        | Map<String, String> | ['java':''] if JavaPlugin is applied, otherwise [] |
| nowarn            | boolean             | false                                              |
| strict            | boolean             | false                                              |
| verbose           | boolean             | false                                              |
| recurse           | boolean             | false                                              |
| debug             | boolean             | false                                              |
| allowNegKeys      | boolean             | false                                              |
| allow64bitsConsts | boolean             | false                                              |
| createGenFolder   | boolean             | true                                               |

If createGenFolder is set to false, no gen-* folder will be created.

sourceDir is only used for backward compatibility

sourceItems are a set of sources, which will be used for generating java files from thrift.
A source can either be a path specified as a string or a file. In case a source is a relative path the source will be relative to _srcDir_. 
In case a source is a directory, the directory will be scanned recursively for *.thrift files and used.   

##### Example

```groovy
compileThrift {
    recurse true

    generator 'html'
    generator 'java', 'private-members'
}
```

### Default Behaviors

When JavaPlugin is applied, generator 'java' will be created and the generated java code will be added to Java source automatically.

## License

Gradle Thrift Plugin is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
