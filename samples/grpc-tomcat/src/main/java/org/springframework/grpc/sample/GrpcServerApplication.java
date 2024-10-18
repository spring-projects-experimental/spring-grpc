package org.springframework.grpc.sample;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.autoconfigure.server.GrpcServerAutoConfiguration;

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.servlet.jakarta.GrpcServlet;

@SpringBootApplication(exclude = GrpcServerAutoConfiguration.class)
public class GrpcServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplication.class, args);
	}

	@Bean
	public ServletRegistrationBean<GrpcServlet> grpcServlet(List<BindableService> services) {
		List<String> paths = services.stream()
			.map(service -> "/" + service.bindService().getServiceDescriptor().getName() + "/*")
			.collect(Collectors.toList());
		ServletRegistrationBean<GrpcServlet> servlet = new ServletRegistrationBean<>(new GrpcServlet(services));
		servlet.setUrlMappings(paths);
		return servlet;
	}

	@Bean
	public BindableService serverReflection() {
		return ProtoReflectionService.newInstance();
	}

}
