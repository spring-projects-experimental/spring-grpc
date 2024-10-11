/*
 * Copyright 2024-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.grpc.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.autoconfigure.server.GrpcServerProperties;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.test.LocalGrpcPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import io.grpc.ManagedChannel;

/**
 * More detailed integration tests for {@link GrpcServerFactory gRPC server factories} and
 * various {@link GrpcServerProperties}.
 */
class GrpcServerIntegrationTests {

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.address=0.0.0.0", "spring.grpc.server.port=0" })
	class ServerWithAnyIPv4AddressAndRandomPort {

		@Test
		void servesResponseToClientWithAnyIPv4AddressAndRandomPort(@Autowired GrpcChannelFactory channels,
				@LocalGrpcPort int port) {
			assertThatResponseIsServedToChannel(channels.createChannel("0.0.0.0:" + port).build());
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.address=::", "spring.grpc.server.port=0" })
	class ServerWithAnyIPv6AddressAndRandomPort {

		@Test
		void servesResponseToClientWithAnyIPv4AddressAndRandomPort(@Autowired GrpcChannelFactory channels,
				@LocalGrpcPort int port) {
			assertThatResponseIsServedToChannel(channels.createChannel("0.0.0.0:" + port).build());
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.address=127.0.0.1", "spring.grpc.server.port=0" })
	class ServerWithLocalhostAndRandomPort {

		@Test
		void servesResponseToClientWithLocalhostAndRandomPort(@Autowired GrpcChannelFactory channels,
				@LocalGrpcPort int port) {
			assertThatResponseIsServedToChannel(channels.createChannel("127.0.0.1:" + port).build());
		}

	}

	@Nested
	@SpringBootTest(properties = "spring.grpc.client.channels.test-channel.address=static://0.0.0.0:9090")
	@DirtiesContext
	class ServerConfiguredWithStaticClientChannel {

		@Test
		void servesResponseToClientWithConfiguredChannel(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("test-channel").build());
		}

	}

	@Nested
	@SpringBootTest(properties = "spring.grpc.server.address=unix:unix-test-channel")
	@EnabledOnOs(OS.LINUX)
	class ServerWithUnixDomain {

		@Test
		void clientChannelWithUnixDomain(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("unix:unix-test-channel").build());
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.client.channels.test-channel.address=static://0.0.0.0:9090",
			"spring.grpc.client.channels.test-channel.negotiation-type=TLS",
			"spring.grpc.client.channels.test-channel.secure=false" })
	@ActiveProfiles("ssl")
	@DirtiesContext
	class ServerWithSsl {

		@Test
		void clientChannelWithSsl(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("test-channel").build());
		}

	}

	private void assertThatResponseIsServedToChannel(ManagedChannel clientChannel) {
		SimpleGrpc.SimpleBlockingStub client = SimpleGrpc.newBlockingStub(clientChannel);
		HelloReply response = client.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertThat(response.getMessage()).isEqualTo("Hello ==> Alien");
	}

}
