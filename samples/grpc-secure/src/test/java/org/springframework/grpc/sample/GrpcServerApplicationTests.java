package org.springframework.grpc.sample;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

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

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;

@SpringBootTest(properties = { "spring.grpc.server.port=0",
		"spring.grpc.client.channels.stub.address=static://0.0.0.0:${local.grpc.port}",
		"spring.grpc.client.channels.basic.address=static://0.0.0.0:${local.grpc.port}",
		"spring.grpc.client.channels.secure.address=static://0.0.0.0:${local.grpc.port}" })
public class GrpcServerApplicationTests {

	public static void main(String[] args) {
		new SpringApplicationBuilder(GrpcServerApplication.class, ExtraConfiguration.class).run(args);
	}

	@Autowired
	@Qualifier("stub")
	private SimpleGrpc.SimpleBlockingStub stub;

	@Autowired
	@Qualifier("secure")
	private SimpleGrpc.SimpleBlockingStub secure;

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
	void unauthauthorized() {
		StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
				() -> secure.streamHello(HelloRequest.newBuilder().setName("Alien").build()).next());
		assertEquals(Code.PERMISSION_DENIED, exception.getStatus().getCode());
	}

	@Test
	@DirtiesContext
	void authenticated() {
		HelloReply response = secure.sayHello(HelloRequest.newBuilder().setName("Alien").build());
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
		SimpleGrpc.SimpleBlockingStub secure(GrpcChannelFactory channels) {
			return SimpleGrpc.newBlockingStub(channels.createChannel("secure",
					ChannelBuilderOptions.defaults().withInterceptors(List.of(new ClientInterceptor() {
						@Override
						public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
								CallOptions callOptions, Channel next) {
							return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
								public void start(ClientCall.Listener<RespT> responseListener,
										io.grpc.Metadata headers) {
									headers.put(GrpcServerApplication.USER_KEY, "user");
									super.start(responseListener, headers);
								};
							};
						}
					}))));
		}

		@Bean
		@Lazy
		SimpleGrpc.SimpleBlockingStub basic(GrpcChannelFactory channels) {
			return SimpleGrpc.newBlockingStub(channels.createChannel("basic", ChannelBuilderOptions.defaults()
				.withInterceptors(List.of(new BasicAuthenticationInterceptor("user", "user")))));
		}

		@Bean
		@Lazy
		SimpleGrpc.SimpleBlockingStub stub(GrpcChannelFactory channels, @LocalGrpcPort int port) {
			return SimpleGrpc.newBlockingStub(channels.createChannel("stub"));
		}

	}

}
