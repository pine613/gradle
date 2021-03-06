/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.internal.service.scopes;

import org.gradle.api.Action;
import org.gradle.api.AntBuilder;
import org.gradle.api.component.SoftwareComponentContainer;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.internal.*;
import org.gradle.api.internal.artifacts.DependencyManagementServices;
import org.gradle.api.internal.artifacts.ModuleInternal;
import org.gradle.api.internal.artifacts.ProjectBackedModule;
import org.gradle.api.internal.artifacts.configurations.DependencyMetaDataProvider;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.component.ComponentRegistry;
import org.gradle.api.internal.component.DefaultSoftwareComponentContainer;
import org.gradle.api.internal.file.*;
import org.gradle.api.internal.initialization.DefaultScriptHandlerFactory;
import org.gradle.api.internal.initialization.ScriptHandlerFactory;
import org.gradle.api.internal.plugins.*;
import org.gradle.api.internal.project.DefaultAntBuilderFactory;
import org.gradle.api.internal.project.DeferredProjectConfiguration;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.project.ant.AntLoggingAdapter;
import org.gradle.api.internal.project.taskfactory.ITaskFactory;
import org.gradle.api.internal.tasks.DefaultTaskContainerFactory;
import org.gradle.api.internal.tasks.TaskContainerInternal;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.configuration.project.DefaultProjectConfigurationActionContainer;
import org.gradle.configuration.project.ProjectConfigurationActionContainer;
import org.gradle.initialization.ProjectAccessListener;
import org.gradle.internal.Factory;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.DefaultServiceRegistry;
import org.gradle.internal.service.ServiceRegistration;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.typeconversion.DefaultTypeConverter;
import org.gradle.internal.typeconversion.TypeConverter;
import org.gradle.logging.LoggingManagerInternal;
import org.gradle.model.internal.inspect.ModelRuleExtractor;
import org.gradle.model.internal.inspect.ModelRuleSourceDetector;
import org.gradle.model.internal.registry.DefaultModelRegistry;
import org.gradle.model.internal.registry.ModelRegistry;
import org.gradle.process.internal.DefaultExecActionFactory;
import org.gradle.process.internal.ExecActionFactory;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.gradle.tooling.provider.model.internal.DefaultToolingModelBuilderRegistry;

import java.io.File;

/**
 * Contains the services for a given project.
 */
public class ProjectScopeServices extends DefaultServiceRegistry {
    private final ProjectInternal project;

    public ProjectScopeServices(final ServiceRegistry parent, final ProjectInternal project) {
        super(parent);
        this.project = project;
        register(new Action<ServiceRegistration>() {
            public void execute(ServiceRegistration registration) {
                registration.add(DomainObjectContext.class, project);
                parent.get(DependencyManagementServices.class).addDslServices(registration);
                for (PluginServiceRegistry pluginServiceRegistry : parent.getAll(PluginServiceRegistry.class)) {
                    pluginServiceRegistry.registerProjectServices(registration);
                }
            }
        });
    }

    protected PluginRegistry createPluginRegistry(PluginRegistry parentRegistry) {
        return parentRegistry.createChild(project.getClassLoaderScope().createChild("plugins").lock());
    }

    protected DeferredProjectConfiguration createDeferredProjectConfiguration() {
        return new DeferredProjectConfiguration(project);
    }

    protected FileResolver createFileResolver(Factory<PatternSet> patternSetFactory) {
        return new BaseDirFileResolver(get(FileSystem.class), project.getProjectDir(), patternSetFactory);
    }

    protected LoggingManagerInternal createLoggingManager() {
        return getFactory(LoggingManagerInternal.class).create();
    }

    protected ProjectConfigurationActionContainer createProjectConfigurationActionContainer() {
        return new DefaultProjectConfigurationActionContainer();
    }

    protected DefaultFileOperations createFileOperations() {
        return new DefaultFileOperations(get(FileResolver.class), project.getTasks(), get(TemporaryFileProvider.class), get(Instantiator.class), get(FileLookup.class));
    }

    protected ExecActionFactory createExecActionFactory() {
        return new DefaultExecActionFactory(get(FileResolver.class));
    }

    protected TemporaryFileProvider createTemporaryFileProvider() {
        return new DefaultTemporaryFileProvider(new Factory<File>() {
            public File create() {
                return new File(project.getBuildDir(), "tmp");
            }
        });
    }

    protected Factory<AntBuilder> createAntBuilderFactory() {
        return new DefaultAntBuilderFactory(new AntLoggingAdapter(), project);
    }

    protected ToolingModelBuilderRegistry createToolingModelRegistry() {
        return new DefaultToolingModelBuilderRegistry();
    }

    protected PluginManagerInternal createPluginManager(Instantiator instantiator) {
        PluginApplicator applicator = new RuleBasedPluginApplicator<ProjectInternal>(project, get(ModelRuleExtractor.class), get(ModelRuleSourceDetector.class));
        return instantiator.newInstance(DefaultPluginManager.class, get(PluginRegistry.class), new DependencyInjectingInstantiator(this), applicator);
    }

    protected ITaskFactory createTaskFactory(ITaskFactory parentFactory) {
        return parentFactory.createChild(project, new ClassGeneratorBackedInstantiator(get(ClassGenerator.class), new DependencyInjectingInstantiator(this)));
    }

    protected Factory<TaskContainerInternal> createTaskContainerInternal() {
        return new DefaultTaskContainerFactory(get(ModelRegistry.class), get(Instantiator.class), get(ITaskFactory.class), project, get(ProjectAccessListener.class));
    }

    protected SoftwareComponentContainer createSoftwareComponentContainer() {
        Instantiator instantiator = get(Instantiator.class);
        return instantiator.newInstance(DefaultSoftwareComponentContainer.class, instantiator);
    }

    protected ProjectFinder createProjectFinder() {
        return new ProjectFinder() {
            public ProjectInternal getProject(String path) {
                return project.project(path);
            }
        };
    }

    protected ModelRegistry createModelRegistry(ModelRuleExtractor ruleExtractor) {
        return new DefaultModelRegistry(ruleExtractor);
    }

    protected ScriptHandler createScriptHandler() {
        ScriptHandlerFactory factory = new DefaultScriptHandlerFactory(
                get(DependencyManagementServices.class),
                get(FileResolver.class),
                get(DependencyMetaDataProvider.class));
        return factory.create(project.getBuildScriptSource(), project.getClassLoaderScope(), project);
    }

    protected DependencyMetaDataProvider createDependencyMetaDataProvider() {
        return new ProjectBackedModuleMetaDataProvider();
    }

    protected ServiceRegistryFactory createServiceRegistryFactory(final ServiceRegistry services) {
        return new ServiceRegistryFactory() {
            public ServiceRegistry createFor(Object domainObject) {
                if (domainObject instanceof TaskInternal) {
                    return new TaskScopeServices(services, project, (TaskInternal) domainObject);
                }
                throw new UnsupportedOperationException();
            }
        };
    }

    protected ComponentRegistry createComponentRegistry() {
        return new ComponentRegistry();
    }

    protected TypeConverter createTypeConverter(FileResolver fileResolver) {
        return new DefaultTypeConverter(fileResolver);
    }

    private class ProjectBackedModuleMetaDataProvider implements DependencyMetaDataProvider {
        public ModuleInternal getModule() {
            return new ProjectBackedModule(project);
        }
    }
}
