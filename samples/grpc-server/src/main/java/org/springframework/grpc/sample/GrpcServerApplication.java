package org.springframework.grpc.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;

import io.grpc.ServerInterceptor;
import io.grpc.Status;

@SpringBootApplication
public class GrpcServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplication.class, args);
	}

	@GlobalServerInterceptor
	@Bean
	public ServerInterceptor globalInterceptor() {
		return new GrpcExceptionHandlerInterceptor(exception -> {
			if (exception instanceof IllegalArgumentException) {
				return Status.INVALID_ARGUMENT.withDescription(exception.getMessage());
			}
			return null;
		});
	}

}
