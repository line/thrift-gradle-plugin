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

import java.io.File;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public abstract class CompileThriftExtension {
    public abstract Property<Boolean> getVerbose();

    public abstract Property<String> getThriftExecutable();

    public abstract Property<Boolean> getNowarn();

    public abstract Property<Boolean> getStrict();

    public abstract Property<Boolean> getRecurse();

    public abstract Property<Boolean> getDebug();

    public abstract Property<Boolean> getCreateGenFolder();

    public abstract MapProperty<String, String> getGenerators();

    public abstract ConfigurableFileCollection getSourceItems();

    public abstract ConfigurableFileCollection getIncludeDirs();

    public abstract DirectoryProperty getOutputDir();

    public void verbose(boolean verbose) {
        getVerbose().set(verbose);
    }

    public void thriftExecutable(String thriftExecutable) {
        getThriftExecutable().set(thriftExecutable);
    }

    public void nowarn(boolean nowarn) {
        getNowarn().set(nowarn);
    }

    public void strict(boolean strict) {
        getStrict().set(strict);
    }

    public void debug(boolean debug) {
        getDebug().set(debug);
    }

    public void recurse(boolean recurse) {
        getRecurse().set(recurse);
    }

    public void createGenFolder(boolean createGenFolder) {
        getCreateGenFolder().set(createGenFolder);
    }

    public void sourceDir(Object file) {
        getSourceItems().from(file);
    }

    public void sourceItems(Object... files) {
        getSourceItems().from(files);
    }

    public void outputDir(File outputDir) {
        getOutputDir().set(outputDir);
    }

    public void outputDir(String outputDir) {
        getOutputDir().set(new File(outputDir));
    }

    public void outputDir(Directory outputDir) {
        getOutputDir().set(outputDir);
    }

    public void outputDir(Provider<? extends Directory> outputDir) {
        getOutputDir().set(outputDir);
    }

    public void generator(String key, String... values) {
        getGenerators().put(key, String.join(",", values));
    }
}
