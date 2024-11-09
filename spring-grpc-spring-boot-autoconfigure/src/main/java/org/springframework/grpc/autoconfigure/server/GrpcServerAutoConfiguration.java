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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.autoconfigure.common.codec.GrpcCodecConfiguration;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.GrpcServiceDiscoverer;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

import io.grpc.BindableService;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;

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
@AutoConfigureAfter(GrpcServerFactoryAutoConfiguration.class)
@ConditionalOnClass(BindableService.class)
@ConditionalOnBean(BindableService.class)
@EnableConfigurationProperties(GrpcServerProperties.class)
@Import({ GrpcCodecConfiguration.class })
public class GrpcServerAutoConfiguration {

	private final GrpcServerProperties properties;

	GrpcServerAutoConfiguration(GrpcServerProperties properties) {
		this.properties = properties;
	}

	@ConditionalOnBean(GrpcServerFactory.class)
	@ConditionalOnMissingBean
	@Bean
	GrpcServerLifecycle grpcServerLifecycle(GrpcServerFactory factory, ApplicationEventPublisher eventPublisher) {
		return new GrpcServerLifecycle(factory, this.properties.getShutdownGracePeriod(), eventPublisher);
	}

	@ConditionalOnMissingBean
	@Bean
	ServerBuilderCustomizers serverBuilderCustomizers(ObjectProvider<ServerBuilderCustomizer<?>> customizers) {
		return new ServerBuilderCustomizers(customizers.orderedStream().toList());
	}

	@ConditionalOnMissingBean
	@Bean
	GrpcServiceDiscoverer grpcServiceDiscoverer(ObjectProvider<BindableService> bindableServicesProvider,
			ObjectProvider<ServerInterceptor> serverInterceptorsProvider, ApplicationContext applicationContext) {
		return new DefaultGrpcServiceDiscoverer(bindableServicesProvider, serverInterceptorsProvider,
				applicationContext);
	}

	@ConditionalOnBean(CompressorRegistry.class)
	@Bean
	<T extends ServerBuilder<T>> ServerBuilderCustomizer<T> compressionServerConfigurer(CompressorRegistry registry) {
		return builder -> builder.compressorRegistry(registry);
	}

	@ConditionalOnBean(DecompressorRegistry.class)
	@Bean
	<T extends ServerBuilder<T>> ServerBuilderCustomizer<T> decompressionServerConfigurer(
			DecompressorRegistry registry) {
		return builder -> builder.decompressorRegistry(registry);
	}

}
