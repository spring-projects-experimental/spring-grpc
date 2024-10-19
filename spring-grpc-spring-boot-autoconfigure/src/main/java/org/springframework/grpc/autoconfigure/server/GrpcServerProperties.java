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
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.grpc.internal.GrpcUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

@ConfigurationProperties(prefix = "spring.grpc.server")
public class GrpcServerProperties {

	/**
	 * Server should listen to any IPv4 and IPv6 address.
	 */
	public static final String ANY_IP_ADDRESS = "*";

	/**
	 * Server address to bind to. The default is any IP address ('*').
	 */
	private String host = ANY_IP_ADDRESS;

	/**
	 * Server port to listen on. When the value is 0, a random available port is selected.
	 * The default is 9090.
	 */
	private int port = GrpcUtils.DEFAULT_PORT;

	/**
	 * Maximum time to wait for the server to gracefully shutdown. When the value is
	 * negative, the server waits forever. When the value is 0, the server will force
	 * shutdown immediately. The default is 30 seconds.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration shutdownGracePeriod = Duration.ofSeconds(30);

	/**
	 * Maximum message size allowed to be received by the server (default 4MiB).
	 */
	@DataSizeUnit(DataUnit.BYTES)
	private DataSize maxInboundMessageSize = DataSize.ofBytes(4194304);

	/**
	 * Maximum metadata size allowed to be received by the server (default 8KiB).
	 */
	@DataSizeUnit(DataUnit.BYTES)
	private DataSize maxInboundMetadataSize = DataSize.ofBytes(8192);

	private final KeepAlive keepAlive = new KeepAlive();

	/**
	 * The address to bind to. could be a host:port combination or a pseudo URL like
	 * static://host:port. Can not be set if host or port are set independently.
	 */
	private String address;

	public String getAddress() {
		return this.address == null ? this.host + ":" + this.port : this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		if (this.address != null) {
			throw new IllegalStateException("Cannot set host when address is already set");
		}
		this.host = host;
	}

	public int getPort() {
		if (this.address != null) {
			return GrpcUtils.getPort(this.address);
		}
		return this.port;
	}

	public void setPort(int port) {
		if (this.address != null) {
			throw new IllegalStateException("Cannot set port when address is already set");
		}
		this.port = port;
	}

	public Duration getShutdownGracePeriod() {
		return this.shutdownGracePeriod;
	}

	public void setShutdownGracePeriod(Duration shutdownGracePeriod) {
		this.shutdownGracePeriod = shutdownGracePeriod;
	}

	public DataSize getMaxInboundMessageSize() {
		return this.maxInboundMessageSize;
	}

	public void setMaxInboundMessageSize(DataSize maxInboundMessageSize) {
		this.maxInboundMessageSize = maxInboundMessageSize;
	}

	public DataSize getMaxInboundMetadataSize() {
		return this.maxInboundMetadataSize;
	}

	public void setMaxInboundMetadataSize(DataSize maxInboundMetadataSize) {
		this.maxInboundMetadataSize = maxInboundMetadataSize;
	}

	public KeepAlive getKeepAlive() {
		return this.keepAlive;
	}

	public static class KeepAlive {

		/**
		 * Duration without read activity before sending a keep alive ping (default 2h).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration time = Duration.ofHours(2);

		/**
		 * Maximum time to wait for read activity after sending a keep alive ping. If
		 * sender does not receive an acknowledgment within this time, it will close the
		 * connection (default 20s).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration timeout = Duration.ofSeconds(20);

		/**
		 * Maximum time a connection can remain idle before being gracefully terminated
		 * (default infinite).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration maxIdle = null;

		/**
		 * Maximum time a connection may exist before being gracefully terminated (default
		 * infinite).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration maxAge = null;

		/**
		 * Maximum time for graceful connection termination (default infinite).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration maxAgeGrace = null;

		/**
		 * Maximum keep-alive time clients are permitted to configure (default 5m).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration permitTime = Duration.ofMinutes(5);

		/**
		 * Whether clients are permitted to send keep alive pings when there are no
		 * outstanding RPCs on the connection (default false).
		 */
		private boolean permitWithoutCalls = false;

		public Duration getTime() {
			return this.time;
		}

		public void setTime(Duration time) {
			this.time = time;
		}

		public Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		public Duration getMaxIdle() {
			return this.maxIdle;
		}

		public void setMaxIdle(Duration maxIdle) {
			this.maxIdle = maxIdle;
		}

		public Duration getMaxAge() {
			return this.maxAge;
		}

		public void setMaxAge(Duration maxAge) {
			this.maxAge = maxAge;
		}

		public Duration getMaxAgeGrace() {
			return this.maxAgeGrace;
		}

		public void setMaxAgeGrace(Duration maxAgeGrace) {
			this.maxAgeGrace = maxAgeGrace;
		}

		public Duration getPermitTime() {
			return this.permitTime;
		}

		public void setPermitTime(Duration permitTime) {
			this.permitTime = permitTime;
		}

		public boolean isPermitWithoutCalls() {
			return this.permitWithoutCalls;
		}

		public void setPermitWithoutCalls(boolean permitWithoutCalls) {
			this.permitWithoutCalls = permitWithoutCalls;
		}

	}

	private final Ssl ssl = new Ssl();

	public Ssl getSsl() {
		return this.ssl;
	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if "bundle" is provided
		 * unless specified otherwise.
		 */
		private Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private String bundle;

		public boolean isEnabled() {
			return (this.enabled != null) ? this.enabled : this.bundle != null;
		}

		public void copyDefaultsFrom(Ssl config) {
			if (this.enabled == null) {
				this.enabled = config.enabled;
			}
			if (this.bundle == null) {
				this.bundle = config.bundle;
			}

		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getBundle() {
			return this.bundle;
		}

		public void setBundle(String bundle) {
			this.bundle = bundle;
		}

	}

}
