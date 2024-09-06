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
package org.springframework.grpc.autoconfigure.server;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(GrpcServerFactory.class)
public class GrpcServiceAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public GrpcServiceDiscoverer defaultGrpcServiceDiscoverer(ApplicationContext applicationContext) {
		return new AnnotationGrpcServiceDiscoverer(applicationContext);
	}

	@ConditionalOnMissingBean
	@Bean
	public GrpcServerLifecycle grpcServerLifecycle(final GrpcServerFactory factory,
			final GrpcServerProperties properties, final ApplicationEventPublisher eventPublisher) {
		return new GrpcServerLifecycle(factory, properties.getShutdownGracePeriod(), eventPublisher);
	}

}

class AnnotationGrpcServiceDiscoverer implements GrpcServiceDiscoverer {

	private final ApplicationContext applicationContext;

	public AnnotationGrpcServiceDiscoverer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public Collection<ServerServiceDefinition> findGrpcServices() {
		return this.applicationContext.getBeansOfType(BindableService.class)
			.values()
			.stream()
			.map(bean -> bean.bindService())
			.collect(Collectors.toList());
	}

}
