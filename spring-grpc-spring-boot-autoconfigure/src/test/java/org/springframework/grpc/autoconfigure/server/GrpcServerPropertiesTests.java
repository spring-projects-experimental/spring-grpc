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

package org.springframework.grpc.autoconfigure.server;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GrpcServerProperties}.
 *
 * @author Chris Bono
 */
class GrpcServerPropertiesTests {

	private GrpcServerProperties bindProperties(Map<String, String> map) {
		return new Binder(new MapConfigurationPropertySource(map))
			.bind("spring.grpc.server", GrpcServerProperties.class)
			.get();
	}

	@Nested
	class BaseProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.address", "my-server-ip");
			map.put("spring.grpc.server.port", "3130");
			map.put("spring.grpc.server.shutdown-grace-period", "15s");
			GrpcServerProperties properties = bindProperties(map);
			assertThat(properties.getAddress()).isEqualTo("my-server-ip");
			assertThat(properties.getPort()).isEqualTo(3130);
		}

	}

	@Nested
	class KeepAliveProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.keep-alive.time", "45m");
			map.put("spring.grpc.server.keep-alive.timeout", "40s");
			GrpcServerProperties.KeepAlive properties = bindProperties(map).getKeepAlive();
			assertThat(properties.getTime()).isEqualTo(Duration.ofMinutes(45));
			assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(40));
		}

	}

}
