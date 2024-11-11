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

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest
class InProcessApplicationContextInitializerTests {

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.host=0.0.0.0", "spring.grpc.server.port=0",
			"spring.grpc.inprocess.enabled=true" })
	class WithInProcessEnabled {

		@Autowired
		private ConfigurableApplicationContext context;

		@Test
		void testInitialize_WithEnabledProperty_ShouldRegisterServerAndChannel() {
			ManagedChannel channel = context.getBean("grpcInProcessChannel", ManagedChannel.class);
			assertThat(channel).isNotNull();
			assertThat(channel.isShutdown()).isFalse();
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.host=0.0.0.0", "spring.grpc.server.port=0",
			"spring.grpc.inprocess.enabled=false" })
	class WithInProcessDisabled {

		@Autowired
		private ConfigurableApplicationContext context;

		@Test
		void testInitialize_WithDisabledProperty_ShouldNotRegisterServerAndChannel() {
			assertThat(context.containsBean("grpcInProcessChannel")).isFalse();
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.host=0.0.0.0", "spring.grpc.server.port=0",
			"spring.grpc.inprocess.enabled=true" })
	class WithDefaultInProcessEnabled {

		@Autowired
		private ConfigurableApplicationContext context;

		@Test
		void testDefaultEnabledProperty_ShouldRegisterServerAndChannel() {
			ManagedChannel channel = context.getBean("grpcInProcessChannel", ManagedChannel.class);
			assertThat(channel).isNotNull();
		}

	}

}