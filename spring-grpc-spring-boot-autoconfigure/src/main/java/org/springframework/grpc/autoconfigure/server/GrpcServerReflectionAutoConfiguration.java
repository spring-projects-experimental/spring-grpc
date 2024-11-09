package org.springframework.grpc.autoconfigure.server;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC Reflection service
 * <p>
 * spring.grpc.reflection.enabled=true must be present in configuration in order for the
 * auto-configuration to execute
 *
 * @author Haris Zujo
 */
@AutoConfiguration
@ConditionalOnClass(ProtoReflectionService.class)
@ConditionalOnProperty(name = "spring.grpc.server.reflection.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcServerReflectionAutoConfiguration {

	@Bean
	public BindableService serverReflection() {
		return ProtoReflectionService.newInstance();
	}
}
