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

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.DefaultGrpcServerFactory;
import org.springframework.grpc.server.GrpcServerConfigurer;
import org.springframework.grpc.server.GrpcServerFactory;

import io.grpc.ServerServiceDefinition;

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(GrpcServerFactory.class)
@AutoConfigureBefore(GrpcServiceAutoConfiguration.class)
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcServerFactoryAutoConfiguration {

	@ConditionalOnMissingBean(GrpcServerFactory.class)
	@Bean
	public DefaultGrpcServerFactory<?> defaultGrpcServerFactory(final GrpcServerProperties properties,
			final GrpcServiceDiscoverer serviceDiscoverer, final List<GrpcServerConfigurer> serverConfigurers) {
		final DefaultGrpcServerFactory<?> factory = new DefaultGrpcServerFactory<>(properties.getAddress(),
				properties.getPort(), serverConfigurers);
		for (final ServerServiceDefinition service : serviceDiscoverer.findGrpcServices()) {
			factory.addService(service);
		}
		return factory;
	}

}
