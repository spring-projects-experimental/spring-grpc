package org.springframework.grpc.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.ReactorSimpleGrpc;
import org.springframework.grpc.sample.proto.ReactorSimpleGrpc.ReactorSimpleStub;
import org.springframework.grpc.test.AutoConfigureInProcessTransport;
import org.springframework.grpc.test.LocalGrpcPort;
import org.springframework.test.annotation.DirtiesContext;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@AutoConfigureInProcessTransport
public class GrpcServerApplicationTests {

	private static Log log = LogFactory.getLog(GrpcServerApplicationTests.class);

	public static void main(String[] args) {
		new SpringApplicationBuilder(GrpcServerApplication.class, ExtraConfiguration.class).run();
	}

	@Autowired
	private ReactorSimpleStub stub;

	@Test
	@DirtiesContext
	void contextLoads() {
	}

	@Test
	@DirtiesContext
	void serverResponds() {
		log.info("Testing");
		Mono<HelloReply> reply = stub.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		StepVerifier.create(reply.map(response -> response.getMessage()))
			.expectNext("Hello ==> Alien")
			.verifyComplete();
	}

	@TestConfiguration
	static class ExtraConfiguration {

		@Bean
		@Lazy
		ReactorSimpleStub stub(GrpcChannelFactory channels, @LocalGrpcPort int port) {
			return ReactorSimpleGrpc.newReactorStub(channels.createChannel("0.0.0.0:" + port));
		}

	}

}
