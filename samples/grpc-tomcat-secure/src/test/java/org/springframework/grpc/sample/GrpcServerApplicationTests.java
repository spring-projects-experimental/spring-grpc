package org.springframework.grpc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.grpc.client.ChannelBuilderOptions;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.test.LocalGrpcPort;
import org.springframework.test.annotation.DirtiesContext;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Status.Code;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GrpcServerApplicationTests {

	private static Log log = LogFactory.getLog(GrpcServerApplicationTests.class);

	public static void main(String[] args) {
		new SpringApplicationBuilder(GrpcServerApplication.class, ExtraConfiguration.class).run(args);
	}

	@Autowired
	@Qualifier("stub")
	private SimpleGrpc.SimpleBlockingStub stub;

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
	void authenticated() {
		log.info("Testing");
		HelloReply response = basic.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertEquals("Hello ==> Alien", response.getMessage());
	}

	@TestConfiguration
	static class ExtraConfiguration {

		@Bean
		@Lazy
		SimpleGrpc.SimpleBlockingStub basic(GrpcChannelFactory channels, @LocalServerPort int port) {
			return SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:" + port,
					ChannelBuilderOptions.defaults().withCustomizer((__, channel) -> {
						channel.intercept(new ClientInterceptor() {
							@Override
							public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
									MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
								return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
									public void start(ClientCall.Listener<RespT> responseListener,
											io.grpc.Metadata headers) {
										headers.put(GrpcSecurity.AUTHORIZATION_KEY,
												"Basic " + Base64.getEncoder().encodeToString("user:user".getBytes()));
										super.start(responseListener, headers);
									};
								};
							}
						});
					})));
		}

		@Bean
		@Lazy
		SimpleGrpc.SimpleBlockingStub stub(GrpcChannelFactory channels, @LocalServerPort int port) {
			return SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:" + port));
		}

	}

}
