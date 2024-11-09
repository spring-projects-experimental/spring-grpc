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

import java.util.List;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

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
	void globalServerInterceptorsAreFoundInProperOrder() {
		this.contextRunner()
			.withUserConfiguration(GlobalServerInterceptorsConfig.class)
			.run((context) -> assertThat(context).getBean(DefaultGrpcServiceDiscoverer.class)
				.extracting(DefaultGrpcServiceDiscoverer::findGlobalInterceptors, InstanceOfAssertFactories.LIST)
				.containsExactly(GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
						GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO));
	}

	@Test
	void servicesAreFoundInProperOrderWithGlobalInterceptorsApplied() {
		// It gets difficult to verify interceptors are added properly to mocked services.
		// To make it easier, we just static mock ServerInterceptors.interceptForward to
		// echo back the service def. This way we can verify the interceptors were passed
		// in the proper order as we rely/trust that ServerInterceptors.interceptForward
		// is
		// tested well in grpc-java.
		try (MockedStatic<ServerInterceptors> serverInterceptorsMocked = Mockito.mockStatic(ServerInterceptors.class)) {
			serverInterceptorsMocked
				.when(() -> ServerInterceptors.interceptForward(any(ServerServiceDefinition.class), anyList()))
				.thenAnswer((Answer<ServerServiceDefinition>) invocation -> invocation.getArgument(0));
			this.contextRunner().withUserConfiguration(GlobalServerInterceptorsConfig.class).run((context) -> {
				assertThat(context).getBean(DefaultGrpcServiceDiscoverer.class)
					.extracting(DefaultGrpcServiceDiscoverer::findServices, InstanceOfAssertFactories.LIST)
					.containsExactly(GlobalServerInterceptorsConfig.SERVICE_DEF_B,
							GlobalServerInterceptorsConfig.SERVICE_DEF_A);
				ArgumentCaptor<ServerServiceDefinition> serviceDefArg = ArgumentCaptor.captor();
				ArgumentCaptor<List<ServerInterceptor>> interceptorsArg = ArgumentCaptor.captor();
				serverInterceptorsMocked.verify(
						() -> ServerInterceptors.interceptForward(serviceDefArg.capture(), interceptorsArg.capture()),
						times(2));
				assertThat(serviceDefArg.getAllValues()).containsExactly(GlobalServerInterceptorsConfig.SERVICE_DEF_B,
						GlobalServerInterceptorsConfig.SERVICE_DEF_A);
				assertThat(interceptorsArg.getAllValues()).containsExactly(
						List.of(GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
								GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO),
						List.of(GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
								GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO));
			});
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class GlobalServerInterceptorsConfig {

		static BindableService SERVICE_A = mock();

		static ServerServiceDefinition SERVICE_DEF_A = mock();

		static BindableService SERVICE_B = mock();

		static ServerServiceDefinition SERVICE_DEF_B = mock();

		static ServerInterceptor GLOBAL_INTERCEPTOR_FOO = mock();

		static ServerInterceptor GLOBAL_INTERCEPTOR_IGNORED = mock();

		static ServerInterceptor GLOBAL_INTERCEPTOR_BAR = mock();

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

		@Bean
		@Order(200)
		@GlobalServerInterceptor
		ServerInterceptor globalInterceptorFoo() {
			return GLOBAL_INTERCEPTOR_FOO;
		}

		@Bean
		@Order(150)
		ServerInterceptor globalInterceptorIgnored() {
			return GLOBAL_INTERCEPTOR_IGNORED;
		}

		@Bean
		@Order(100)
		@GlobalServerInterceptor
		ServerInterceptor globalInterceptorBar() {
			return GLOBAL_INTERCEPTOR_BAR;
		}

	}

}
