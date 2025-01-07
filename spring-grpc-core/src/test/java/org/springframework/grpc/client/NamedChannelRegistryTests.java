/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.unit.DataSize;

/**
 * Tests for {@link NamedChannelRegistry}.
 */
class NamedChannelRegistryTests {

	@Nested
	class GetChannelAPI {

		@Test
		void withDefaultNameReturnsDefaultChannel() {
			var defaultChannel = new NamedChannel();
			var registry = new NamedChannelRegistry(defaultChannel, Map.of());
			assertThat(registry.getChannel("default")).isSameAs(defaultChannel);
			assertThat(registry.getDefaultChannel()).isSameAs(defaultChannel);
			assertThat(registry).extracting("channels", InstanceOfAssertFactories.MAP).isEmpty();
		}

		@Test
		void withKnownNameReturnsKnownChannel() {
			var defaultChannel = new NamedChannel();
			var channel1 = new NamedChannel();
			var registry = new NamedChannelRegistry(defaultChannel, Map.of("c1", channel1));
			assertThat(registry.getChannel("c1")).isSameAs(channel1);
			assertThat(registry).extracting("channels", InstanceOfAssertFactories.MAP)
				.containsExactly(entry("c1", channel1));
		}

		@Test
		void withUnknownNameReturnsNewChannelWithCopiedDefaults() {
			var defaultChannel = new NamedChannel();
			defaultChannel.setAddress("static://my-server:9999");
			defaultChannel.setDefaultLoadBalancingPolicy("custom");
			defaultChannel.getHealth().setEnabled(true);
			defaultChannel.getHealth().setServiceName("custom-service");
			defaultChannel.setEnableKeepAlive(true);
			defaultChannel.setIdleTimeout(Duration.ofMinutes(1));
			defaultChannel.setKeepAliveTime(Duration.ofMinutes(4));
			defaultChannel.setKeepAliveTimeout(Duration.ofMinutes(6));
			defaultChannel.setKeepAliveWithoutCalls(true);
			defaultChannel.setMaxInboundMessageSize(DataSize.ofMegabytes(100));
			defaultChannel.setMaxInboundMetadataSize(DataSize.ofMegabytes(200));
			defaultChannel.setUserAgent("me");
			defaultChannel.getSsl().setEnabled(true);
			defaultChannel.getSsl().setBundle("custom-bundle");
			var registry = new NamedChannelRegistry(defaultChannel, Map.of());
			var newChannel = registry.getChannel("new-channel");
			assertThat(newChannel).usingRecursiveComparison().ignoringFields("address").isEqualTo(defaultChannel);
			assertThat(registry).extracting("channels", InstanceOfAssertFactories.MAP)
				.containsExactly(entry("new-channel", newChannel));
		}

	}

	@Nested
	class GetTargetAPI {

		@Test
		void channelWithStaticAddressReturnsStrippedAddress() {
			var defaultChannel = new NamedChannel();
			var channel1 = new NamedChannel();
			channel1.setAddress("static://my-server:8888");
			var registry = new NamedChannelRegistry(defaultChannel, Map.of("c1", channel1));
			assertThat(registry.getTarget("c1")).isEqualTo("my-server:8888");
			assertThat(registry).extracting("channels", InstanceOfAssertFactories.MAP)
				.containsExactly(entry("c1", channel1));
		}

		@Test
		void channelWithTcpAddressReturnsStrippedAddress() {
			var defaultChannel = new NamedChannel();
			var channel1 = new NamedChannel();
			channel1.setAddress("tcp://my-server:8888");
			var registry = new NamedChannelRegistry(defaultChannel, Map.of("c1", channel1));
			assertThat(registry.getTarget("c1")).isEqualTo("my-server:8888");
			assertThat(registry).extracting("channels", InstanceOfAssertFactories.MAP)
				.containsExactly(entry("c1", channel1));
		}

		@Test
		void channelWithAddressPropertyPlaceholdersPopulatesFromEnvironment() {
			var defaultChannel = new NamedChannel();
			var channel1 = new NamedChannel();
			channel1.setAddress("my-server-${channelName}:8888");
			var registry = new NamedChannelRegistry(defaultChannel, Map.of("c1", channel1));
			var env = new MockEnvironment();
			env.setProperty("channelName", "foo");
			registry.setEnvironment(env);
			assertThat(registry.getTarget("c1")).isEqualTo("my-server-foo:8888");
			assertThat(registry).extracting("channels", InstanceOfAssertFactories.MAP)
				.containsExactly(entry("c1", channel1));
		}

	}

	@Nested
	class CopyDefaultsAPI {

		@Test
		void copyFromDefaultChannel() {
			var registry = new NamedChannelRegistry(new NamedChannel(), Map.of());
			var defaultChannel = registry.getDefaultChannel();
			var newChannel = defaultChannel.copy();
			assertThat(newChannel).usingRecursiveComparison().isEqualTo(defaultChannel);
		}

	}

}
