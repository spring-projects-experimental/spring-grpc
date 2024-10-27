package org.springframework.grpc.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.MeterRegistry;
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
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.grpc.test.LocalGrpcPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = "spring.grpc.server.port=0")
public class GrpcServerApplicationTests {

	private static Log log = LogFactory.getLog(GrpcServerApplicationTests.class);

	public static void main(String[] args) {
		new SpringApplicationBuilder(GrpcServerApplication.class, ExtraConfiguration.class).profiles("ssl").run(args);
	}

	@Autowired
	private SimpleGrpc.SimpleBlockingStub stub;

	@Autowired
	private MeterRegistry meterRegistry;

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

	@Test
	@DirtiesContext
	void verifyMetrics() {
		log.info("Verify stats are collected");
		stub.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertThat(meterRegistry.get("grpc.server.requests.received").counter().count()).isEqualTo(1);
		assertThat(meterRegistry.get("grpc.server.responses.sent").counter().count()).isEqualTo(1);
		stub.sayHello(HelloRequest.newBuilder().setName("Jamie").build());
		assertThat(meterRegistry.get("grpc.server.requests.received").counter().count()).isEqualTo(2);
		assertThat(meterRegistry.get("grpc.server.responses.sent").counter().count()).isEqualTo(2);
	}

	@TestConfiguration
	static class ExtraConfiguration {

		@Bean
		@Lazy
		SimpleGrpc.SimpleBlockingStub stub(GrpcChannelFactory channels, @LocalGrpcPort int port) {
			return SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:" + port).build());
		}

	}

}
