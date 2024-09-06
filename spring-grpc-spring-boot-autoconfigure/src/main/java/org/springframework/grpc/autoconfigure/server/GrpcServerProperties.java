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
import java.time.temporal.ChronoUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.grpc.util.GrpcUtils;

@ConfigurationProperties(prefix = "spring.grpc.server")
public class GrpcServerProperties {

	private String address = GrpcUtils.ANY_IP_ADDRESS;

	private int port = 9090;

	/**
	 * The time to wait for the server to gracefully shutdown (completing all requests
	 * after the server started to shutdown). If set to a negative value, the server waits
	 * forever. If set to {@code 0} the server will force shutdown immediately. Defaults
	 * to {@code 30s}.
	 * @param shutdownGracePeriod The time to wait for a graceful shutdown.
	 * @return The time to wait for a graceful shutdown.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration shutdownGracePeriod = Duration.of(30, ChronoUnit.SECONDS);

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Duration getShutdownGracePeriod() {
		return shutdownGracePeriod;
	}

	public void setShutdownGracePeriod(Duration shutdownGracePeriod) {
		this.shutdownGracePeriod = shutdownGracePeriod;
	}

}
