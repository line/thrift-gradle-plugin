#!/usr/bin/env sh
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -ev

./gradlew publishToMavenLocal

./gradlew -p examples/java-project --refresh-dependencies --rerun-tasks clean check

./gradlew -p examples/disable-java-plugin-detection --refresh-dependencies --rerun-tasks clean check

./gradlew -p examples/no-java-plugin --refresh-dependencies --rerun-tasks clean compileThrift

./gradlew -p examples/define-task-manually --refresh-dependencies --rerun-tasks clean check
