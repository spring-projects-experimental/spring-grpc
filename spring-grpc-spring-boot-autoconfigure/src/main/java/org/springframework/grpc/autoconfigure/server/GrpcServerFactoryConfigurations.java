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

import java.util.List;

import javax.net.ssl.SSLException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.DefaultGrpcServerFactory;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.ShadedNettyGrpcServerFactory;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * Configurations for {@link GrpcServerFactory gRPC server factories}.
 *
 * @author Chris Bono
 */
class GrpcServerFactoryConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)
	@ConditionalOnMissingBean(GrpcServerFactory.class)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class ShadedNettyServerFactoryConfiguration {

		@Bean
		ShadedNettyGrpcServerFactory shadedNettyGrpcServerFactory(GrpcServerProperties properties,
				ObjectProvider<BindableService> grpcServicesProvider,
				ServerBuilderCustomizers serverBuilderCustomizers) {
			ShadedNettyServerFactoryPropertyMapper mapper = new ShadedNettyServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			ShadedNettyGrpcServerFactory factory = new ShadedNettyGrpcServerFactory(properties.getAddress(),
					properties.getPort(), builderCustomizers);
			grpcServicesProvider.orderedStream().map(BindableService::bindService).forEach(factory::addService);
			return factory;
		}

		@Bean
		ServerBuilderCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder> sslServerCustomizer(
				GrpcServerProperties properties, SslBundles bundles) {
			if (properties.getSsl().isEnabled()) {
				SslBundle bundle = bundles.getBundle(properties.getSsl().getBundle());
				return builder -> {
					try {
						builder.sslContext(io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
							.configure(io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder
								.forServer(bundle.getManagers().getKeyManagerFactory()))
							.build());
					}
					catch (SSLException e) {
						throw new IllegalStateException("Failed to create SSL context", e);
					}
				};
			}
			else {
				return builder -> {
				};
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(NettyServerBuilder.class)
	@ConditionalOnMissingBean(GrpcServerFactory.class)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class NettyServerFactoryConfiguration {

		@Bean
		NettyGrpcServerFactory nettyGrpcServerFactory(GrpcServerProperties properties,
				ObjectProvider<BindableService> grpcServicesProvider,
				ServerBuilderCustomizers serverBuilderCustomizers) {
			NettyServerFactoryPropertyMapper mapper = new NettyServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<NettyServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			NettyGrpcServerFactory factory = new NettyGrpcServerFactory(properties.getAddress(), properties.getPort(),
					builderCustomizers);
			grpcServicesProvider.orderedStream().map(BindableService::bindService).forEach(factory::addService);
			return factory;
		}

		@Bean
		ServerBuilderCustomizer<NettyServerBuilder> sslServerCustomizer(GrpcServerProperties properties,
				SslBundles bundles) {
			if (properties.getSsl().isEnabled()) {
				SslBundle bundle = bundles.getBundle(properties.getSsl().getBundle());
				return builder -> {
					try {
						builder.sslContext(GrpcSslContexts
							.configure(SslContextBuilder.forServer(bundle.getManagers().getKeyManagerFactory()))
							.build());
					}
					catch (SSLException e) {
						throw new IllegalStateException("Failed to create SSL context", e);
					}
				};
			}
			else {
				return builder -> {
				};
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ServerBuilder.class)
	@ConditionalOnMissingBean(GrpcServerFactory.class)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class ServiceProviderServerFactoryConfiguration {

		@Bean
		<T extends ServerBuilder<T>> DefaultGrpcServerFactory<T> serviceProviderGrpcServerFactory(
				GrpcServerProperties properties, ObjectProvider<BindableService> grpcServicesProvider,
				ServerBuilderCustomizers serverBuilderCustomizers) {
			DefaultServerFactoryPropertyMapper<T> mapper = new DefaultServerFactoryPropertyMapper<>(properties);
			List<ServerBuilderCustomizer<T>> builderCustomizers = List.of(mapper::customizeServerBuilder,
					serverBuilderCustomizers::customize);
			DefaultGrpcServerFactory<T> factory = new DefaultGrpcServerFactory<>(properties.getAddress(),
					properties.getPort(), builderCustomizers);
			grpcServicesProvider.orderedStream().map(BindableService::bindService).forEach(factory::addService);
			return factory;
		}

	}

}
