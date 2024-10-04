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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.netty.NettyServerBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.DefaultGrpcServerFactory;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.ShadedNettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GrpcServerAutoConfiguration}.
 *
 * @author Chris Bono
 */
class GrpcServerAutoConfigurationTests {

	private ApplicationContextRunner contextRunner() {
		BindableService service = mock();
		ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();
		when(service.bindService()).thenReturn(serviceDefinition);
		// NOTE: we use noop server lifecycle to avoid startup
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class))
			.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean(BindableService.class, () -> service);
	}

	private ApplicationContextRunner contextRunnerWithLifecyle() {
		BindableService service = mock();
		ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();
		when(service.bindService()).thenReturn(serviceDefinition);
		// NOTE: we use noop server lifecycle to avoid startup
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class))
			.withBean(BindableService.class, () -> service);
	}

	@Test
	void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(BindableService.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenNoBindableServicesRegisteredAutoConfigurationIsSkipped() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class));
	}

	@Test
	void whenHasUserDefinedServerLifecycleDoesNotAutoConfigureBean() {
		GrpcServerLifecycle customServerLifecycle = mock(GrpcServerLifecycle.class);
		this.contextRunnerWithLifecyle()
			.withBean("customServerLifecycle", GrpcServerLifecycle.class, () -> customServerLifecycle)
			.run((context) -> assertThat(context).getBean(GrpcServerLifecycle.class).isSameAs(customServerLifecycle));
	}

	@Test
	void serverLifecycleAutoConfiguredAsExpected() {
		this.contextRunnerWithLifecyle()
			.run((context) -> assertThat(context).getBean(GrpcServerLifecycle.class)
				.hasFieldOrPropertyWithValue("factory", context.getBean(GrpcServerFactory.class)));
	}

	@Test
	void whenHasUserDefinedServerBuilderCustomizersDoesNotAutoConfigureBean() {
		ServerBuilderCustomizers customCustomizers = mock(ServerBuilderCustomizers.class);
		this.contextRunner()
			.withBean("customCustomizers", ServerBuilderCustomizers.class, () -> customCustomizers)
			.run((context) -> assertThat(context).getBean(ServerBuilderCustomizers.class).isSameAs(customCustomizers));
	}

	@Test
	void serverBuilderCustomizersAutoConfiguredAsExpected() {
		this.contextRunner()
			.withUserConfiguration(ServerBuilderCustomizersConfig.class)
			.run((context) -> assertThat(context).getBean(ServerBuilderCustomizers.class)
				.extracting("customizers", InstanceOfAssertFactories.list(ServerBuilderCustomizer.class))
				.containsExactly(ServerBuilderCustomizersConfig.CUSTOMIZER_BAR,
						ServerBuilderCustomizersConfig.CUSTOMIZER_FOO));
	}

	@Test
	void whenHasUserDefinedServerFactoryDoesNotAutoConfigureBean() {
		GrpcServerFactory customServerFactory = mock(GrpcServerFactory.class);
		this.contextRunner()
			.withBean("customServerFactory", GrpcServerFactory.class, () -> customServerFactory)
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class).isSameAs(customServerFactory));
	}

	@Test
	void whenShadedAndNonShadedNettyOnClasspathShadedNettyFactoryIsAutoConfigured() {
		this.contextRunner()
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(ShadedNettyGrpcServerFactory.class));
	}

	@Test
	void whenOnlyNonShadedNettyOnClasspathNonShadedNettyFactoryIsAutoConfigured() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(NettyGrpcServerFactory.class));
	}

	@Test
	void whenNeitherShadedNorNonShadedNettyOnClasspathBaseServerFactoryIsAutoConfigured() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class))
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(DefaultGrpcServerFactory.class));
	}

	@Test
	void shadedNettyServerFactoryAutoConfiguredAsExpected() {
		serverFactoryAutoConfiguredAsExpected(this.contextRunner(), ShadedNettyGrpcServerFactory.class);
	}

	@Test
	void nettyServerFactoryAutoConfiguredAsExpected() {
		serverFactoryAutoConfiguredAsExpected(this.contextRunner()
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)),
				NettyGrpcServerFactory.class);
	}

	@Test
	void baseServerFactoryAutoConfiguredAsExpected() {
		serverFactoryAutoConfiguredAsExpected(
				this.contextRunner()
					.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
							io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)),
				DefaultGrpcServerFactory.class);
	}

	private void serverFactoryAutoConfiguredAsExpected(ApplicationContextRunner contextRunner,
			Class<?> expectedServerFactoryType) {
		contextRunner.withPropertyValues("spring.grpc.server.address=myhost", "spring.grpc.server.port=6160")
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(expectedServerFactoryType)
				.hasFieldOrPropertyWithValue("address", "myhost")
				.hasFieldOrPropertyWithValue("port", 6160)
				.extracting("serviceList", InstanceOfAssertFactories.list(ServerServiceDefinition.class))
				.singleElement()
				.extracting(ServerServiceDefinition::getServiceDescriptor)
				.extracting(ServiceDescriptor::getName)
				.isEqualTo("my-service"));
	}

	@Test
	void shadedNettyServerFactoryAutoConfiguredWithCustomizers() {
		io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder builder = mock();
		serverFactoryAutoConfiguredWithCustomizers(this.contextRunnerWithLifecyle(), builder,
				ShadedNettyGrpcServerFactory.class);
	}

	@SuppressWarnings("rawtypes")
	@Test
	void nettyServerFactoryAutoConfiguredWithCustomizers() {
		// FilteredClassLoader hides the class from the auto-configuration but not from
		// the Java SPI
		// used by ServerBuilder.forPort(int) which by default returns shaded Netty. This
		// results in
		// class cast exception when NettyGrpcServerFactory is expecting a non-shaded
		// server builder.
		// We static mock the builder to return non-shaded Netty - which would happen in
		// real world.
		try (MockedStatic<ServerBuilder> serverBuilderForPort = Mockito.mockStatic(ServerBuilder.class)) {
			serverBuilderForPort.when(() -> ServerBuilder.forPort(anyInt()))
				.thenAnswer((Answer<NettyServerBuilder>) invocation -> NettyServerBuilder
					.forPort(invocation.getArgument(0)));
			NettyServerBuilder builder = mock();
			serverFactoryAutoConfiguredWithCustomizers(this.contextRunnerWithLifecyle()
				.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)),
					builder, NettyGrpcServerFactory.class);
		}
	}

	@Test
	<T extends ServerBuilder<T>> void baseServerFactoryAutoConfiguredWithCustomizers() {
		ServerBuilder<T> builder = mock();
		serverFactoryAutoConfiguredWithCustomizers(
				this.contextRunnerWithLifecyle()
					.withClassLoader(new FilteredClassLoader(NettyServerBuilder.class,
							io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)),
				builder, DefaultGrpcServerFactory.class);
	}

	@SuppressWarnings("unchecked")
	private <T extends ServerBuilder<T>> void serverFactoryAutoConfiguredWithCustomizers(
			ApplicationContextRunner contextRunner, ServerBuilder<T> mockServerBuilder,
			Class<?> expectedServerFactoryType) {
		ServerBuilderCustomizer<T> customizer1 = (serverBuilder) -> serverBuilder.keepAliveTime(40L, TimeUnit.SECONDS);
		ServerBuilderCustomizer<T> customizer2 = (serverBuilder) -> serverBuilder.keepAliveTime(50L, TimeUnit.SECONDS);
		ServerBuilderCustomizers customizers = new ServerBuilderCustomizers(List.of(customizer1, customizer2));
		contextRunner.withPropertyValues("spring.grpc.server.port=0", "spring.grpc.server.keep-alive.time=30s")
			.withBean("serverBuilderCustomizers", ServerBuilderCustomizers.class, () -> customizers)
			.run((context) -> assertThat(context).getBean(GrpcServerFactory.class)
				.isInstanceOf(expectedServerFactoryType)
				.extracting("serverBuilderCustomizers", InstanceOfAssertFactories.list(ServerBuilderCustomizer.class))
				.satisfies((allCustomizers) -> {
					allCustomizers.forEach((c) -> c.customize(mockServerBuilder));
					InOrder ordered = inOrder(mockServerBuilder);
					ordered.verify(mockServerBuilder)
						.keepAliveTime(Duration.ofSeconds(30L).toNanos(), TimeUnit.NANOSECONDS);
					ordered.verify(mockServerBuilder).keepAliveTime(40L, TimeUnit.SECONDS);
					ordered.verify(mockServerBuilder).keepAliveTime(50L, TimeUnit.SECONDS);
				}));
	}

	@Configuration(proxyBeanMethods = false)
	static class ServerBuilderCustomizersConfig {

		static ServerBuilderCustomizer<?> CUSTOMIZER_FOO = mock();

		static ServerBuilderCustomizer<?> CUSTOMIZER_BAR = mock();

		@Bean
		@Order(200)
		ServerBuilderCustomizer<?> customizerFoo() {
			return CUSTOMIZER_FOO;
		}

		@Bean
		@Order(100)
		ServerBuilderCustomizer<?> customizerBar() {
			return CUSTOMIZER_BAR;
		}

	}

}
