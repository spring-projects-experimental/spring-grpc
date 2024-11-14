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
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.util.unit.DataSize;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side components.
 * <p>
 * gRPC must be on the classpath and at least one {@link BindableService} bean registered
 * in the context in order for the auto-configuration to execute.
 *
 * @author David Syer
 * @author Chris Bono
 * @author Toshiaki Maki
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(BindableService.class)
@ConditionalOnBean(BindableService.class)
public class GrpcServerFactoryAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnNotWebApplication
	@Import({ GrpcServerFactoryConfigurations.ShadedNettyServerFactoryConfiguration.class,
			GrpcServerFactoryConfigurations.NettyServerFactoryConfiguration.class })
	static class GrpcServerFactoryConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication
	static class GrpcServletConfiguration {

		@Bean
		public ServletRegistrationBean<GrpcServlet> grpcServlet(GrpcServerProperties properties,
				GrpcServiceDiscoverer discoverer, ServerBuilderCustomizers serverBuilderCustomizers) {
			List<ServerServiceDefinition> services = discoverer.findServices();
			List<String> paths = services.stream()
				.map(service -> "/" + service.getServiceDescriptor().getName() + "/*")
				.collect(Collectors.toList());
			ServletServerBuilder servletServerBuilder = new ServletServerBuilder();
			services.forEach(servletServerBuilder::addService);
			PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
			mapper.from(properties.getMaxInboundMessageSize())
				.asInt(DataSize::toBytes)
				.to(servletServerBuilder::maxInboundMessageSize);
			serverBuilderCustomizers.customize(servletServerBuilder);
			ServletRegistrationBean<GrpcServlet> servlet = new ServletRegistrationBean<>(
					servletServerBuilder.buildServlet());
			servlet.setUrlMappings(paths);
			return servlet;
		}

	}

}
