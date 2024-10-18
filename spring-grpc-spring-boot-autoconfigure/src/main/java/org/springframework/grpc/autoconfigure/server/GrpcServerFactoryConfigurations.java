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

import javax.net.ssl.KeyManagerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.ShadedNettyGrpcServerFactory;

import io.grpc.BindableService;
import io.grpc.netty.NettyServerBuilder;

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
				ObjectProvider<BindableService> grpcServicesProvider, ServerBuilderCustomizers serverBuilderCustomizers,
				SslBundles bundles) {
			ShadedNettyServerFactoryPropertyMapper mapper = new ShadedNettyServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			KeyManagerFactory keyManager = null;
			if (properties.getSsl().isEnabled()) {
				SslBundle bundle = bundles.getBundle(properties.getSsl().getBundle());
				keyManager = bundle.getManagers().getKeyManagerFactory();
			}
			ShadedNettyGrpcServerFactory factory = new ShadedNettyGrpcServerFactory(properties.getAddress(), keyManager,
					builderCustomizers);
			grpcServicesProvider.orderedStream().map(BindableService::bindService).forEach(factory::addService);
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(NettyServerBuilder.class)
	@ConditionalOnMissingBean(GrpcServerFactory.class)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class NettyServerFactoryConfiguration {

		@Bean
		NettyGrpcServerFactory nettyGrpcServerFactory(GrpcServerProperties properties,
				ObjectProvider<BindableService> grpcServicesProvider, ServerBuilderCustomizers serverBuilderCustomizers,
				SslBundles bundles) {
			NettyServerFactoryPropertyMapper mapper = new NettyServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<NettyServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			KeyManagerFactory keyManager = null;
			if (properties.getSsl().isEnabled()) {
				SslBundle bundle = bundles.getBundle(properties.getSsl().getBundle());
				keyManager = bundle.getManagers().getKeyManagerFactory();
			}
			NettyGrpcServerFactory factory = new NettyGrpcServerFactory(properties.getAddress(), keyManager,
					builderCustomizers);
			grpcServicesProvider.orderedStream().map(BindableService::bindService).forEach(factory::addService);
			return factory;
		}

	}

}
