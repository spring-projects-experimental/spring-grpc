package org.springframework.grpc.autoconfigure.server;

import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.server.ServerBuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcServerMetricAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GrpcServerMetricAutoConfiguration.class));

	@Test
	void whenObservationRegistryNotProvided_thenMetricsInterceptorNotConfigured() {
		this.contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(ServerBuilderCustomizer.class);
		});
	}

	@Test
	void whenMetricsInterceptorConfigured_thenServerBuilderCustomizerConfigured() {
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

}