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

package org.springframework.grpc.autoconfigure.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.grpc.autoconfigure.server.DefaultGrpcServiceDiscovererTests.DefaultGrpcServiceDiscovererTestsConfig.SERVICE_A;
import static org.springframework.grpc.autoconfigure.server.DefaultGrpcServiceDiscovererTests.DefaultGrpcServiceDiscovererTestsConfig.SERVICE_B;

import java.util.LinkedHashMap;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

/**
 * Tests for {@link DefaultGrpcServiceDiscoverer}.
 *
 * @author Chris Bono
 */
class DefaultGrpcServiceDiscovererTests {

	private ApplicationContextRunner contextRunner() {
		// NOTE: we use noop server lifecycle to avoid startup
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class))
			.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock);
	}

	@Test
	void servicesAreFoundInProperOrderWithExpectedGrpcServiceAnnotations() {
		TestServiceConfigurer configurer = new TestServiceConfigurer();
		this.contextRunner()
			.withUserConfiguration(DefaultGrpcServiceDiscovererTestsConfig.class)
			.withBean("customServiceConfigurer", GrpcServiceConfigurer.class, () -> configurer)
			.run((context) -> {
				assertThat(context).getBean(DefaultGrpcServiceDiscoverer.class)
					.extracting(DefaultGrpcServiceDiscoverer::findServices, InstanceOfAssertFactories.LIST)
					.containsExactly(DefaultGrpcServiceDiscovererTestsConfig.SERVICE_DEF_B,
							DefaultGrpcServiceDiscovererTestsConfig.SERVICE_DEF_A);
				assertThat(configurer.invocations).hasSize(2);
				assertThat(configurer.invocations.keySet()).containsExactly(SERVICE_B, SERVICE_A);
				assertThat(configurer.invocations).containsEntry(SERVICE_B, null);
				assertThat(configurer.invocations).hasEntrySatisfying(SERVICE_A, (serviceInfo) -> {
					assertThat(serviceInfo.interceptors()).isEmpty();
					assertThat(serviceInfo.interceptorNames()).isEmpty();
					assertThat(serviceInfo.blendWithGlobalInterceptors()).isFalse();
				});
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class DefaultGrpcServiceDiscovererTestsConfig {

		static BindableService SERVICE_A = mock();

		static ServerServiceDefinition SERVICE_DEF_A = mock();

		static BindableService SERVICE_B = mock();

		static ServerServiceDefinition SERVICE_DEF_B = mock();

		@GrpcService
		@Bean
		@Order(200)
		BindableService serviceA() {
			when(SERVICE_A.bindService()).thenReturn(SERVICE_DEF_A);
			return SERVICE_A;
		}

		@Bean
		@Order(100)
		BindableService serviceB() {
			when(SERVICE_B.bindService()).thenReturn(SERVICE_DEF_B);
			return SERVICE_B;
		}

	}

	static class TestServiceConfigurer implements GrpcServiceConfigurer {

		Map<BindableService, GrpcServiceInfo> invocations = new LinkedHashMap<>();

		@Override
		public ServerServiceDefinition configure(BindableService bindableService, GrpcServiceInfo serviceInfo) {
			invocations.put(bindableService, serviceInfo);
			return bindableService.bindService();
		}

	}

}
