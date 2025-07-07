/*
 * Based on jruyi/thrift-gradle-plugin/src/main/groovy/org/jruyi/gradle/thrift/plugin/CompileThrift.groovy
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

public abstract class CompileThrift extends DefaultTask {

    // Forked from https://github.com/jruyi/thrift-gradle-plugin/blob/aef83035ffe141b0507f5a2254aa1f7193976c4a/src/main/groovy/org/jruyi/gradle/thrift/plugin/CompileThrift.groovy

    @Incremental
    @InputFiles
    public abstract ConfigurableFileCollection getSourceItems();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @InputFiles
    public abstract ConfigurableFileCollection getIncludeDirs();

    @Input
    @Optional
    public abstract Property<String> getThriftExecutable();

    @Input
    @Optional
    public abstract Property<Boolean> getCreateGenFolder();

    @Input
    @Optional
    public abstract Property<Boolean> getNowarn();

    @Input
    @Optional
    public abstract Property<Boolean> getStrict();

    @Input
    @Optional
    public abstract Property<Boolean> getRecurse();

    @Input
    @Optional
    public abstract Property<Boolean> getDebug();

    @Input
    @Optional
    public abstract Property<Boolean> getVerbose();

    @Input
    public abstract MapProperty<String, String> getGenerators();

    @Input
    @Optional
    public abstract Property<Boolean> getAutoDownload();

    @Input
    @Optional
    public abstract Property<String> getThriftVersion();

    @Input
    @Optional
    public abstract Property<String> getThriftRepository();

    @InputFiles
    @Optional
    public abstract DirectoryProperty getLocalBinaryDir();

    @Inject
    public abstract ExecOperations getExecOperations();

    @Inject
    public abstract ObjectFactory getObjectFactory();

    @TaskAction
    void compileThrift(InputChanges inputs) {
        if (!inputs.isIncremental()) {
            compileAll();
            return;
        }

        final List<File> changedFiles = new ArrayList<>();
        final Iterable<FileChange> fileChanges = inputs.getFileChanges(getSourceItems());
        for (FileChange change : fileChanges) {
            if (change.getChangeType() == ChangeType.REMOVED) {
                compileAll();
                return;
            }
            if (change.getFile().getName().endsWith(".thrift")) {
                changedFiles.add(change.getFile());
            }
        }

        final File outputDirFile = getOutputDir().getAsFile().get();
        if (!outputDirFile.exists() && !outputDirFile.mkdirs()) {
            throw new GradleException(
                    "Could not create thrift output directory: " + outputDirFile.getAbsolutePath());
        }

        changedFiles.forEach(changedFile -> {
            compile(changedFile.getAbsolutePath());
        });
    }

    void compileAll() {
        final File outputDirFile = getOutputDir().getAsFile().get();
        // Using same method of File#deleteDir in groovy.
        if (!ResourceGroovyMethods.deleteDir(outputDirFile)) {
            throw new GradleException(
                    "Could not delete thrift output directory: " + outputDirFile.getAbsolutePath());
        }

        if (!outputDirFile.mkdirs()) {
            throw new GradleException(
                    "Could not create thrift output directory: " + outputDirFile.getAbsolutePath());
        }

        // expand all items.
        final Set<String> resolvedSourceItems = new HashSet<>();
        getSourceItems().forEach(sourceItem -> {
            if (sourceItem.isFile()) {
                resolvedSourceItems.add(sourceItem.getAbsolutePath());
            } else if (sourceItem.isDirectory()) {
                try {
                    getObjectFactory().fileTree().from(sourceItem.getCanonicalPath()).matching(files -> {
                        files.include("**/*.thrift");
                    }).forEach(file -> resolvedSourceItems.add(file.getAbsolutePath()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else if (!sourceItem.exists()) {
                getLogger().warn("Could not find {}. Will ignore it", sourceItem);
            } else {
                getLogger().warn("Unable to handle {}. Will ignore it", sourceItem);
            }
        });

        getLogger().info("Items to be generated for: {}", resolvedSourceItems);

        resolvedSourceItems.forEach(this::compile);
    }

    void compile(String source) {
        final File outputDirFile = getOutputDir().getAsFile().get();
        final String thriftExecutable = resolveThriftExecutable();
        final List<String> cmdLine = new ArrayList<>(
                Arrays.asList(thriftExecutable,
                              getCreateGenFolder().getOrElse(true) ? "-o" : "-out",
                              outputDirFile.getAbsolutePath()));
        getGenerators().get().forEach((key, value) -> {
            cmdLine.add("--gen");

            String cmd = key.trim();
            final String options = value.trim();
            if (!options.isEmpty()) {
                cmd += ':' + options;
            }
            cmdLine.add(cmd);
        });

        getIncludeDirs().forEach(includeDir -> {
            cmdLine.add("-I");
            cmdLine.add(includeDir.getAbsolutePath());
        });

        if (getRecurse().getOrElse(false)) {
            cmdLine.add("-r");
        }
        if (getNowarn().getOrElse(false)) {
            cmdLine.add("-nowarn");
        }
        if (getStrict().getOrElse(false)) {
            cmdLine.add("-strict");
        }
        if (getVerbose().getOrElse(false)) {
            cmdLine.add("-v");
        }
        if (getDebug().getOrElse(false)) {
            cmdLine.add("-debug");
        }
        cmdLine.add(source);

        final ExecResult result = getExecOperations().exec(execSpec -> {
            execSpec.commandLine(cmdLine);
        });

        final int exitCode = result.getExitValue();
        if (exitCode != 0) {
            throw new GradleException("Failed to compile " + source + ", exit=" + exitCode);
        }
    }

    private String resolveThriftExecutable() {
        final String configuredExecutable = getThriftExecutable().getOrElse("thrift");

        if (!"thrift".equals(configuredExecutable) || !getAutoDownload().getOrElse(true)) {
            return configuredExecutable;
        }

        final ThriftBinaryDownloader downloader = new ThriftBinaryDownloader(
                getLogger(),
                getThriftRepository().getOrElse(ThriftPlugin.DEFAULT_THRIFT_REPOSITORY),
                getThriftVersion().getOrElse(ThriftPlugin.DEFAULT_THRIFT_VERSION),
                getLocalBinaryDir().getAsFile().get()
        );

        final File downloadedBinary = downloader.downloadBinary();
        return downloadedBinary.getAbsolutePath();
    }
}
