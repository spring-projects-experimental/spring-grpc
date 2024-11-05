package org.springframework.grpc.autoconfigure.server;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC Reflection service
 * <p>
 * grpc.reflection.enabled=true must be present in configuration in order for the
 * auto-configuration to execute
 *
 * @author Haris Zujo
 */
@Configuration
@ConditionalOnClass(ProtoReflectionService.class)
public class GrpcServerReflectionAutoConfiguration {

	@Bean
	@ConditionalOnProperty(name = "grpc.server.reflection.enabled", havingValue = "true")
	public BindableService serverReflection() {
		return ProtoReflectionService.newInstance();
	}

}
