/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.platform.base.internal.registry;

import com.google.common.collect.ImmutableList;
import org.gradle.api.Nullable;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.model.internal.core.ModelActionRole;
import org.gradle.model.internal.core.ModelReference;
import org.gradle.model.internal.core.ModelView;
import org.gradle.model.internal.core.MutableModelNode;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.gradle.model.internal.inspect.*;
import org.gradle.model.internal.type.ModelType;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.ComponentBinaries;
import org.gradle.platform.base.ComponentSpec;

import java.util.List;

import static org.gradle.model.internal.core.NodePredicate.allLinksTransitive;

public class ComponentBinariesModelRuleExtractor extends AbstractAnnotationDrivenComponentModelRuleExtractor<ComponentBinaries> {
    @Nullable
    @Override
    public <R, S> ExtractedModelRule registration(MethodRuleDefinition<R, S> ruleDefinition, MethodModelRuleExtractionContext context) {
        return createRegistration(ruleDefinition, context);
    }

    private <R, S extends BinarySpec, C extends ComponentSpec> ExtractedModelRule createRegistration(final MethodRuleDefinition<R, ?> ruleDefinition, ValidationProblemCollector problems) {
        RuleMethodDataCollector dataCollector = new RuleMethodDataCollector();
        visitAndVerifyMethodSignature(dataCollector, ruleDefinition, problems);
        if (problems.hasProblems()) {
            return null;
        }

        final Class<S> binaryType = dataCollector.getParameterType(BinarySpec.class);
        final Class<C> componentType = dataCollector.getParameterType(ComponentSpec.class);
        return new ExtractedComponentBinariesRule<R, S, C>(componentType, binaryType, ruleDefinition);
    }

    private void visitAndVerifyMethodSignature(RuleMethodDataCollector dataCollector, MethodRuleDefinition<?, ?> ruleDefinition, ValidationProblemCollector problems) {
        validateIsVoidMethod(ruleDefinition, problems);
        visitSubject(dataCollector, ruleDefinition, BinarySpec.class, problems);
        visitDependency(dataCollector, ruleDefinition, ModelType.of(ComponentSpec.class), problems);
    }

    private static class ComponentBinariesRule<R, S extends BinarySpec, C extends ComponentSpec> extends ModelMapBasedRule<R, S, ComponentSpec, C> {
        private final Class<S> binaryType;

        public ComponentBinariesRule(ModelReference<C> subject, final Class<C> componentType, final Class<S> binaryType, MethodRuleDefinition<R, ?> ruleDefinition, ModelRuleInvoker<R> ruleInvoker) {
            super(subject, componentType, ruleDefinition, ruleInvoker);
            this.binaryType = binaryType;
        }

        protected void execute(final MutableModelNode modelNode, final C component, final List<ModelView<?>> inputs) {
            invoke(inputs, component.getBinaries().withType(binaryType), component);
        }
    }

    private static class ExtractedComponentBinariesRule<R, S extends BinarySpec, C extends ComponentSpec> implements ExtractedModelRule {
        private final Class<C> componentType;
        private final Class<S> binaryType;
        private final MethodRuleDefinition<R, ?> ruleDefinition;

        public ExtractedComponentBinariesRule(Class<C> componentType, Class<S> binaryType, MethodRuleDefinition<R, ?> ruleDefinition) {
            this.componentType = componentType;
            this.binaryType = binaryType;
            this.ruleDefinition = ruleDefinition;
        }

        @Override
        public ModelRuleDescriptor getDescriptor() {
            return null;
        }

        @Override
        public void apply(MethodModelRuleApplicationContext context, MutableModelNode target) {
            ModelReference<C> subject = ModelReference.of(componentType);
            ComponentBinariesRule<R, S, C> componentBinariesRule = new ComponentBinariesRule<R, S, C>(subject, componentType, binaryType, ruleDefinition, context.invokerFor(ruleDefinition));
            target.applyTo(allLinksTransitive(), ModelActionRole.Finalize, componentBinariesRule);
        }

        @Override
        public List<? extends Class<?>> getRuleDependencies() {
            return ImmutableList.of(ComponentModelBasePlugin.class);
        }
    }
}
