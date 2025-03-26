/*
 * Copyright 2024-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.grpc.client;

import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

public class GrpcClientConfiguration implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
		Set<AnnotationAttributes> attrs = meta.getMergedRepeatableAnnotationAttributes(ImportGrpcClients.class,
				ImportGrpcClients.Container.class, false);
		for (AnnotationAttributes attr : attrs) {
			register(registry, meta, attr, meta.getClassName() + ".");
		}
		String name = GrpcClientRegistryPostProcessor.class.getName();
		if (!registry.containsBeanDefinition(name)) {
			registry.registerBeanDefinition(name, new RootBeanDefinition(GrpcClientRegistryPostProcessor.class));
		}
	}

	private void register(BeanDefinitionRegistry registry, AnnotationMetadata meta, AnnotationAttributes attr,
			String stem) {
		String target = attr.getString("target");
		String prefix = attr.getString("prefix");
		Class<?>[] types = attr.getClassArray("types");
		Class<? extends StubFactory<?>> factory = attr.getClass("factory");
		Class<?>[] basePackageClasses = attr.getClassArray("basePackageClasses");
		String[] basePackages = attr.getStringArray("basePackages");
		if (types.length == 0 && basePackageClasses.length == 0 && basePackages.length == 0) {
			basePackages = new String[] { ClassUtils.getPackageName(meta.getClassName()) };
		}
		register(registry, stem, target, prefix, types, factory, basePackageClasses, basePackages);
	}

	private void register(BeanDefinitionRegistry registry, String stem, String target, String prefix, Class<?>[] types,
			Class<? extends StubFactory<?>> factory, Class<?>[] basePackageClasses, String[] basePackages) {
		RootBeanDefinition beanDef = new RootBeanDefinition(SimpleGrpcClientRegistryCustomizer.class);
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		String name = target;
		beanDef.setInstanceSupplier(() -> new SimpleGrpcClientRegistryCustomizer(name, prefix, types, factory,
				basePackageClasses, basePackages));
		registry.registerBeanDefinition(stem + target, beanDef);
	}

	static class SimpleGrpcClientRegistryCustomizer implements GrpcClientRegistryCustomizer {

		private String target;

		private Class<?>[] types;

		private String prefix;

		private Class<?>[] basePackageClasses;

		private Class<? extends StubFactory<?>> type;

		private String[] basePackages;

		SimpleGrpcClientRegistryCustomizer(String target, String prefix, Class<?>[] types,
				Class<? extends StubFactory<?>> type, Class<?>[] basePackageClasses, String[] basePackages) {
			this.target = target;
			this.prefix = prefix;
			this.types = types;
			this.type = type;
			this.basePackageClasses = basePackageClasses;
			this.basePackages = basePackages;
		}

		@Override
		public void customize(GrpcClientRegistry registry) {
			registry.channel(this.target).prefix(this.prefix).register(this.types);
			registry.channel(this.target).prefix(this.prefix).scan(this.type).packageClasses(this.basePackageClasses);
			registry.channel(this.target).prefix(this.prefix).scan(this.type).packages(this.basePackages);
		}

	}

}
