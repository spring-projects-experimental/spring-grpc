package org.springframework.grpc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.test.annotation.DirtiesContext;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;

@SpringBootTest
@DirtiesContext
class YetAnotherApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void serverResponds() {
		var channel = Grpc.newChannelBuilderForAddress("0.0.0.0", 9090, InsecureChannelCredentials.create()).build();
		var stub = SimpleGrpc.newBlockingStub(channel);
		HelloReply response = stub.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertEquals("Hello ==> Alien", response.getMessage());
		channel.shutdown();
	}

	@TestConfiguration
	static class ExtraConfiguration {

	}

}
