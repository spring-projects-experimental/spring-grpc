/*
 * Copyright 2023-2024 the original author or authors.
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
 *
 * Partial copy from net.devh:grpc-spring-boot-starter.
 */

package org.springframework.grpc.autoconfigure.server;

import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.BindableService;
import io.grpc.protobuf.services.HealthStatusManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side health service.
 *
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 * @author Chris Bono
 */
@AutoConfiguration(before = GrpcServerFactoryAutoConfiguration.class)
@ConditionalOnClass(HealthStatusManager.class)
@ConditionalOnProperty(name = "spring.grpc.server.health.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcServerHealthAutoConfiguration {

	@Bean(destroyMethod = "enterTerminalState")
	@ConditionalOnMissingBean
	HealthStatusManager healthStatusManager() {
		return new HealthStatusManager();
	}

	@Bean
	BindableService grpcHealthService(HealthStatusManager healthStatusManager) {
		return healthStatusManager.getHealthService();
	}

	@Configuration(proxyBeanMethods = false)
	@AutoConfigureAfter(name = "org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration")
	@ConditionalOnClass(HealthEndpoint.class)
	@ConditionalOnBean(HealthEndpoint.class)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class ActuatorHealthAdapterConfiguration {

		@Bean
		ActuatorHealthAdapter healthAdapter(HealthStatusManager healthStatusManager, HealthEndpoint healthEndpoint,
				GrpcServerProperties serverProperties) {
			return new ActuatorHealthAdapter();
		}

	}

	/**
	 * Adapts {@link HealthContributor Actuator health checks} into gRPC health checks by
	 * periodically invoking {@link HealthEndpoint health endpoints} and updating the
	 * health status in gRPC {@link HealthStatusManager}.
	 */
	static class ActuatorHealthAdapter {

	}

}
