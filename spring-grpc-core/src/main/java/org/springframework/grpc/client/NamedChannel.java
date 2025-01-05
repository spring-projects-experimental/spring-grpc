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

import java.time.Duration;
import java.util.function.Consumer;

import org.springframework.util.unit.DataSize;

import io.grpc.ManagedChannel;

/**
 * Represents the configuration for a {@link ManagedChannel gRPC channel}.
 *
 * @author Chris Bono
 */
public class NamedChannel {

	/**
	 * The target address uri to connect to.
	 */
	private String address = "static://localhost:9090";

	public String getAddress() {
		return this.address;
	}

	public void setAddress(final String address) {
		this.address = address;
	}

	// --------------------------------------------------
	// defaultLoadBalancingPolicy
	// --------------------------------------------------

	/**
	 * The default load balancing policy the channel should use.
	 */
	private String defaultLoadBalancingPolicy = "round_robin";

	public String getDefaultLoadBalancingPolicy() {
		return this.defaultLoadBalancingPolicy;
	}

	public void setDefaultLoadBalancingPolicy(final String defaultLoadBalancingPolicy) {
		this.defaultLoadBalancingPolicy = defaultLoadBalancingPolicy;
	}

	// --------------------------------------------------

	private final Health health = new Health();

	public Health getHealth() {
		return this.health;
	}

	/**
	 * The negotiation type for the channel.
	 */
	private NegotiationType negotiationType = NegotiationType.PLAINTEXT;

	public NegotiationType getNegotiationType() {
		return this.negotiationType;
	}

	public void setNegotiationType(NegotiationType negotiationType) {
		this.negotiationType = negotiationType;
	}

	// --------------------------------------------------
	// KeepAlive
	// --------------------------------------------------

	/**
	 * Whether keep alive is enabled on the channel.
	 */
	private boolean enableKeepAlive = false;

	public boolean isEnableKeepAlive() {
		return this.enableKeepAlive;
	}

	public void setEnableKeepAlive(boolean enableKeepAlive) {
		this.enableKeepAlive = enableKeepAlive;
	}

	// --------------------------------------------------

	/**
	 * The duration without ongoing RPCs before going to idle mode.
	 */
	private Duration idleTimeout = Duration.ofSeconds(20);

	public Duration getIdleTimeout() {
		return this.idleTimeout;
	}

	public void setIdleTimeout(Duration idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	// --------------------------------------------------

	/**
	 * The delay before sending a keepAlive. Note that shorter intervals increase the
	 * network burden for the server and this value can not be lower than
	 * 'permitKeepAliveTime' on the server.
	 */
	private Duration keepAliveTime = Duration.ofMinutes(5);

	public Duration getKeepAliveTime() {
		return this.keepAliveTime;
	}

	public void setKeepAliveTime(Duration keepAliveTime) {
		this.keepAliveTime = keepAliveTime;
	}

	// --------------------------------------------------

	/**
	 * The default timeout for a keepAlives ping request.
	 */
	private Duration keepAliveTimeout = Duration.ofSeconds(20);

	public Duration getKeepAliveTimeout() {
		return this.keepAliveTimeout;
	}

	public void setKeepAliveTimeout(Duration keepAliveTimeout) {
		this.keepAliveTimeout = keepAliveTimeout;
	}

	// --------------------------------------------------

	/**
	 * Whether a keepAlive will be performed when there are no outstanding RPC on a
	 * connection.
	 */
	private boolean keepAliveWithoutCalls = false;

	public boolean isKeepAliveWithoutCalls() {
		return this.keepAliveWithoutCalls;
	}

	public void setKeepAliveWithoutCalls(boolean keepAliveWithoutCalls) {
		this.keepAliveWithoutCalls = keepAliveWithoutCalls;
	}

	// --------------------------------------------------
	// Message Transfer
	// --------------------------------------------------

	/**
	 * Maximum message size allowed to be received by the channel (default 4MiB). Set to
	 * '-1' to use the highest possible limit (not recommended).
	 */
	private DataSize maxInboundMessageSize = DataSize.ofBytes(4194304);

	/**
	 * Maximum metadata size allowed to be received by the channel (default 8KiB). Set to
	 * '-1' to use the highest possible limit (not recommended).
	 */
	private DataSize maxInboundMetadataSize = DataSize.ofBytes(8192);

	public DataSize getMaxInboundMessageSize() {
		return this.maxInboundMessageSize;
	}

	public void setMaxInboundMessageSize(final DataSize maxInboundMessageSize) {
		this.setMaxInboundSize(maxInboundMessageSize, (s) -> this.maxInboundMessageSize = s, "maxInboundMesssageSize");
	}

	public DataSize getMaxInboundMetadataSize() {
		return this.maxInboundMetadataSize;
	}

	public void setMaxInboundMetadataSize(DataSize maxInboundMetadataSize) {
		this.setMaxInboundSize(maxInboundMetadataSize, (s) -> this.maxInboundMetadataSize = s,
				"maxInboundMetadataSize");
	}

	private void setMaxInboundSize(DataSize maxSize, Consumer<DataSize> setter, String propertyName) {
		if (maxSize != null && maxSize.toBytes() >= 0) {
			setter.accept(maxSize);
		}
		else if (maxSize != null && maxSize.toBytes() == -1) {
			setter.accept(DataSize.ofBytes(Integer.MAX_VALUE));
		}
		else {
			throw new IllegalArgumentException("Unsupported %s: %s".formatted(propertyName, maxSize));
		}
	}

	// --------------------------------------------------

	/**
	 * The custom User-Agent for the channel.
	 */
	private String userAgent = null;

	public String getUserAgent() {
		return this.userAgent;
	}

	public void setUserAgent(final String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * Provide a copy of the channel instance.
	 * @return a copy of the channel instance.
	 */
	public NamedChannel copy() {
		NamedChannel copy = new NamedChannel();
		copy.address = this.address;
		copy.defaultLoadBalancingPolicy = this.defaultLoadBalancingPolicy;
		copy.negotiationType = this.negotiationType;
		copy.enableKeepAlive = this.enableKeepAlive;
		copy.idleTimeout = this.idleTimeout;
		copy.keepAliveTime = this.keepAliveTime;
		copy.keepAliveTimeout = this.keepAliveTimeout;
		copy.keepAliveWithoutCalls = this.keepAliveWithoutCalls;
		copy.maxInboundMessageSize = this.maxInboundMessageSize;
		copy.maxInboundMetadataSize = this.maxInboundMetadataSize;
		copy.userAgent = this.userAgent;
		copy.health.copyValuesFrom(this.getHealth());
		copy.ssl.copyValuesFrom(this.getSsl());
		return copy;
	}

	// --------------------------------------------------

	/**
	 * Flag to say that strict SSL checks are not enabled (so the remote certificate could
	 * be anonymous).
	 */
	private boolean secure = true;

	public boolean isSecure() {
		return this.secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	// --------------------------------------------------

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

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getBundle() {
			return this.bundle;
		}

		public void setBundle(String bundle) {
			this.bundle = bundle;
		}

		/**
		 * Copies the values from another instance.
		 * @param other instance to copy values from
		 */
		public void copyValuesFrom(Ssl other) {
			this.enabled = other.enabled;
			this.bundle = other.bundle;
		}

	}

	public static class Health {

		/**
		 * Whether to enable client-side health check for the channel.
		 */
		private boolean enabled = false;

		/**
		 * Name of the service to check health on.
		 */
		private String serviceName;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getServiceName() {
			return this.serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		/**
		 * Copies the values from another instance.
		 * @param other instance to copy values from
		 */
		public void copyValuesFrom(Health other) {
			this.enabled = other.enabled;
			this.serviceName = other.serviceName;
		}

	}

}
