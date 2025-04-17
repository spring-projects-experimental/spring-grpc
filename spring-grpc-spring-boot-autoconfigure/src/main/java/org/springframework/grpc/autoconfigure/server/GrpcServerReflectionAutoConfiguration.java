package org.springframework.grpc.autoconfigure.server;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC Reflection service
 * <p>
 * This auto-configuration is enabled by default. To disable it, set the configuration
 * flag {spring.grpc.server.reflection.enabled=false} in your application properties.
 *
 * @author Haris Zujo
 */
@AutoConfiguration(before = GrpcServerFactoryAutoConfiguration.class)
@ConditionalOnGrpcServerEnabled
@ConditionalOnClass(ProtoReflectionServiceV1.class)
@ConditionalOnProperty(name = "spring.grpc.server.reflection.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcServerReflectionAutoConfiguration {

	@Bean
	public BindableService serverReflection() {
		return ProtoReflectionServiceV1.newInstance();
	}

}
