/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testkit.runner.internal.feature;

import org.gradle.testkit.runner.internal.TestKitFeature;
import org.gradle.testkit.runner.UnsupportedFeatureException;
import org.gradle.util.GradleVersion;

public class BuildResultTasksFeatureCheck implements FeatureCheck {

    private final GradleVersion targetGradleVersion;

    public BuildResultTasksFeatureCheck(GradleVersion targetGradleVersion) {
        this.targetGradleVersion = targetGradleVersion;
    }

    @Override
    public void verify() {
        if (!supportsVersion()) {
            throw new UnsupportedFeatureException("capture executed tasks with the GradleRunner", targetGradleVersion, TestKitFeature.CAPTURE_BUILD_RESULT_TASKS.getSince());
        }
    }

    private boolean supportsVersion() {
        return targetGradleVersion.compareTo(TestKitFeature.CAPTURE_BUILD_RESULT_TASKS.getSince()) >= 0;
    }
}
