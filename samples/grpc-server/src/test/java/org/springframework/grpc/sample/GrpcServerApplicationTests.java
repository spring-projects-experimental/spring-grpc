package org.springframework.grpc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
public class GrpcServerApplicationTests {

	private static Log log = LogFactory.getLog(GrpcServerApplicationTests.class);

	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplicationTests.class, args);
	}

	@Autowired
	private SimpleGrpc.SimpleBlockingStub stub;

	@Test
	@DirtiesContext
	void contextLoads() {
	}

	@Test
	@DirtiesContext
	void serverResponds() {
		log.info("Testing");
		HelloReply response = stub.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertEquals("Hello ==> Alien", response.getMessage());
	}

	@TestConfiguration
	static class ExtraConfiguration {

		@Bean
		SimpleGrpc.SimpleBlockingStub stub(GrpcChannelFactory channels) {
			return SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:9090").build());
		}

	}

}
