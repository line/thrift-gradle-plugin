## Gradle Thrift Plugin

Gradle Thrift Plugin uses thrift compiler to compile Thrift IDL files.
This project is forked from [jruyi/thrift-gradle-plugin](https://github.com/jruyi/thrift-gradle-plugin).  
It is open-sourced and licensed
under [Apache License 2.0](https://www.tldrlegal.com/license/apache-license-2-0-apache-2-0)
by [LINE Corporation](https://engineering.linecorp.com/en).

### Usage

To use this plugin, add the following to your build script.

```groovy
plugins {
    id "com.linecorp.thrift-gradle-plugin" version "0.6.0"
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
        classpath("com.linecorp.thrift-gradle-plugin:com.linecorp.thrift-gradle-plugin.gradle.plugin:0.6.0")
    }
}

apply plugin: "com.linecorp.thrift-gradle-plugin"
```

### Example

See the `examples/java-project` directory for a very simple example.

### Implicitly Applied Plugins

None.

### Tasks

The Thrift plugin adds compileThrift task which compiles Thrift IDL files using thrift compiler.
You can use thrift extension to configure the added compileThrift task.

##### Table-1 Properties of compileThrift Extension

| Extension property | Type                | Default value of compileThrift added by plugin                                  |
|--------------------|---------------------|---------------------------------------------------------------------------------|
| thriftExecutable   | String              | thrift                                                                          |
| sourceDir          | File                | _projectDir_/src/main/thrift                                                    |
| sourceItems        | Object...           | _projectDir_/src/main/thrift                                                    |
| outputDir          | File                | _buildDir_/generated-sources/thrift                                             |
| includeDirs        | Set<File>           | []                                                                              |
| generators         | Map<String, String> | ['java':''] if autoDetectPlugin is true and JavaPlugin is applied, otherwise [] |
| nowarn             | boolean             | false                                                                           |
| strict             | boolean             | false                                                                           |
| verbose            | boolean             | false                                                                           |
| recurse            | boolean             | false                                                                           |
| debug              | boolean             | false                                                                           |
| createGenFolder    | boolean             | true                                                                            |
| autoDetectPlugin   | boolean             | true                                                                            |

If createGenFolder is set to false, no gen-* folder will be created.

sourceDir is only used for backward compatibility

sourceItems are a set of sources, which will be used for generating java files from thrift.
A source can either be a path specified as a string or a file. In case a source is a relative path the source
will be
relative to _srcDir_.
In case a source is a directory, the directory will be scanned recursively for *.thrift files and used.

When autoDetectPlugin is true, generator 'java' will be created and the generated java code will be added to
Java source
automatically. And the task will be added to compileJava task's dependency.
We can disable this by setting autoDetectPlugin to false.

##### Example

```groovy
compileThrift {
    recurse true

    generator 'html'
    generator 'java', 'private-members'
}
```

### Creating a custom task by extending CompileThrift

You can create a custom task by extending CompileThrift with the following properties.
Plugin will set default values to the properties listed in Table-3.

##### Table-2 Task properties of CompileThrift

| Task property    | Type                |
|------------------|---------------------|
| thriftExecutable | String              |
| sourceItems      | Object...           |
| outputDir        | File                |
| includeDirs      | Set<File>           |
| generators       | Map<String, String> |
| nowarn           | boolean             |
| strict           | boolean             |
| verbose          | boolean             |
| recurse          | boolean             |
| debug            | boolean             |
| createGenFolder  | boolean             |

##### Table-3 Default value of task properties set by plugin

| Task property    | Type    | Default value of CompileThrift set by plugin |
|------------------|---------|----------------------------------------------|
| thriftExecutable | String  | thrift                                       |
| nowarn           | boolean | false                                        |
| strict           | boolean | false                                        |
| verbose          | boolean | false                                        |
| recurse          | boolean | false                                        |
| debug            | boolean | false                                        |
| createGenFolder  | boolean | true                                         |

##### Example

Below example is creating a custom task by extending CompileThrift and add it to compileJava task's dependency.

```groovy
tasks.register("customCompileThrift", com.linecorp.thrift.plugin.CompileThrift) {
    thriftExecutable = "../../lib/thrift/0.17.0/thrift.${osdetector.classifier}"
    sourceItems.from(layout.projectDirectory.dir("src/main/thrift"))
    outputDir.set(layout.buildDirectory.dir("gen-gen-src"))
    generators.put('java', 'private-members')
}

tasks.named("compileJava") {
    dependsOn("customCompileThrift")
}
```
