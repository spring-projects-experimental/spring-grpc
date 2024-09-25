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

package org.springframework.grpc.server;

import java.util.List;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.autoconfigure.server.GrpcServerAutoConfiguration;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GrpcServerAutoConfiguration}.
 *
 * @author Chris Bono
 */
class GrpcServerAutoConfigurationTests {

	private ApplicationContextRunner validContextRunner() {
		BindableService service = mock();
		ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();
		when(service.bindService()).thenReturn(serviceDefinition);
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class))
			.withBean(BindableService.class, () -> service);
	}

	@Test
	void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.validContextRunner()
			.withClassLoader(new FilteredClassLoader(BindableService.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenNoBindableServicesRegisteredAutoConfigurationIsSkipped() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenHasUserDefinedServerFactoryDoesNotAutoConfigureBean() {
		// NOTE: we use noop server lifecycle to avoid startup
		GrpcServerFactory customServerFactory = mock(GrpcServerFactory.class);
		this.validContextRunner()
			.withBean("customServerFactory", GrpcServerFactory.class, () -> customServerFactory)
			.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class).isSameAs(customServerFactory));
	}

	@Test
	void whenHasUserDefinedServerLifecycleDoesNotAutoConfigureBean() {
		GrpcServerLifecycle customServerLifecycle = mock(GrpcServerLifecycle.class);
		this.validContextRunner()
			.withBean("customServerLifecycle", GrpcServerLifecycle.class, () -> customServerLifecycle)
			.run((context) -> assertThat(context).getBean(GrpcServerLifecycle.class).isSameAs(customServerLifecycle));
	}

	@Test
	void serverFactoryAutoConfiguredAsExpected() {
		// NOTE: we use noop server lifecycle to avoid startup
		this.validContextRunner()
			.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withPropertyValues("spring.grpc.server.address=myhost", "spring.grpc.server.port=6160")
			.run((context) -> assertThat(context).getBean(DefaultGrpcServerFactory.class)
				.hasFieldOrPropertyWithValue("address", "myhost")
				.hasFieldOrPropertyWithValue("port", 6160)
				.hasFieldOrPropertyWithValue("serverBuilderCustomizers", List.of())
				.extracting("serviceList", InstanceOfAssertFactories.list(ServerServiceDefinition.class))
				.singleElement()
				.extracting(ServerServiceDefinition::getServiceDescriptor)
				.extracting(ServiceDescriptor::getName)
				.isEqualTo("my-service"));
	}

	@Test
	void serverFactoryAutoConfiguredWithCustomizers() {
		this.validContextRunner()
			.withUserConfiguration(ServerFactoryCustomizersConfig.class)
			.run((context) -> assertThat(context).getBean(DefaultGrpcServerFactory.class)
				.extracting("serverBuilderCustomizers", InstanceOfAssertFactories.list(ServerBuilderCustomizer.class))
				.containsExactly(ServerFactoryCustomizersConfig.CUSTOMIZER_BAR,
						ServerFactoryCustomizersConfig.CUSTOMIZER_FOO));
	}

	@Test
	void serverLifecycleAutoConfiguredAsExpected() {
		this.validContextRunner()
			.run((context) -> assertThat(context).getBean(GrpcServerLifecycle.class)
				.hasFieldOrPropertyWithValue("factory", context.getBean(DefaultGrpcServerFactory.class)));
	}

	@Configuration(proxyBeanMethods = false)
	static class ServerFactoryCustomizersConfig {

		static ServerBuilderCustomizer CUSTOMIZER_FOO = (serverBuilder) -> {
		};

		static ServerBuilderCustomizer CUSTOMIZER_BAR = (serverBuilder) -> {
		};

		@Bean
		@Order(200)
		ServerBuilderCustomizer customizerFoo() {
			return CUSTOMIZER_FOO;
		}

		@Bean
		@Order(100)
		ServerBuilderCustomizer customizerBar() {
			return CUSTOMIZER_BAR;
		}

	}

}
