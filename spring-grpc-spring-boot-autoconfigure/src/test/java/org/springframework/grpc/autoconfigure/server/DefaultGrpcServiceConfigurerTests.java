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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.lang.Nullable;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

/**
 * Tests for {@link DefaultGrpcServiceConfigurer}.
 *
 * @author Chris Bono
 */
class DefaultGrpcServiceConfigurerTests {

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
			.run((context) -> assertThat(context).getBean(DefaultGrpcServiceConfigurer.class)
				.extracting("globalInterceptors", InstanceOfAssertFactories.LIST)
				.containsExactly(GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
						GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO));
	}

	@Nested
	class WithNoServiceInfoSpecified {

		@Test
		void whenNoGlobalInterceptorsRegisteredThenServiceGetsNoInterceptors() {
			customizeContextAndRunServiceConfigurerWithServiceInfo(Function.identity(), null, List.of());
		}

		@Test
		void whenGlobalInterceptorsRegisteredThenServiceGetsGlobalInterceptors() {
			customizeContextAndRunServiceConfigurerWithServiceInfo(
					(contextRunner) -> contextRunner.withUserConfiguration(GlobalServerInterceptorsConfig.class), null,
					List.of(GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
							GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO));
		}

	}

	@Nested
	class WithServiceInfoWithSingleInterceptor {

		@Test
		void whenSingleBeanOfInterceptorTypeRegisteredThenItIsUsed() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptors(List.of(TestServerInterceptorA.class));
			List<ServerInterceptor> expectedInterceptors = List.of(ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo(
					(contextRunner) -> contextRunner.withUserConfiguration(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

		@Test
		void whenMultipleBeansOfInterceptorTypeRegisteredThenThrowsException() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptors(List.of(ServerInterceptor.class));
			customizeContextAndRunServiceConfigurerWithServiceInfo(
					(contextRunner) -> contextRunner.withUserConfiguration(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, NoUniqueBeanDefinitionException.class);
		}

		@Test
		void whenNoBeanOfInterceptorTypeRegisteredThenThrowsException() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptors(List.of(ServerInterceptor.class));
			customizeContextAndRunServiceConfigurerWithServiceInfo(Function.identity(), serviceInfo,
					NoSuchBeanDefinitionException.class);
		}

	}

	@Nested
	class WithServiceInfoWithMultipleInterceptors {

		@Test
		void whenSingleBeanOfEachInterceptorTypeRegisteredThenTheyAreUsed() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo
				.withInterceptors(List.of(TestServerInterceptorB.class, TestServerInterceptorA.class));
			List<ServerInterceptor> expectedInterceptors = List.of(ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo(
					(contextRunner) -> contextRunner.withUserConfiguration(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

	}

	@Nested
	class WithServiceInfoWithSingleInterceptorName {

		@Test
		void whenSingleBeanWithInterceptorNameRegisteredThenItIsUsed() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptorNames(List.of("interceptorB"));
			List<ServerInterceptor> expectedInterceptors = List.of(ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B);
			customizeContextAndRunServiceConfigurerWithServiceInfo(
					(contextRunner) -> contextRunner.withUserConfiguration(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

		@Test
		void whenNoBeanWithInterceptorNameRegisteredThenThrowsException() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptorNames(List.of("interceptor1"));
			customizeContextAndRunServiceConfigurerWithServiceInfo(Function.identity(), serviceInfo,
					NoSuchBeanDefinitionException.class);
		}

	}

	@Nested
	class WithServiceInfoWithMultipleInterceptorNames {

		@Test
		void whenSingleBeanWithEachInterceptorNameRegisteredThenTheyAreUsed() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptorNames(List.of("interceptorB", "interceptorA"));
			List<ServerInterceptor> expectedInterceptors = List.of(ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo(
					(contextRunner) -> contextRunner.withUserConfiguration(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

	}

	@Nested
	class WithServiceInfoWithInterceptorAndInterceptorName {

		@SuppressWarnings("unchecked")
		@Test
		void whenSingleBeanOfEachAvailableThenTheyAreBothUsed() {
			GrpcServiceInfo serviceInfo = new GrpcServiceInfo(new Class[] { TestServerInterceptorB.class },
					new String[] { "interceptorA" }, false);
			List<ServerInterceptor> expectedInterceptors = List.of(ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo(
					(contextRunner) -> contextRunner.withUserConfiguration(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

	}

	@Nested
	class WithServiceInfoCombinedWithGlobalInterceptors {

		@Test
		void whenBlendInterceptorsFalseThenGlobalInterceptorsAddedFirst() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo
				.withInterceptors(List.of(TestServerInterceptorB.class, TestServerInterceptorA.class));
			List<ServerInterceptor> expectedInterceptors = List.of(
					GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
					GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo((contextRunner) -> contextRunner
				.withUserConfiguration(GlobalServerInterceptorsConfig.class, ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

		@SuppressWarnings("unchecked")
		@Disabled("Needs 'blend interceptors' to be implemented")
		@Test
		void whenBlendInterceptorsTrueThenGlobalInterceptorsBlended() {
			GrpcServiceInfo serviceInfo = new GrpcServiceInfo(
					new Class[] { TestServerInterceptorB.class, TestServerInterceptorA.class }, new String[0], true);
			List<ServerInterceptor> expectedInterceptors = List.of(
					GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo((contextRunner) -> contextRunner
				.withUserConfiguration(GlobalServerInterceptorsConfig.class, ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

	}

	private void customizeContextAndRunServiceConfigurerWithServiceInfo(
			Function<ApplicationContextRunner, ApplicationContextRunner> contextCustomizer, GrpcServiceInfo serviceInfo,
			List<ServerInterceptor> expectedInterceptors) {
		this.customizeContextAndRunServiceConfigurerWithServiceInfo(contextCustomizer, serviceInfo,
				expectedInterceptors, null);
	}

	private void customizeContextAndRunServiceConfigurerWithServiceInfo(
			Function<ApplicationContextRunner, ApplicationContextRunner> contextCustomizer, GrpcServiceInfo serviceInfo,
			Class<? extends Throwable> expectedExceptionType) {
		this.customizeContextAndRunServiceConfigurerWithServiceInfo(contextCustomizer, serviceInfo, null,
				expectedExceptionType);
	}

	private void customizeContextAndRunServiceConfigurerWithServiceInfo(
			Function<ApplicationContextRunner, ApplicationContextRunner> contextCustomizer, GrpcServiceInfo serviceInfo,
			@Nullable List<ServerInterceptor> expectedInterceptors,
			@Nullable Class<? extends Throwable> expectedExceptionType) {
		// It gets difficult to verify interceptors are added properly to mocked services.
		// To make it easier, we just static mock ServerInterceptors.interceptForward to
		// echo back the service def. This way we can verify the interceptors were passed
		// in the proper order as we rely on ServerInterceptors.interceptForward being
		// well tested in grpc-java.
		try (MockedStatic<ServerInterceptors> serverInterceptorsMocked = Mockito.mockStatic(ServerInterceptors.class)) {
			serverInterceptorsMocked
				.when(() -> ServerInterceptors.interceptForward(any(ServerServiceDefinition.class), anyList()))
				.thenAnswer((Answer<ServerServiceDefinition>) invocation -> invocation.getArgument(0));
			BindableService service = mock();
			ServerServiceDefinition serviceDef = mock();
			when(service.bindService()).thenReturn(serviceDef);
			this.contextRunner()
				.withBean("service", BindableService.class, () -> service)
				.with(contextCustomizer)
				.run((context) -> {
					DefaultGrpcServiceConfigurer configurer = context.getBean(DefaultGrpcServiceConfigurer.class);
					if (expectedExceptionType != null) {
						assertThatThrownBy(() -> configurer.configure(service, serviceInfo))
							.isInstanceOf(expectedExceptionType);
						serverInterceptorsMocked.verifyNoInteractions();
					}
					else {
						configurer.configure(service, serviceInfo);
						serverInterceptorsMocked
							.verify(() -> ServerInterceptors.interceptForward(serviceDef, expectedInterceptors));
					}
				});
		}
	}

	interface TestServerInterceptorA extends ServerInterceptor {

	}

	interface TestServerInterceptorB extends ServerInterceptor {

	}

	@Configuration(proxyBeanMethods = false)
	static class GlobalServerInterceptorsConfig {

		static BindableService SERVICE_A = mock();

		static ServerServiceDefinition SERVICE_DEF_A = mock();

		static ServerInterceptor GLOBAL_INTERCEPTOR_FOO = mock();

		static ServerInterceptor GLOBAL_INTERCEPTOR_IGNORED = mock();

		static ServerInterceptor GLOBAL_INTERCEPTOR_BAR = mock();

		@Bean
		BindableService serviceA() {
			when(SERVICE_A.bindService()).thenReturn(SERVICE_DEF_A);
			return SERVICE_A;
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

	@Configuration(proxyBeanMethods = false)
	static class ServiceSpecificInterceptorsConfig {

		static TestServerInterceptorB SVC_INTERCEPTOR_B = mock();

		static TestServerInterceptorA SVC_INTERCEPTOR_A = mock();

		@Bean
		@Order(150)
		TestServerInterceptorB interceptorB() {
			return SVC_INTERCEPTOR_B;
		}

		@Bean
		@Order(225)
		TestServerInterceptorA interceptorA() {
			return SVC_INTERCEPTOR_A;
		}

	}

}
