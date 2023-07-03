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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ThriftPlugin implements Plugin<Project> {

    public static final String COMPILE_THRIFT_TASK = "compileThrift";

    @Override
    public void apply(Project project) {
        final CompileThrift compileThrift = project.getTasks().create(COMPILE_THRIFT_TASK, CompileThrift.class);
        compileThrift.sourceDir(project.getProjectDir() + "src/main/thrift");
        compileThrift.outputDir(project.getBuildDir() + "/generated-sources/thrift");
    }
}
