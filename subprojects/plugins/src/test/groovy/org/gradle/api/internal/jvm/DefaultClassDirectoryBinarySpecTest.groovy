/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.jvm

import org.gradle.api.internal.project.taskfactory.ITaskFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.jvm.platform.JavaPlatform
import org.gradle.jvm.toolchain.JavaToolChain
import spock.lang.Specification

public class DefaultClassDirectoryBinarySpecTest extends Specification {
    def "has a useful toString() representation"() {
        expect:
        def binary = binary(name)
        binary.toString() == displayName
        binary.displayName == displayName

        where:
        name           | displayName
        'mainClasses'  | 'classes \'main\''
        'otherClasses' | 'classes \'other\''
        'otherBinary'  | 'classes \'otherBinary\''
    }

    private DefaultClassDirectoryBinarySpec binary(String name) {
        new DefaultClassDirectoryBinarySpec(name, Stub(SourceSet), Stub(JavaToolChain), Stub(JavaPlatform), DirectInstantiator.INSTANCE, Mock(ITaskFactory))
    }
}
