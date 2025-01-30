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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

public class GrpcClientRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

	private GenericApplicationContext context;

	private boolean initialized = false;

	private GrpcClientRegistry registry;

	private void initialize(GenericApplicationContext context) {
		if (this.initialized) {
			return;
		}
		this.initialized = true;
		this.registry = new GrpcClientRegistry(context);
		if (context.getBeanNamesForType(GrpcClientRegistryCustomizer.class).length > 0) {
			List<GrpcClientRegistryCustomizer> values = new ArrayList<>(
					context.getBeansOfType(GrpcClientRegistryCustomizer.class).values());
			AnnotationAwareOrderComparator.sort(values);
			values.forEach(customizer -> customizer.customize(this.registry));
		}
		this.registry.close();
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		if (this.context != null) {
			initialize(this.context);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (applicationContext instanceof GenericApplicationContext generic) {
			this.context = generic;
		}
		else {
			throw new IllegalStateException("ApplicationContext must be a GenericApplicationContext");
		}
	}

}
