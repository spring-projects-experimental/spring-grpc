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
import javax.net.ssl.TrustManagerFactory;

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
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

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
				GrpcServiceDiscoverer grpcServicesDiscoverer, ServerBuilderCustomizers serverBuilderCustomizers,
				SslBundles bundles) {
			ShadedNettyServerFactoryPropertyMapper mapper = new ShadedNettyServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			KeyManagerFactory keyManager = null;
			TrustManagerFactory trustManager = null;
			if (properties.getSsl().isEnabled()) {
				SslBundle bundle = bundles.getBundle(properties.getSsl().getBundle());
				keyManager = bundle.getManagers().getKeyManagerFactory();
				trustManager = properties.getSsl().isSecure() ? bundle.getManagers().getTrustManagerFactory()
						: io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE;
				trustManager = bundle.getManagers().getTrustManagerFactory();
			}
			ShadedNettyGrpcServerFactory factory = new ShadedNettyGrpcServerFactory(properties.getAddress(),
					builderCustomizers, keyManager, trustManager, properties.getSsl().getClientAuth());
			grpcServicesDiscoverer.findServices().forEach(factory::addService);
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
				GrpcServiceDiscoverer grpcServicesDiscoverer, ServerBuilderCustomizers serverBuilderCustomizers,
				SslBundles bundles) {
			NettyServerFactoryPropertyMapper mapper = new NettyServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<NettyServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			KeyManagerFactory keyManager = null;
			TrustManagerFactory trustManager = null;
			if (properties.getSsl().isEnabled()) {
				SslBundle bundle = bundles.getBundle(properties.getSsl().getBundle());
				keyManager = bundle.getManagers().getKeyManagerFactory();
				trustManager = properties.getSsl().isSecure() ? bundle.getManagers().getTrustManagerFactory()
						: InsecureTrustManagerFactory.INSTANCE;
			}
			NettyGrpcServerFactory factory = new NettyGrpcServerFactory(properties.getAddress(), builderCustomizers,
					keyManager, trustManager, properties.getSsl().getClientAuth());
			grpcServicesDiscoverer.findServices().forEach(factory::addService);
			return factory;
		}

	}

}
