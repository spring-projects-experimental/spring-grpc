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

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.test.annotation.DirtiesContext;

import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.health.v1.HealthGrpc.HealthBlockingStub;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for gRPC server health feature.
 */
class GrpcServerHealthIntegrationTests {

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.health.actuator.health-indicator-paths=custom",
			"spring.grpc.server.health.actuator.update-initial-delay=3s",
			"spring.grpc.server.health.actuator.update-rate=3s", "management.health.defaults.enabled=true" })
	@DirtiesContext
	class WithActuatorHealthAdapter {

		@Test
		void healthIndicatorsAdaptedToGprcHealthStatus(@Autowired GrpcChannelFactory channels) {
			var channel = channels.createChannel("0.0.0.0:0").build();
			var healthStub = HealthGrpc.newBlockingStub(channel);
			var serviceName = "custom";

			// initially the status should be SERVING
			assertThatGrpcHealthStatusIs(healthStub, serviceName, ServingStatus.SERVING, Duration.ofSeconds(4));

			// put the service down and the status should then be NOT_SERVING
			CustomHealthIndicator.SERVICE_IS_UP = false;
			assertThatGrpcHealthStatusIs(healthStub, serviceName, ServingStatus.NOT_SERVING, Duration.ofSeconds(4));

			// put the service up and the status should be SERVING
			CustomHealthIndicator.SERVICE_IS_UP = true;
			assertThatGrpcHealthStatusIs(healthStub, serviceName, ServingStatus.SERVING, Duration.ofSeconds(4));
		}

		private void assertThatGrpcHealthStatusIs(HealthBlockingStub healthBlockingStub, String service,
				ServingStatus expectedStatus, Duration maxWaitTime) {
			Awaitility.await().atMost(maxWaitTime).ignoreException(StatusRuntimeException.class).untilAsserted(() -> {
				var healthRequest = HealthCheckRequest.newBuilder().setService(service).build();
				var healthResponse = healthBlockingStub.check(healthRequest);
				assertThat(healthResponse.getStatus()).isEqualTo(expectedStatus);
				var overallHealthRequest = HealthCheckRequest.newBuilder().setService("").build();
				var overallHealthResponse = healthBlockingStub.check(overallHealthRequest);
				assertThat(overallHealthResponse.getStatus()).isEqualTo(expectedStatus);
			});
		}

		@TestConfiguration
		static class MyHealthIndicatorsConfig {

			@ConditionalOnEnabledHealthIndicator("custom")
			@Bean
			CustomHealthIndicator customHealthIndicator() {
				return new CustomHealthIndicator();
			}

		}

		static class CustomHealthIndicator implements HealthIndicator {

			static boolean SERVICE_IS_UP = true;

			@Override
			public Health health() {
				return SERVICE_IS_UP ? Health.up().build() : Health.down().build();
			}

		}

	}

}
