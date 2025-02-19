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
 */

package org.springframework.grpc.autoconfigure.server.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.grpc.autoconfigure.server.GrpcServerFactoryAutoConfiguration;
import org.springframework.grpc.autoconfigure.server.ServerBuilderCustomizers;
import org.springframework.grpc.autoconfigure.server.health.GrpcServerHealthAutoConfiguration.ActuatorHealthAdapterConfiguration;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.util.StringUtils;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.protobuf.services.HealthStatusManager;

/**
 * Tests for {@link GrpcServerHealthAutoConfiguration}.
 *
 * @author Chris Bono
 */
class GrpcServerHealthAutoConfigurationTests {

	private ApplicationContextRunner contextRunner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerHealthAutoConfiguration.class));
	}

	@Test
	void whenHealthStatusManagerNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(HealthStatusManager.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerHealthAutoConfiguration.class));
	}

	@Test
	void whenHealthPropertyNotSetHealthIsAutoConfigured() {
		this.contextRunner()
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerHealthAutoConfiguration.class));
	}

	@Test
	void whenHealthPropertyIsTrueHealthIsAutoConfigured() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.health.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerHealthAutoConfiguration.class));
	}

	@Test
	void whenHealthPropertyIsFalseAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.health.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerHealthAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerHealthAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerHealthAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerHealthAutoConfiguration.class));
	}

	@Test
	void healthIsAutoConfiguredBeforeGrpcServerFactory() {
		BindableService service = mock();
		ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();
		when(service.bindService()).thenReturn(serviceDefinition);
		this.contextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerFactoryAutoConfiguration.class))
			.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean("serverBuilderCustomizers", ServerBuilderCustomizers.class, Mockito::mock)
			.withBean("grpcServicesDiscoverer", GrpcServiceDiscoverer.class, Mockito::mock)
			.withBean("sslBundles", SslBundles.class, Mockito::mock)
			.withBean(BindableService.class, () -> service)
			.run((context) -> assertThatBeanDefinitionsContainInOrder(context, GrpcServerHealthAutoConfiguration.class,
					GrpcServerFactoryAutoConfiguration.class));
	}

	@Disabled("Will be tested in an integration test once the Actuator adapter is implemented")
	@Test
	void enterTerminalStateIsCalledWhenStatusManagerIsStopped() {
	}

	@Test
	void whenHasUserDefinedHealthStatusManagerDoesNotAutoConfigureBean() {
		HealthStatusManager customHealthStatusManager = mock();
		this.contextRunner()
			.withBean("customHealthStatusManager", HealthStatusManager.class, () -> customHealthStatusManager)
			.withPropertyValues("spring.grpc.server.health.enabled=false")
			.run((context) -> assertThat(context).getBean(HealthStatusManager.class)
				.isSameAs(customHealthStatusManager));
	}

	@Test
	void healthStatusManagerAutoConfiguredAsExpected() {
		this.contextRunner().run((context) -> {
			assertThat(context).hasSingleBean(GrpcServerHealthAutoConfiguration.class);
			assertThat(context).hasSingleBean(HealthStatusManager.class);
			assertThat(context).getBean("grpcHealthService", BindableService.class).isNotNull();
		});
	}

	private void assertThatBeanDefinitionsContainInOrder(ConfigurableApplicationContext context,
			Class<?>... configClasses) {
		var configBeanDefNames = Arrays.stream(configClasses).map(this::beanDefinitionNameForConfigClass).toList();
		var filteredBeanDefNames = Arrays.stream(context.getBeanDefinitionNames())
			.filter(configBeanDefNames::contains)
			.toList();
		assertThat(filteredBeanDefNames).containsExactlyElementsOf(configBeanDefNames);
	}

	private String beanDefinitionNameForConfigClass(Class<?> configClass) {
		var fullName = configClass.getName();
		return StringUtils.uncapitalize(fullName);
	}

	@Nested
	class ActuatorHealthAdapterConfigurationTests {

		private ApplicationContextRunner validContextRunner() {
			return GrpcServerHealthAutoConfigurationTests.this.contextRunner()
				.withPropertyValues("spring.grpc.server.health.actuator.health-indicator-paths=my-indicator")
				.withConfiguration(AutoConfigurations.of(HealthEndpointAutoConfiguration.class,
						TaskSchedulingAutoConfiguration.class));
		}

		@Test
		void adapterIsAutoConfiguredAfterHealthAutoConfiguration() {
			this.validContextRunner()
				.run((context) -> assertThatBeanDefinitionsContainInOrder(context,
						HealthEndpointAutoConfiguration.class, ActuatorHealthAdapterConfiguration.class));
		}

		@Test
		void adapterIsAutoConfiguredAfterTaskSchedulingAutoConfiguration() {
			this.validContextRunner()
				.run((context) -> assertThatBeanDefinitionsContainInOrder(context,
						TaskSchedulingAutoConfiguration.class, ActuatorHealthAdapterConfiguration.class));
		}

		@Test
		void whenHealthEndpointNotOnClasspathAutoConfigurationIsSkipped() {
			GrpcServerHealthAutoConfigurationTests.this.contextRunner()
				.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
				.withClassLoader(new FilteredClassLoader(HealthEndpoint.class))
				.run((context) -> assertThat(context)
					.doesNotHaveBean(GrpcServerHealthAutoConfiguration.ActuatorHealthAdapterConfiguration.class));
		}

		@Test
		void whenHealthEndpointNotAvailableAutoConfigurationIsSkipped() {
			this.validContextRunner()
				.withPropertyValues("management.endpoint.health.enabled=false")
				.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
				.run((context) -> assertThat(context)
					.doesNotHaveBean(GrpcServerHealthAutoConfiguration.ActuatorHealthAdapterConfiguration.class));
		}

		@Test
		void whenActuatorPropertyNotSetAdapterIsAutoConfigured() {
			this.validContextRunner()
				.run((context) -> assertThat(context)
					.hasSingleBean(GrpcServerHealthAutoConfiguration.ActuatorHealthAdapterConfiguration.class));
		}

		@Test
		void whenActuatorPropertyIsTrueAdapterIsAutoConfigured() {
			this.validContextRunner()
				.withPropertyValues("spring.grpc.server.health.actuator.enabled=true")
				.run((context) -> assertThat(context)
					.hasSingleBean(GrpcServerHealthAutoConfiguration.ActuatorHealthAdapterConfiguration.class));
		}

		@Test
		void whenActuatorPropertyIsFalseAdapterIsNotAutoConfigured() {
			this.validContextRunner()
				.withPropertyValues("spring.grpc.server.health.actuator.enabled=false")
				.run((context) -> assertThat(context)
					.doesNotHaveBean(GrpcServerHealthAutoConfiguration.ActuatorHealthAdapterConfiguration.class));
		}

		@Test
		void whenHealthIndicatorPathsIsNotSpecifiedAdapterIsNotAutoConfigured() {
			GrpcServerHealthAutoConfigurationTests.this.contextRunner()
				.withConfiguration(AutoConfigurations.of(HealthEndpointAutoConfiguration.class,
						TaskSchedulingAutoConfiguration.class))
				.run((context) -> assertThat(context)
					.doesNotHaveBean(GrpcServerHealthAutoConfiguration.ActuatorHealthAdapterConfiguration.class));
		}

		@Test
		void whenHealthIndicatorPathsIsSpecifiedEmptyAdapterIsNotAutoConfigured() {
			GrpcServerHealthAutoConfigurationTests.this.contextRunner()
				.withPropertyValues("spring.grpc.server.health.actuator.health-indicator-paths=")
				.withConfiguration(AutoConfigurations.of(HealthEndpointAutoConfiguration.class,
						TaskSchedulingAutoConfiguration.class))
				.run((context) -> assertThat(context)
					.doesNotHaveBean(GrpcServerHealthAutoConfiguration.ActuatorHealthAdapterConfiguration.class));
		}

		@Test
		void whenHasUserDefinedAdapterDoesNotAutoConfigureBean() {
			ActuatorHealthAdapter customAdapter = mock();
			this.validContextRunner()
				.withBean("customAdapter", ActuatorHealthAdapter.class, () -> customAdapter)
				.run((context) -> assertThat(context).getBean(ActuatorHealthAdapter.class).isSameAs(customAdapter));
		}

		@Test
		void adapterAutoConfiguredAsExpected() {
			this.validContextRunner()
				.run((context) -> assertThat(context).hasSingleBean(ActuatorHealthAdapter.class)
					.hasSingleBean(ActuatorHealthAdapterInvoker.class));
		}

	}

}
