package org.springframework.grpc.sample;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.grpc.client.ChannelBuilderOptions;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.security.BasicAuthenticationInterceptor;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.grpc.test.LocalGrpcPort;
import org.springframework.test.annotation.DirtiesContext;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.reflection.v1.ServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.stub.StreamObserver;

@SpringBootTest(properties = { "spring.grpc.server.port=0",
		"spring.grpc.client.channels.stub.address=static://0.0.0.0:${local.grpc.port}" })
public class GrpcServerApplicationTests {

	public static void main(String[] args) {
		new SpringApplicationBuilder(GrpcServerApplication.class, ExtraConfiguration.class).run(args);
	}

	@Autowired
	@Qualifier("stub")
	private SimpleGrpc.SimpleBlockingStub stub;

	@Autowired
	@Qualifier("reflect")
	private ServerReflectionGrpc.ServerReflectionStub reflect;

	@Autowired
	@Qualifier("basic")
	private SimpleGrpc.SimpleBlockingStub basic;

	@Test
	@DirtiesContext
	void contextLoads() {
	}

	@Test
	@DirtiesContext
	void unauthenticated() {
		StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
				() -> stub.sayHello(HelloRequest.newBuilder().setName("Alien").build()));
		assertEquals(Code.UNAUTHENTICATED, exception.getStatus().getCode());
	}

	@Test
	@DirtiesContext
	void anonymous() throws Exception {
		AtomicReference<ServerReflectionResponse> response = new AtomicReference<>();
		AtomicBoolean error = new AtomicBoolean();
		StreamObserver<ServerReflectionResponse> responses = new StreamObserver<ServerReflectionResponse>() {
			@Override
			public void onNext(ServerReflectionResponse value) {
				response.set(value);
			}

			@Override
			public void onError(Throwable t) {
				error.set(true);
			}

			@Override
			public void onCompleted() {
			}
		};
		StreamObserver<ServerReflectionRequest> request = reflect.serverReflectionInfo(responses);
		request.onNext(ServerReflectionRequest.newBuilder().setListServices("").build());
		request.onCompleted();
		Awaitility.await().until(() -> response.get() != null || error.get());
	}

	@Test
	@DirtiesContext
	void unauthauthorized() {
		StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
				() -> basic.streamHello(HelloRequest.newBuilder().setName("Alien").build()).next());
		assertEquals(Code.PERMISSION_DENIED, exception.getStatus().getCode());
	}

	@Test
	@DirtiesContext
	void authenticated() {
		HelloReply response = basic.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertEquals("Hello ==> Alien", response.getMessage());
	}

	@Test
	@DirtiesContext
	void basic() {
		HelloReply response = basic.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertEquals("Hello ==> Alien", response.getMessage());
	}

	@TestConfiguration
	static class ExtraConfiguration {

		@Bean
		@Lazy
		SimpleGrpc.SimpleBlockingStub basic(GrpcChannelFactory channels) {
			return SimpleGrpc.newBlockingStub(channels.createChannel("stub", ChannelBuilderOptions.defaults()
				.withInterceptors(List.of(new BasicAuthenticationInterceptor("user", "user")))));
		}

		@Bean
		@Lazy
		SimpleGrpc.SimpleBlockingStub stub(GrpcChannelFactory channels, @LocalGrpcPort int port) {
			return SimpleGrpc.newBlockingStub(channels.createChannel("stub"));
		}

		@Bean
		@Lazy
		ServerReflectionGrpc.ServerReflectionStub reflect(GrpcChannelFactory channels, @LocalGrpcPort int port) {
			return ServerReflectionGrpc.newStub(channels.createChannel("stub"));
		}

	}

}
