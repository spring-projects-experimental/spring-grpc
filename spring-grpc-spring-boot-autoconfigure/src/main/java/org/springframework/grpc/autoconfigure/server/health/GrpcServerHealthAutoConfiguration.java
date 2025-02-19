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

package org.springframework.grpc.autoconfigure.server.health;

import java.util.List;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.grpc.autoconfigure.server.ConditionalOnGrpcServerEnabled;
import org.springframework.grpc.autoconfigure.server.GrpcServerFactoryAutoConfiguration;
import org.springframework.grpc.autoconfigure.server.GrpcServerProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.grpc.BindableService;
import io.grpc.protobuf.services.HealthStatusManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side health service.
 *
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 * @author Chris Bono
 */
@AutoConfiguration(before = GrpcServerFactoryAutoConfiguration.class)
@ConditionalOnGrpcServerEnabled
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
	@ConditionalOnClass(HealthEndpoint.class)
	@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class)
	@AutoConfigureAfter(value = TaskSchedulingAutoConfiguration.class,
			name = "org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration")
	@ConditionalOnProperty(name = "spring.grpc.server.health.actuator.enabled", havingValue = "true",
			matchIfMissing = true)
	@Conditional(OnHealthIndicatorPathsCondition.class)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	@EnableScheduling
	static class ActuatorHealthAdapterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ActuatorHealthAdapter healthAdapter(HealthStatusManager healthStatusManager, HealthEndpoint healthEndpoint,
				StatusAggregator statusAggregator, GrpcServerProperties serverProperties) {
			return new ActuatorHealthAdapter(healthStatusManager, healthEndpoint, statusAggregator,
					serverProperties.getHealth().getActuator().getUpdateOverallHealth(),
					serverProperties.getHealth().getActuator().getHealthIndicatorPaths());
		}

		@Bean
		ActuatorHealthAdapterInvoker healthAdapterInvoker(ActuatorHealthAdapter healthAdapter,
				SimpleAsyncTaskSchedulerBuilder schedulerBuilder, GrpcServerProperties serverProperties) {
			return new ActuatorHealthAdapterInvoker(healthAdapter, schedulerBuilder,
					serverProperties.getHealth().getActuator().getUpdateInitialDelay(),
					serverProperties.getHealth().getActuator().getUpdateRate());
		}

	}

	/**
	 * Condition to determine if
	 * {@code spring.grpc.server.health.actuator.health-indicator-paths} is specified with
	 * at least one entry.
	 */
	static class OnHealthIndicatorPathsCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String propertyName = "spring.grpc.server.health.actuator.health-indicator-paths";
			BindResult<List<String>> property = Binder.get(context.getEnvironment())
				.bind(propertyName, Bindable.listOf(String.class));
			ConditionMessage.Builder messageBuilder = ConditionMessage
				.forCondition("Health indicator paths (at least one)");
			if (property.isBound() && !property.get().isEmpty()) {
				return ConditionOutcome
					.match(messageBuilder.because("property %s found with at least one entry".formatted(propertyName)));
			}
			return ConditionOutcome.noMatch(
					messageBuilder.because("property %s not found with at least one entry".formatted(propertyName)));
		}

	}

}
