package org.springframework.grpc.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

import io.grpc.Status;

@SpringBootApplication
public class GrpcServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplication.class, args);
	}

	@Bean
	public GrpcExceptionHandler globalInterceptor() {
		return exception -> {
			if (exception instanceof IllegalArgumentException) {
				return Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).asException();
			}
			return null;
		};
	}

}
