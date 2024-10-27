package org.springframework.grpc.autoconfigure.server;

import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.server.ServerBuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcServerObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GrpcServerObservationAutoConfiguration.class));

	@Test
	void whenObservationRegistryNotProvided_thenObservationInterceptorNotConfigured() {
		this.contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(ServerBuilderCustomizer.class);
		});
	}

	@Test
	void whenObservationInterceptorConfigured_thenServerBuilderCustomizerConfigured() {
		this.contextRunner.withBean(ObservationRegistry.class, ObservationRegistry::create).run(context -> {
			assertThat(context).hasSingleBean(ServerBuilderCustomizer.class);
			assertThat(context).hasSingleBean(ServerInterceptor.class);
			ServerInterceptor interceptor = context.getBean(ServerInterceptor.class);
			ServerBuilderCustomizer customizer = context.getBean(ServerBuilderCustomizer.class);
			ServerBuilder<?> builder = org.mockito.Mockito.mock(ServerBuilder.class);
			customizer.customize(builder);
			org.mockito.Mockito.verify(builder, org.mockito.Mockito.times(1)).intercept(interceptor);
		});
	}

	@Test
	void whenObservationPropertyDisabled_thenServerBuilderCustomizerNotConfigured() {
		this.contextRunner.withPropertyValues("spring.grpc.server.observation.enabled=false")
			.withBean(ObservationRegistry.class, ObservationRegistry::create)
			.run(context -> {
				assertThat(context).doesNotHaveBean(ServerBuilderCustomizer.class);
				assertThat(context).doesNotHaveBean(ServerInterceptor.class);
			});
	}

}