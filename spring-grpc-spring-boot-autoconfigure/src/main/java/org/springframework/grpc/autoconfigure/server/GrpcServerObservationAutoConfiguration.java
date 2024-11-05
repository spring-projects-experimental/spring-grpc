package org.springframework.grpc.autoconfigure.server;

import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.ServerBuilderCustomizer;

@AutoConfiguration(
		afterName = "org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration")
@ConditionalOnClass(value = { ObservationRegistry.class, ObservationGrpcServerInterceptor.class })
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnProperty(value = "spring.grpc.server.observation.enabled", matchIfMissing = true)
public class GrpcServerObservationAutoConfiguration {

	@Bean
	public ServerInterceptor observationGrpcServerInterceptor(ObservationRegistry observationRegistry) {
		return new ObservationGrpcServerInterceptor(observationRegistry);
	}

	@Bean
	<T extends ServerBuilder<T>> ServerBuilderCustomizer<T> metricsInterceptor(
			ServerInterceptor observationGrpcServerInterceptor) {
		return (serverBuilder) -> serverBuilder.intercept(observationGrpcServerInterceptor);
	}

}