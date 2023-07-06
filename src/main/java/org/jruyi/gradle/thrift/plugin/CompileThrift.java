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
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jruyi.gradle.thrift.plugin;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
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
    File outputDir;

    @Input
    Set<File> includeDirs = new HashSet<>();

    @Input
    String thriftExecutable = "thrift";

    @Input
    final Map<String, String> generators = new LinkedHashMap<>();

    @Input
    boolean createGenFolder = true;

    @Input
    boolean recurse;

    @Input
    boolean allowNegKeys;

    @Input
    boolean allow64bitsConsts;

    @Internal
    boolean nowarn;

    @Internal
    boolean strict;

    @Internal
    boolean verbose;

    @Internal
    boolean debug;

    public File getOutputDir() {
        return outputDir;
    }

    public Set<File> getIncludeDirs() {
        return includeDirs;
    }

    public String getThriftExecutable() {
        return thriftExecutable;
    }

    public Map<String, String> getGenerators() {
        return generators;
    }

    public boolean isCreateGenFolder() {
        return createGenFolder;
    }

    public boolean isRecurse() {
        return recurse;
    }

    public boolean isAllowNegKeys() {
        return allowNegKeys;
    }

    public boolean isAllow64bitsConsts() {
        return allow64bitsConsts;
    }

    public boolean isNowarn() {
        return nowarn;
    }

    public boolean isStrict() {
        return strict;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setThriftExecutable(String thriftExecutable) {
        this.thriftExecutable = thriftExecutable;
    }

    public void setCreateGenFolder(boolean createGenFolder) {
        this.createGenFolder = createGenFolder;
    }

    public void setRecurse(boolean recurse) {
        this.recurse = recurse;
    }

    public void setAllowNegKeys(boolean allowNegKeys) {
        this.allowNegKeys = allowNegKeys;
    }

    public void setAllow64bitsConsts(boolean allow64bitsConsts) {
        this.allow64bitsConsts = allow64bitsConsts;
    }

    public void setNowarn(boolean nowarn) {
        this.nowarn = nowarn;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    void thriftExecutable(Object thriftExecutable) {
        this.thriftExecutable = String.valueOf(thriftExecutable);
    }

    void sourceDir(Object sourceDir) {
        sourceItems(sourceDir);
    }

    void sourceItems(Object... sourceItems) {
        for (Object sourceItem : sourceItems) {
            getSourceItems().from(convertToFile(sourceItem));
        }
    }

    void outputDir(Object outputDir) {
        if (!(outputDir instanceof File)) {
            outputDir = getProject().file(outputDir);
        }
        if (this.outputDir == outputDir) {
            return;
        }
        final File oldOutputDir = currentOutputDir();
        this.outputDir = (File) outputDir;
        addSourceDir(oldOutputDir);
    }

    void includeDir(Object includeDir) {
        if (!(includeDir instanceof File)) {
            includeDir = getProject().file(includeDir);
        }

        includeDirs.add((File) includeDir);
    }

    void generator(String gen, String... args) {
        final String options;
        if (args == null || args.length < 1) {
            options = "";
        } else {
            final int n = args.length;
            for (int i = 0; i < n; ++i) {
                args[i] = args[i].trim();
            }
            options = String.join(",", args);
        }
        generators.put(String.valueOf(gen).trim(), options);
    }

    void createGenFolder(boolean createGenFolder) {
        if (this.createGenFolder == createGenFolder) {
            return;
        }
        final File oldOutputDir = currentOutputDir();
        this.createGenFolder = createGenFolder;
        addSourceDir(oldOutputDir);
    }

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

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new GradleException(
                    "Could not create thrift output directory: " + outputDir.getAbsolutePath());
        }

        changedFiles.forEach(changedFile -> {
            compile(changedFile.getAbsolutePath());
        });
    }

    void compileAll() {
        // Using same method of File#deleteDir in groovy.
        if (!ResourceGroovyMethods.deleteDir(outputDir)) {
            throw new GradleException(
                    "Could not delete thrift output directory: " + outputDir.getAbsolutePath());
        }

        if (!outputDir.mkdirs()) {
            throw new GradleException(
                    "Could not create thrift output directory: " + outputDir.getAbsolutePath());
        }

        // expand all items.
        final Set<String> resolvedSourceItems = new HashSet<>();
        getSourceItems().forEach(sourceItem -> {
            if (sourceItem.isFile()) {
                resolvedSourceItems.add(sourceItem.getAbsolutePath());
            } else if (sourceItem.isDirectory()) {
                try {
                    getProject().fileTree(sourceItem.getCanonicalPath(), files -> {
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
        final List<String> cmdLine = new ArrayList<>(
                Arrays.asList(thriftExecutable, createGenFolder ? "-o" : "-out", outputDir.getAbsolutePath()));
        generators.forEach((key, value) -> {
            cmdLine.add("--gen");

            String cmd = key.trim();
            final String options = value.trim();
            if (!options.isEmpty()) {
                cmd += ':' + options;
            }
            cmdLine.add(cmd);
        });

        includeDirs.forEach(includeDir -> {
            cmdLine.add("-I");
            cmdLine.add(includeDir.getAbsolutePath());
        });

        if (recurse) {
            cmdLine.add("-r");
        }
        if (nowarn) {
            cmdLine.add("-nowarn");
        }
        if (strict) {
            cmdLine.add("-strict");
        }
        if (verbose) {
            cmdLine.add("-v");
        }
        if (debug) {
            cmdLine.add("-debug");
        }
        cmdLine.add(source);

        final ExecResult result = getProject().exec(execSpec -> {
            execSpec.commandLine(cmdLine);
        });

        final int exitCode = result.getExitValue();
        if (exitCode != 0) {
            throw new GradleException("Failed to compile " + source + ", exit=" + exitCode);
        }
    }

    void makeAsDependency(File oldOutputDir) {
        final Task compileJava = getProject().getTasks().findByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
        if (compileJava == null) {
            return;
        }

        generators.put("java", "");
        final File genJava;
        try {
            genJava = currentOutputDir().getCanonicalFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (genJava.equals(oldOutputDir)) {
            return;
        }

        final SourceSetContainer sourceSetContainer = getProject().getExtensions().getByType(
                SourceSetContainer.class);
        final SourceSet sourceSet = sourceSetContainer.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        if (oldOutputDir != null) {
            final Set<File> filteredJavaSrcDirs = sourceSet.getJava().getSourceDirectories().filter(
                    file -> !file.equals(oldOutputDir)).getFiles();
            sourceSet.getJava().setSrcDirs(filteredJavaSrcDirs);
        }
        sourceSet.getJava().srcDir(genJava.getAbsolutePath());

        compileJava.dependsOn(this);
    }

    private void addSourceDir(File oldOutputDir) {
        if (getProject().getPlugins().hasPlugin("java")) {
            makeAsDependency(oldOutputDir);
        } else {
            getProject().getPlugins().whenPluginAdded(plugin -> {
                if (plugin instanceof JavaPlugin) {
                    makeAsDependency(oldOutputDir);
                }
            });
        }
    }

    File convertToFile(Object item) {
        if (item instanceof File) {
            return (File) item;
        }

        final File result = new File(item.toString());
        if (result.exists()) {
            return result;
        }

        return getProject().file(item);
    }

    private File currentOutputDir() {
        File currentOutputDir = outputDir;
        if (currentOutputDir == null) {
            return null;
        }
        if (createGenFolder) {
            currentOutputDir = new File(currentOutputDir, "gen-java");
        }
        return currentOutputDir;
    }
}
