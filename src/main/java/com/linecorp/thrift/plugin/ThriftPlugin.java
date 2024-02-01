/*
 * Based on jruyi/thrift-gradle-plugin/src/main/groovy/org/jruyi/gradle/thrift/plugin/ThriftPlugin.groovy
 * Modified 2023 LINE Corporation
 *
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

public class ThriftPlugin implements Plugin<Project> {

    // Forked from https://github.com/jruyi/thrift-gradle-plugin/blob/aef83035ffe141b0507f5a2254aa1f7193976c4a/src/main/groovy/org/jruyi/gradle/thrift/plugin/ThriftPlugin.groovy

    public static final String COMPILE_THRIFT_TASK = "compileThrift";

    @Override
    public void apply(Project project) {
        final CompileThriftExtension extension = createExtension(project);
        final TaskProvider<CompileThrift> compileThriftTaskProvider = registerDefaultTask(project, extension);

        project.getPluginManager().withPlugin("java", appliedPlugin -> {
            // In the future if we start to support kotlin, we may need to let user choose which one they want
            // to generate.
            extension.getGenerators().putAll(extension.getAutoDetectPlugin().map(autoDetect -> {
                final Map<String, String> map = new HashMap<>();
                if (autoDetect) {
                    map.put("java", "");
                }
                return map;
            }));

            project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME).configure(task -> {
                task.dependsOn(extension.getGenerators().flatMap(generators -> {
                    if (generators.containsKey("java")) {
                        return compileThriftTaskProvider;
                    } else {
                        return project.provider(ArrayList::new);
                    }
                }));
            });

            final SourceSetContainer sourceSetContainer =
                    project.getExtensions().getByType(SourceSetContainer.class);
            final SourceSet mainSourceSet = sourceSetContainer.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            final Provider<Object> outputDirectory = extension.getGenerators().flatMap(generators -> {
                if (generators.containsKey("java")) {
                    return compileThriftTaskProvider
                            .flatMap(CompileThrift::getOutputDir)
                            .zip(compileThriftTaskProvider.flatMap(CompileThrift::getCreateGenFolder),
                                 (directory, genFolder) -> {
                                     if (genFolder) {
                                         return directory.dir("gen-java");
                                     } else {
                                         return directory;
                                     }
                                 });
                } else {
                    return project.provider(ArrayList::new);
                }
            });
            mainSourceSet.getJava().srcDir(outputDirectory);
        });
    }

    private TaskProvider<CompileThrift> registerDefaultTask(Project project,
                                                            CompileThriftExtension extension) {
        final TaskProvider<CompileThrift> compileThriftTaskProvider =
                project.getTasks().register(COMPILE_THRIFT_TASK, CompileThrift.class);

        compileThriftTaskProvider.configure(task -> {
            task.getThriftExecutable().set(extension.getThriftExecutable());
            task.getNowarn().set(extension.getNowarn());
            task.getVerbose().set(extension.getVerbose());
            task.getStrict().set(extension.getStrict());
            task.getDebug().set(extension.getDebug());
            task.getRecurse().set(extension.getRecurse());
            task.getGenerators().set(extension.getGenerators());
            task.getCreateGenFolder().set(extension.getCreateGenFolder());
            task.getIncludeDirs().setFrom(extension.getIncludeDirs());
            task.getOutputDir().set(extension.getOutputDir());

            // Give default value for ConfigurableFileCollection,
            // If we set this at createExtension, it's not easy to remove set one from Collection when we want
            // to change in build.gradle. Because current convention will only allow us to append more items.
            final Directory dir = project.getLayout().getProjectDirectory().dir("src/main/thrift");
            // Looks like getElements can return Provider.
            task.getSourceItems().setFrom(extension.getSourceItems().getElements().map(locations -> {
                if (locations.isEmpty()) {
                    return Collections.singleton(dir);
                }
                return locations;
            }));
        });
        return compileThriftTaskProvider;
    }

    private CompileThriftExtension createExtension(Project project) {
        final CompileThriftExtension extension = project.getExtensions().create("compileThrift",
                                                                                CompileThriftExtension.class);
        extension.getThriftExecutable().convention("thrift");
        extension.getNowarn().convention(false);
        extension.getVerbose().convention(false);
        extension.getStrict().convention(false);
        extension.getDebug().convention(false);
        extension.getRecurse().convention(false);
        extension.getAutoDetectPlugin().convention(true);
        extension.getCreateGenFolder().convention(true);
        extension.getOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("generated-sources/thrift"));
        return extension;
    }
}
