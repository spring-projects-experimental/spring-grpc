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

package org.springframework.grpc.autoconfigure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.grpc.client.NegotiationType;
import org.springframework.util.unit.DataSize;

/**
 * Tests for {@link GrpcClientProperties}.
 *
 * @author Chris Bono
 */
class GrpcClientPropertiesTests {

	private GrpcClientProperties bindProperties(Map<String, String> map) {
		return new Binder(new MapConfigurationPropertySource(map))
			.bind("spring.grpc.client", GrpcClientProperties.class)
			.get();
	}

	@Nested
	class BindPropertiesAPI {

		@Test
		void withDefaultValues() {
			Map<String, String> map = new HashMap<>();
			// we have to at least bind one property or bind() fails
			map.put("spring.grpc.client.default-channel.enable-keep-alive", "false");
			GrpcClientProperties properties = bindProperties(map);
			var defaultChannel = properties.getDefaultChannel();
			assertThat(defaultChannel.getAddress()).isEqualTo("static://localhost:9090");
			assertThat(defaultChannel.getDefaultLoadBalancingPolicy()).isEqualTo("round_robin");
			assertThat(defaultChannel.getHealth().isEnabled()).isFalse();
			assertThat(defaultChannel.getHealth().getServiceName()).isNull();
			assertThat(defaultChannel.getNegotiationType()).isEqualTo(NegotiationType.PLAINTEXT);
			assertThat(defaultChannel.isEnableKeepAlive()).isFalse();
			assertThat(defaultChannel.getIdleTimeout()).isEqualTo(Duration.ofSeconds(20));
			assertThat(defaultChannel.getKeepAliveTime()).isEqualTo(Duration.ofMinutes(5));
			assertThat(defaultChannel.getKeepAliveTimeout()).isEqualTo(Duration.ofSeconds(20));
			assertThat(defaultChannel.isEnableKeepAlive()).isFalse();
			assertThat(defaultChannel.isKeepAliveWithoutCalls()).isFalse();
			assertThat(defaultChannel.getMaxInboundMessageSize()).isEqualTo(DataSize.ofBytes(4194304));
			assertThat(defaultChannel.getMaxInboundMetadataSize()).isEqualTo(DataSize.ofBytes(8192));
			assertThat(defaultChannel.getUserAgent()).isNull();
			assertThat(defaultChannel.isSecure()).isTrue();
			assertThat(defaultChannel.getSsl().isEnabled()).isFalse();
			assertThat(defaultChannel.getSsl().getBundle()).isNull();
		}

		@Test
		void withSpecifiedValues() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.client.default-channel.address", "static://my-server:8888");
			map.put("spring.grpc.client.default-channel.default-load-balancing-policy", "pick_first");
			map.put("spring.grpc.client.default-channel.health.enabled", "true");
			map.put("spring.grpc.client.default-channel.health.service-name", "my-service");
			map.put("spring.grpc.client.default-channel.negotiation-type", "plaintext_upgrade");
			map.put("spring.grpc.client.default-channel.enable-keep-alive", "true");
			map.put("spring.grpc.client.default-channel.idle-timeout", "1m");
			map.put("spring.grpc.client.default-channel.keep-alive-time", "200s");
			map.put("spring.grpc.client.default-channel.keep-alive-timeout", "60000ms");
			map.put("spring.grpc.client.default-channel.keep-alive-without-calls", "true");
			map.put("spring.grpc.client.default-channel.max-inbound-message-size", "200MB");
			map.put("spring.grpc.client.default-channel.max-inbound-metadata-size", "1GB");
			map.put("spring.grpc.client.default-channel.user-agent", "me");
			map.put("spring.grpc.client.default-channel.secure", "false");
			map.put("spring.grpc.client.default-channel.ssl.enabled", "true");
			map.put("spring.grpc.client.default-channel.ssl.bundle", "my-bundle");
			GrpcClientProperties properties = bindProperties(map);
			var defaultChannel = properties.getDefaultChannel();
			assertThat(defaultChannel.getAddress()).isEqualTo("static://my-server:8888");
			assertThat(defaultChannel.getDefaultLoadBalancingPolicy()).isEqualTo("pick_first");
			assertThat(defaultChannel.getHealth().isEnabled()).isTrue();
			assertThat(defaultChannel.getHealth().getServiceName()).isEqualTo("my-service");
			assertThat(defaultChannel.getNegotiationType()).isEqualTo(NegotiationType.PLAINTEXT_UPGRADE);
			assertThat(defaultChannel.isEnableKeepAlive()).isTrue();
			assertThat(defaultChannel.getIdleTimeout()).isEqualTo(Duration.ofMinutes(1));
			assertThat(defaultChannel.getKeepAliveTime()).isEqualTo(Duration.ofSeconds(200));
			assertThat(defaultChannel.getKeepAliveTimeout()).isEqualTo(Duration.ofMillis(60000));
			assertThat(defaultChannel.isEnableKeepAlive()).isTrue();
			assertThat(defaultChannel.isKeepAliveWithoutCalls()).isTrue();
			assertThat(defaultChannel.getMaxInboundMessageSize()).isEqualTo(DataSize.ofMegabytes(200));
			assertThat(defaultChannel.getMaxInboundMetadataSize()).isEqualTo(DataSize.ofGigabytes(1));
			assertThat(defaultChannel.getUserAgent()).isEqualTo("me");
			assertThat(defaultChannel.isSecure()).isFalse();
			assertThat(defaultChannel.getSsl().isEnabled()).isTrue();
			assertThat(defaultChannel.getSsl().getBundle()).isEqualTo("my-bundle");
		}

		@Test
		void withoutKeepAliveUnitsSpecified() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.client.default-channel.idle-timeout", "1");
			map.put("spring.grpc.client.default-channel.keep-alive-time", "60");
			map.put("spring.grpc.client.default-channel.keep-alive-timeout", "5");
			GrpcClientProperties properties = bindProperties(map);
			var defaultChannel = properties.getDefaultChannel();
			assertThat(defaultChannel.getIdleTimeout()).isEqualTo(Duration.ofSeconds(1));
			assertThat(defaultChannel.getKeepAliveTime()).isEqualTo(Duration.ofSeconds(60));
			assertThat(defaultChannel.getKeepAliveTimeout()).isEqualTo(Duration.ofSeconds(5));
		}

		@Test
		void withoutInboundSizeUnitsSpecified() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.client.default-channel.max-inbound-message-size", "1000");
			map.put("spring.grpc.client.default-channel.max-inbound-metadata-size", "256");
			GrpcClientProperties properties = bindProperties(map);
			var defaultChannel = properties.getDefaultChannel();
			assertThat(defaultChannel.getMaxInboundMessageSize()).isEqualTo(DataSize.ofBytes(1000));
			assertThat(defaultChannel.getMaxInboundMetadataSize()).isEqualTo(DataSize.ofBytes(256));
		}

	}

	@Nested
	class CopyDefaultsAPI {

		@Test
		void withNoUserSpecifiedValues() {
			GrpcClientProperties properties = new GrpcClientProperties();
			var defaultChannel = properties.getDefaultChannel();
			var newChannel = defaultChannel.copy();
			assertThat(newChannel).usingRecursiveComparison().isEqualTo(defaultChannel);
		}

	}

	@Nested
	class GetChannelAPI {

		@Test
		void withDefaultNameReturnsDefaultChannel() {
			GrpcClientProperties properties = new GrpcClientProperties();
			var defaultChannel = properties.getDefaultChannel();
			assertThat(properties.getChannel("default")).isSameAs(defaultChannel);
			assertThat(properties.getChannels()).hasSize(0);
		}

		@Test
		void withUnknownNameReturnsNewChannelWithCopiedDefaults() {
			GrpcClientProperties properties = new GrpcClientProperties();
			var defaultChannel = properties.getDefaultChannel();
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
			var newChannel = properties.getChannel("new");
			assertThat(newChannel).usingRecursiveComparison().ignoringFields("address").isEqualTo(defaultChannel);
			assertThat(properties.getChannels()).containsExactly(entry("new", newChannel));
		}

	}

	@Nested
	class GetTargetAPI {

		@Test
		void withCustomChannelReturnsCustomChannelAddress() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.client.channels.custom.address", "static://my-server:8888");
			GrpcClientProperties properties = bindProperties(map);
			assertThat(properties.getTarget("custom")).isEqualTo("my-server:8888");
			assertThat(properties.getChannels()).containsOnlyKeys("custom");
		}

		@Test
		void withCustomChannelReturnsDuplicateEntryMap() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.client.channels.custom.address", "static://my-server:8888");
			GrpcClientProperties properties = bindProperties(map);
			GrpcClientAutoConfiguration.NamedChannelVirtualTargets virtualTargets = new GrpcClientAutoConfiguration.NamedChannelVirtualTargets(
					properties);
			var address = virtualTargets.getTarget("custom");
			assertThat(address).isEqualTo("my-server:8888");
			assertThat(properties.getTarget("custom")).isEqualTo("my-server:8888");
			assertThat(properties.getChannels()).containsOnlyKeys("custom");
		}

	}

}
