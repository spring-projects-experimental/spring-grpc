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

package org.springframework.grpc.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.grpc.server.service.DefaultGrpcServiceDiscovererTests.DefaultGrpcServiceDiscovererTestsConfig.SERVICE_A;
import static org.springframework.grpc.server.service.DefaultGrpcServiceDiscovererTests.DefaultGrpcServiceDiscovererTestsConfig.SERVICE_B;

import java.util.LinkedHashMap;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

/**
 * Tests for {@link DefaultGrpcServiceDiscoverer}.
 *
 * @author Chris Bono
 */
class DefaultGrpcServiceDiscovererTests {

	@Test
	void servicesAreFoundInProperOrderWithExpectedGrpcServiceAnnotations() {
		new ApplicationContextRunner().withUserConfiguration(DefaultGrpcServiceDiscovererTestsConfig.class)
			.run((context) -> {
				assertThat(context).getBean(DefaultGrpcServiceDiscoverer.class)
					.extracting(DefaultGrpcServiceDiscoverer::findServices, InstanceOfAssertFactories.LIST)
					.containsExactly(DefaultGrpcServiceDiscovererTestsConfig.SERVICE_DEF_B,
							DefaultGrpcServiceDiscovererTestsConfig.SERVICE_DEF_A);
				TestServiceConfigurer configurer = context.getBean(TestServiceConfigurer.class);
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

		static BindableService SERVICE_A = Mockito.mock();

		static ServerServiceDefinition SERVICE_DEF_A = Mockito.mock();

		static BindableService SERVICE_B = Mockito.mock();

		static ServerServiceDefinition SERVICE_DEF_B = Mockito.mock();

		@Bean
		TestServiceConfigurer testServiceConfigurer() {
			return new TestServiceConfigurer();
		}

		@Bean
		GrpcServiceDiscoverer grpcServiceDiscoverer(GrpcServiceConfigurer grpcServiceConfigurer,
				ApplicationContext applicationContext) {
			return new DefaultGrpcServiceDiscoverer(grpcServiceConfigurer, applicationContext);
		}

		@GrpcService
		@Bean
		@Order(200)
		BindableService serviceA() {
			Mockito.when(SERVICE_A.bindService()).thenReturn(SERVICE_DEF_A);
			return SERVICE_A;
		}

		@Bean
		@Order(100)
		BindableService serviceB() {
			Mockito.when(SERVICE_B.bindService()).thenReturn(SERVICE_DEF_B);
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
