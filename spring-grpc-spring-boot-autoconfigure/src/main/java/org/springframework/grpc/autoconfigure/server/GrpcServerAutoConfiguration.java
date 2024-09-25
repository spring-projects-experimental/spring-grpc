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

import io.grpc.BindableService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.DefaultGrpcServerFactory;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side components.
 * <p>
 * gRPC must be on the classpath and at least one {@link BindableService} bean registered
 * in the context in order for the auto-configuration to execute.
 *
 * @author David Syer
 * @author Chris Bono
 */
@AutoConfiguration
@ConditionalOnClass(BindableService.class)
@ConditionalOnBean(BindableService.class)
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcServerAutoConfiguration {

	private final GrpcServerProperties properties;

	GrpcServerAutoConfiguration(GrpcServerProperties properties) {
		this.properties = properties;
	}

	@ConditionalOnMissingBean(GrpcServerFactory.class)
	@Bean
	DefaultGrpcServerFactory<?> defaultGrpcServerFactory(ObjectProvider<BindableService> grpcServicesProvider,
			ObjectProvider<ServerBuilderCustomizer> builderCustomizersProvider) {
		DefaultGrpcServerFactory<?> factory = new DefaultGrpcServerFactory<>(this.properties.getAddress(),
				this.properties.getPort(), builderCustomizersProvider.orderedStream().toList());
		grpcServicesProvider.orderedStream().map(BindableService::bindService).forEach(factory::addService);
		return factory;
	}

	@ConditionalOnMissingBean
	@Bean
	GrpcServerLifecycle grpcServerLifecycle(GrpcServerFactory factory, ApplicationEventPublisher eventPublisher) {
		return new GrpcServerLifecycle(factory, this.properties.getShutdownGracePeriod(), eventPublisher);
	}

}
