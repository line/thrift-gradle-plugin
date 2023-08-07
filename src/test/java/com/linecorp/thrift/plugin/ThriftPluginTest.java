/*
 * Copyright 2023 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.thrift.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ThriftPluginTest {
    @TempDir
    Path projectDir;

    private Path buildFile;

    private String thriftPathExpression;

    @BeforeEach
    public void setup() throws Exception {
        buildFile = projectDir.resolve("build.gradle");
        Files.write(buildFile,
                    Collections.singletonList(
                            "    plugins { \n" +
                            "        id \"java\" \n" +
                            "        id \"com.linecorp.thrift-gradle-plugin\" \n" +
                            "        id \"com.google.osdetector\" version \"1.7.3\" \n" +
                            "    }\n" +
                            "     repositories {\n" +
                            "         mavenCentral()\n" +
                            "     }\n" +
                            "     dependencies {\n" +
                            "        implementation 'javax.annotation:javax.annotation-api:1.3.2'\n" +
                            "        implementation 'org.slf4j:slf4j-api:2.0.7'\n" +
                            "        implementation 'org.apache.thrift:libthrift:0.17.0'\n" +
                            "    }\n" +
                            "    task printJavaSourceDirs(type: DefaultTask) {\n" +
                            "        def sourceSet = project.sourceSets.main \n" +
                            "        def javaSourceDirs = sourceSet.java.srcDirs\n" +
                            "        doLast {\n" +
                            "           javaSourceDirs.each { dir ->\n" +
                            "                println \"'javaSourceDir - $dir'\"\n" +
                            "            }\n" +
                            "        }\n" +
                            "    }\n"),
                    StandardOpenOption.CREATE);

        thriftPathExpression = Paths.get("lib/thrift/0.17.0").toAbsolutePath() +
                               "/thrift.${osdetector.classifier}";
    }

    @ParameterizedTest
    @ValueSource(strings = { "7.6", "8.0", "8.1" })
    public void generateJavaWithSimpleConfig(String version) throws Exception {
        copyDefaultThriftFiles();
        Files.write(buildFile,
                    Collections.singletonList(
                            "    compileThrift {\n" +
                            "        thriftExecutable \"" + thriftPathExpression + "\"\n" +
                            "    }\n"),
                    StandardOpenOption.APPEND);

        final BuildResult gradle = GradleRunner.create()
                                               .withProjectDir(projectDir.toFile())
                                               .withGradleVersion(version)
                                               .withArguments(Arrays.asList("compileJava", "--info"))
                                               .withPluginClasspath()
                                               .build();

        assertThat(gradle.task(":compileThrift").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(gradle.getOutput()).contains("-o " + projectDir.toFile().getCanonicalPath());
        assertThat(projectDir.resolve("build/generated-sources/thrift/gen-java")
                             .resolve("com/linecorp/thrift/plugin/test/TestService.java")
        ).exists();
        assertThat(projectDir.resolve("build/generated-sources/thrift/gen-java")
                             .resolve("com/linecorp/thrift/plugin/test/TestStruct.java")
        ).exists();

        assertThat(projectDir.resolve("build/classes/java/main")
                             .resolve("com/linecorp/thrift/plugin/test/TestStruct.class")
        ).exists();
    }

    @ParameterizedTest
    @ValueSource(strings = { "7.6", "8.0", "8.1" })
    public void generateJavaChangeOutputDirectory(String version) throws Exception {
        copyDefaultThriftFiles();
        Files.write(buildFile,
                    Collections.singletonList(
                            "    compileThrift {\n" +
                            "        thriftExecutable \"" + thriftPathExpression + "\"\n" +
                            "        outputDir layout.buildDirectory.dir(\"generated/thrift\")\n" +
                            "        createGenFolder false\n" +
                            "    }\n"),
                    StandardOpenOption.APPEND);

        final BuildResult gradle = GradleRunner.create()
                                               .withProjectDir(projectDir.toFile())
                                               .withGradleVersion(version)
                                               .withArguments(Arrays.asList("compileJava", "--info"))
                                               .withPluginClasspath()
                                               .build();

        assertThat(gradle.task(":compileThrift").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(gradle.getOutput()).contains("-out " + projectDir.toFile().getCanonicalPath());
        assertThat(projectDir.resolve("build/generated/thrift")
                             .resolve("com/linecorp/thrift/plugin/test/TestService.java")
        ).exists();
        assertThat(projectDir.resolve("build/generated/thrift")
                             .resolve("com/linecorp/thrift/plugin/test/TestStruct.java")
        ).exists();

        assertThat(projectDir.resolve("build/classes/java/main")
                             .resolve("com/linecorp/thrift/plugin/test/TestStruct.class")
        ).exists();
    }

    @ParameterizedTest
    @ValueSource(strings = { "7.6", "8.0", "8.1" })
    public void generateJavaUsingDifferentSourceDirectory(String version) throws Exception {
        Files.createDirectories(projectDir.resolve("thrift"));

        Files.copy(Paths.get("src/test/resources/test.thrift"),
                   projectDir.resolve("thrift/test.thrift"));

        Files.write(buildFile,
                    Collections.singletonList(
                            "    compileThrift {\n" +
                            "        thriftExecutable \"" + thriftPathExpression + "\"\n" +
                            "        sourceDir \"thrift\"\n" +
                            "    }\n"),
                    StandardOpenOption.APPEND);

        final BuildResult gradle = GradleRunner.create()
                                               .withProjectDir(projectDir.toFile())
                                               .withGradleVersion(version)
                                               .withArguments(Arrays.asList("compileJava", "--info"))
                                               .withPluginClasspath()
                                               .build();

        assertThat(gradle.task(":compileThrift").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(gradle.getOutput()).contains("-o " + projectDir.toFile().getCanonicalPath());
        assertThat(projectDir.resolve("build/generated-sources/thrift/gen-java")
                             .resolve("com/linecorp/thrift/plugin/test/TestService.java")
        ).exists();
        assertThat(projectDir.resolve("build/generated-sources/thrift/gen-java")
                             .resolve("com/linecorp/thrift/plugin/test/TestStruct.java")
        ).exists();

        assertThat(projectDir.resolve("build/classes/java/main")
                             .resolve("com/linecorp/thrift/plugin/test/TestStruct.class")
        ).exists();
    }

    @ParameterizedTest
    @ValueSource(strings = { "7.6", "8.0", "8.1" })
    public void generateJava(String version) throws Exception {
        copyDefaultThriftFiles();
        Files.write(buildFile,
                    Collections.singletonList(
                            "    compileThrift {\n" +
                            "        thriftExecutable \"" + thriftPathExpression + "\"\n" +
                            "        sourceDir \"src/main/thrift\"\n" +
                            "        outputDir layout.buildDirectory.dir(\"generated-sources/thrift\")\n" +
                            "        nowarn true\n" +
                            "        strict true\n" +
                            "        verbose true\n" +
                            "        recurse true\n" +
                            "        debug true\n" +
                            "        generator 'java'\n" +
                            "    }\n"),
                    StandardOpenOption.APPEND);

        final BuildResult gradle = GradleRunner.create()
                                               .withProjectDir(projectDir.toFile())
                                               .withGradleVersion(version)
                                               .withArguments(Arrays.asList("compileJava", "--info"))
                                               .withPluginClasspath()
                                               .build();

        assertThat(gradle.task(":compileThrift").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(gradle.getOutput()).contains(
                "-o " +
                projectDir.toFile().getCanonicalPath() +
                "/build/generated-sources/thrift --gen java" +
                " -r -nowarn -strict -v -debug " +
                projectDir.toFile().getCanonicalPath() +
                "/src/main/thrift/test.thrift"
        );
        assertThat(projectDir.resolve("build/generated-sources/thrift/gen-java")
                             .resolve("com/linecorp/thrift/plugin/test/TestService.java")
        ).exists();
        assertThat(projectDir.resolve("build/generated-sources/thrift/gen-java")
                             .resolve("com/linecorp/thrift/plugin/test/TestStruct.java")
        ).exists();

        assertThat(projectDir.resolve("build/classes/java/main")
                             .resolve("com/linecorp/thrift/plugin/test/TestStruct.class")
        ).exists();
    }

    @ParameterizedTest
    @ValueSource(strings = { "7.6", "8.0", "8.1" })
    public void generateNonJava(String version) throws Exception {
        copyDefaultThriftFiles();
        Files.write(buildFile,
                    Collections.singletonList(
                            "    compileThrift {\n" +
                            "        thriftExecutable \"" + thriftPathExpression + "\"\n" +
                            "        sourceDir \"src/main/thrift\"\n" +
                            "        outputDir layout.buildDirectory.dir(\"generated-sources/thrift\")\n" +
                            "        nowarn true\n" +
                            "        strict true\n" +
                            "        verbose true\n" +
                            "        recurse true\n" +
                            "        debug true\n" +
                            "        generator 'perl'\n" +
                            "        generator 'html'\n" +
                            "        generator 'json'\n" +
                            "    }\n"),
                    StandardOpenOption.APPEND);

        final BuildResult gradle = GradleRunner.create()
                                               .withProjectDir(projectDir.toFile())
                                               .withGradleVersion(version)
                                               .withArguments(Arrays.asList("compileThrift", "--info"))
                                               .withPluginClasspath()
                                               .build();

        assertThat(gradle.task(":compileThrift").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(gradle.getOutput()).contains(
                "-o " +
                projectDir.toFile().getCanonicalPath() +
                "/build/generated-sources/thrift --gen java --gen perl --gen html --gen json" +
                " -r -nowarn -strict -v -debug " +
                projectDir.toFile().getCanonicalPath() +
                "/src/main/thrift/test.thrift"
        );
        assertThat(projectDir.resolve("build/generated-sources/thrift/gen-html"))
                .isDirectoryContaining(path -> "index.html".equals(path.getFileName().toString()))
                .isDirectoryContaining(path -> "test.html".equals(path.getFileName().toString()))
                .isDirectoryContaining(path -> "style.css".equals(path.getFileName().toString()));
        assertThat(projectDir.resolve("build/generated-sources/thrift/gen-perl"))
                .isDirectoryContaining(path -> "Constants.pm".equals(path.getFileName().toString()))
                .isDirectoryContaining(path -> "Types.pm".equals(path.getFileName().toString()))
                .isDirectoryContaining(path -> "TestService.pm".equals(path.getFileName().toString()));
        assertThat(projectDir.resolve("build/generated-sources/thrift/gen-json/test.json")).exists();
    }

    @ParameterizedTest
    @ValueSource(strings = { "7.6", "8.0", "8.1" })
    public void incremental(String version) throws Exception {
        copyDefaultThriftFiles();
        Files.write(buildFile,
                    Collections.singletonList(
                            "    import com.linecorp.thrift.plugin.CompileThrift\n" +
                            "    compileThrift {\n" +
                            "        thriftExecutable \"" + thriftPathExpression + "\"\n" +
                            "        sourceDir \"src/main/thrift\"\n" +
                            "        outputDir layout.buildDirectory.dir(\"generated-sources/thrift\")\n" +
                            "        nowarn true\n" +
                            "        strict true\n" +
                            "        verbose true\n" +
                            "        recurse true\n" +
                            "        debug true\n" +
                            "        createGenFolder false\n" +
                            "        generator 'java'\n" +
                            "    }\n"
                    ),
                    StandardOpenOption.APPEND);

        final Path test2Thrift = projectDir.resolve("src/main/thrift/test2.thrift");
        Files.copy(Paths.get("src/test/resources/test2.thrift"),
                   test2Thrift);

        final GradleRunner runner = GradleRunner.create()
                                                .withProjectDir(projectDir.toFile())
                                                .withGradleVersion(version)
                                                .withArguments(Arrays.asList("compileThrift", "--info"))
                                                .withPluginClasspath();
        runner.build();

        Files.write(test2Thrift,
                    Collections.singletonList(
                            "    struct TestStruct3 {\n" +
                            "        1:required i32 num = 0,\n" +
                            "    }\n"
                    ),
                    StandardOpenOption.APPEND);

        final BuildResult gradle = runner.build();

        assertThat(gradle.task(":compileThrift").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(gradle.getOutput()).doesNotContain("test.thrift");
        assertThat(gradle.getOutput()).contains("test2.thrift");
    }

    private void copyDefaultThriftFiles() throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/thrift"));
        Files.copy(Paths.get("src/test/resources/test.thrift"),
                   projectDir.resolve("src/main/thrift/test.thrift"));
    }

}
