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

@ConfigurationProperties(prefix = "spring.grpc.server")
public class GrpcServerProperties {

	/**
	 * Server should listen to any IPv4 and IPv6 address.
	 */
	public static final String ANY_IP_ADDRESS = "*";

	/**
	 * Server should listen to any IPv4 address.
	 */
	public static final String ANY_IPv4_ADDRESS = "0.0.0.0";

	/**
	 * Server should listen to any IPv6 address.
	 */
	public static final String ANY_IPv6_ADDRESS = "::";

	/**
	 * Server address to bind to. The default is any IP address ('*').
	 */
	private String address = ANY_IP_ADDRESS;

	/**
	 * Server port to listen on. When the value is 0, a random available port is selected.
	 */
	private int port = 9090;

	/**
	 * Maximum time to wait for the server to gracefully shutdown. When the value is
	 * negative, the server waits forever. When the value is 0, the server will force
	 * shutdown immediately. The default is 30 seconds.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration shutdownGracePeriod = Duration.of(30, ChronoUnit.SECONDS);

	private final KeepAlive keepAlive = new KeepAlive();

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

	public KeepAlive getKeepAlive() {
		return this.keepAlive;
	}

	public static class KeepAlive {

		/**
		 * Duration without read activity before sending a keep alive ping (default 2h).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration time = Duration.of(2, ChronoUnit.HOURS);

		/**
		 * Maximum time to wait for read activity after sending a keep alive ping. If
		 * sender does not receive an acknowledgment within this time, it will close the
		 * connection (default 20s).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration timeout = Duration.of(20, ChronoUnit.SECONDS);

		public Duration getTime() {
			return time;
		}

		public void setTime(Duration time) {
			this.time = time;
		}

		public Duration getTimeout() {
			return timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

	}

}
