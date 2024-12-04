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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.grpc.client.NegotiationType;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyChannelBuilder;

@ConfigurationProperties(prefix = "spring.grpc.client")
public class GrpcClientProperties implements EnvironmentAware {

	private NamedChannel defaultChannel = new NamedChannel();

	private Map<String, NamedChannel> channels = new HashMap<>();

	private Environment environment = new StandardEnvironment();

	GrpcClientProperties() {
		this.defaultChannel.setAddress("static://localhost:9090");
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public Map<String, NamedChannel> getChannels() {
		return channels;
	}

	/**
	 * Gets the default {@link NamedChannel} configured for the GRPC client.
	 * @return the default {@link NamedChannel}
	 */
	public NamedChannel getDefaultChannel() {
		return defaultChannel;
	}

	public NamedChannel getChannel(String name) {
		if ("default".equals(name)) {
			return defaultChannel;
		}
		return channels.computeIfAbsent(name, authority -> {
			NamedChannel channel = new NamedChannel();
			channel.copyDefaultsFrom(defaultChannel);
			if (!authority.contains(":/") && !authority.startsWith("unix:")) {
				authority = "static://" + authority;
			}
			channel.setAddress(authority);
			return channel;
		});
	}

	public String getTarget(String authority) {
		NamedChannel channel = getChannel(authority);
		String address = channel.getAddress();
		if (address.startsWith("static:") || address.startsWith("tcp:")) {
			address = address.substring(address.indexOf(":") + 1).replaceFirst("/*", "");
		}
		address = environment.resolvePlaceholders(address);
		return address.toString();
	}

	public static class NamedChannel {

		private String address = null;

		/**
		 * Gets the target address uri.
		 * @return The address to connect to or null
		 * @see #setAddress(String)
		 */
		public String getAddress() {
			return this.address;
		}

		/**
		 * Set the address uri for the channel. If nothing is configured then the name of
		 * the client will be used along with the default scheme. We recommend explicitly
		 * configuring the scheme used for the address resolutions such as {@code dns:/}.
		 * @param address The address to use for the channel or null to default to the
		 * fallback.
		 *
		 * @see #setAddress(String)
		 */
		public void setAddress(final String address) {
			this.address = address;
		}

		// --------------------------------------------------
		// defaultLoadBalancingPolicy
		// --------------------------------------------------

		private String defaultLoadBalancingPolicy;

		private static final String DEFAULT_DEFAULT_LOAD_BALANCING_POLICY = "round_robin";

		/**
		 * Gets the default load balancing policy this channel should use.
		 * @return The default load balancing policy.
		 * @see ManagedChannelBuilder#defaultLoadBalancingPolicy(String)
		 */
		public String getDefaultLoadBalancingPolicy() {
			return this.defaultLoadBalancingPolicy == null ? DEFAULT_DEFAULT_LOAD_BALANCING_POLICY
					: this.defaultLoadBalancingPolicy;
		}

		/**
		 * Sets the default load balancing policy for this channel. This config might be
		 * overwritten by the service config received from the target address. The names
		 * have to be resolvable from the {@link LoadBalancerRegistry}. By default this
		 * the {@code round_robin} policy. Please note that this policy is different from
		 * the normal grpc-java default policy {@code pick_first}.
		 * @param defaultLoadBalancingPolicy The default load balancing policy to use or
		 * null to use the fallback.
		 */
		public void setDefaultLoadBalancingPolicy(final String defaultLoadBalancingPolicy) {
			this.defaultLoadBalancingPolicy = defaultLoadBalancingPolicy;
		}

		// --------------------------------------------------

		private final Health health = new Health();

		public Health getHealth() {
			return this.health;
		}

		/**
		 * The negotiation type for the channel. Default is
		 * {@link NegotiationType#PLAINTEXT}.
		 */
		private NegotiationType negotiationType = NegotiationType.PLAINTEXT;

		public NegotiationType getNegotiationType() {
			return negotiationType;
		}

		public void setNegotiationType(NegotiationType negotiationType) {
			this.negotiationType = negotiationType;
		}

		// --------------------------------------------------
		// KeepAlive
		// --------------------------------------------------

		private Boolean enableKeepAlive;

		private static final boolean DEFAULT_ENABLE_KEEP_ALIVE = false;

		/**
		 * Gets whether keepAlive is enabled.
		 * @return True, if keep alive should be enabled. False otherwise.
		 *
		 * @see #setEnableKeepAlive(Boolean)
		 */
		public boolean isEnableKeepAlive() {
			return this.enableKeepAlive == null ? DEFAULT_ENABLE_KEEP_ALIVE : this.enableKeepAlive;
		}

		/**
		 * Sets whether keepAlive should be enabled. Defaults to false.
		 * @param enableKeepAlive True, to enable. False, to disable. Null, to use the
		 * fallback.
		 */
		public void setEnableKeepAlive(final Boolean enableKeepAlive) {
			this.enableKeepAlive = enableKeepAlive;
		}

		// --------------------------------------------------

		@DurationUnit(ChronoUnit.SECONDS)
		private Duration idleTimeout;

		private static final Duration DEFAULT_IDLE_TIMEOUT = Duration.of(20, ChronoUnit.SECONDS);

		/**
		 * Gets the idle timeout.
		 * @return The idle tomeout.
		 *
		 * @see #setIdleTimeout(Duration)
		 */
		public Duration getIdleTimeout() {
			return this.idleTimeout == null ? DEFAULT_IDLE_TIMEOUT : this.idleTimeout;
		}

		/**
		 * The idle timeout.
		 * @param idleTimeout The new idle timeout, or null to use the fallback.
		 *
		 */
		public void setIdleTimeout(final Duration idleTimeout) {
			this.idleTimeout = idleTimeout;
		}

		// --------------------------------------------------

		@DurationUnit(ChronoUnit.SECONDS)
		private Duration keepAliveTime;

		private static final Duration DEFAULT_KEEP_ALIVE_TIME = Duration.of(5, ChronoUnit.MINUTES);

		/**
		 * Gets the default delay before we send a keepAlive.
		 * @return The default delay before sending keepAlives.
		 *
		 * @see #setKeepAliveTime(Duration)
		 */
		public Duration getKeepAliveTime() {
			return this.keepAliveTime == null ? DEFAULT_KEEP_ALIVE_TIME : this.keepAliveTime;
		}

		/**
		 * The default delay before we send a keepAlives. Defaults to {@code 5min}.
		 * Default unit {@link ChronoUnit#SECONDS SECONDS}. Please note that shorter
		 * intervals increase the network burden for the server. Cannot be lower than
		 * permitKeepAliveTime on server (default 5min).
		 * @param keepAliveTime The new default delay before sending keepAlives, or null
		 * to use the fallback.
		 *
		 * @see #setEnableKeepAlive(Boolean)
		 * @see NettyChannelBuilder#keepAliveTime(long, TimeUnit)
		 */
		public void setKeepAliveTime(final Duration keepAliveTime) {
			this.keepAliveTime = keepAliveTime;
		}

		// --------------------------------------------------

		@DurationUnit(ChronoUnit.SECONDS)
		private Duration keepAliveTimeout;

		private static final Duration DEFAULT_KEEP_ALIVE_TIMEOUT = Duration.of(20, ChronoUnit.SECONDS);

		/**
		 * The default timeout for a keepAlives ping request.
		 * @return The default timeout for a keepAlives ping request.
		 *
		 * @see #setKeepAliveTimeout(Duration)
		 */
		public Duration getKeepAliveTimeout() {
			return this.keepAliveTimeout == null ? DEFAULT_KEEP_ALIVE_TIMEOUT : this.keepAliveTimeout;
		}

		/**
		 * The default timeout for a keepAlives ping request. Defaults to {@code 20s}.
		 * Default unit {@link ChronoUnit#SECONDS SECONDS}.
		 * @param keepAliveTimeout The default timeout for a keepAlives ping request.
		 *
		 * @see #setEnableKeepAlive(Boolean)
		 * @see NettyChannelBuilder#keepAliveTimeout(long, TimeUnit)
		 */
		public void setKeepAliveTimeout(final Duration keepAliveTimeout) {
			this.keepAliveTimeout = keepAliveTimeout;
		}

		// --------------------------------------------------

		private Boolean keepAliveWithoutCalls;

		private static final boolean DEFAULT_KEEP_ALIVE_WITHOUT_CALLS = false;

		/**
		 * Gets whether keepAlive will be performed when there are no outstanding RPC on a
		 * connection.
		 * @return True, if keepAlives should be performed even when there are no RPCs.
		 * False otherwise.
		 *
		 * @see #setKeepAliveWithoutCalls(Boolean)
		 */
		public boolean isKeepAliveWithoutCalls() {
			return this.keepAliveWithoutCalls == null ? DEFAULT_KEEP_ALIVE_WITHOUT_CALLS : this.keepAliveWithoutCalls;
		}

		/**
		 * Sets whether keepAlive will be performed when there are no outstanding RPC on a
		 * connection. Defaults to {@code false}.
		 * @param keepAliveWithoutCalls whether keepAlive will be performed when there are
		 * no outstanding RPC on a connection.
		 *
		 * @see #setEnableKeepAlive(Boolean)
		 * @see NettyChannelBuilder#keepAliveWithoutCalls(boolean)
		 */
		public void setKeepAliveWithoutCalls(final Boolean keepAliveWithoutCalls) {
			this.keepAliveWithoutCalls = keepAliveWithoutCalls;
		}

		// --------------------------------------------------
		// Message Transfer
		// --------------------------------------------------

		@DataSizeUnit(DataUnit.BYTES)
		private DataSize maxInboundMessageSize = null;

		/**
		 * Gets the maximum message size allowed to be received by the channel. If not set
		 * ({@code null}) then {@link GrpcUtil#DEFAULT_MAX_MESSAGE_SIZE gRPC's default}
		 * should be used. If set to {@code -1} then it will use the highest possible
		 * limit (not recommended).
		 * @return The maximum message size allowed or null if the default should be used.
		 *
		 * @see #setMaxInboundMessageSize(DataSize)
		 */
		public DataSize getMaxInboundMessageSize() {
			return this.maxInboundMessageSize;
		}

		/**
		 * Sets the maximum message size in bytes allowed to be received by the channel.
		 * If not set ({@code null}) then it will default to
		 * {@link GrpcUtil#DEFAULT_MAX_MESSAGE_SIZE gRPC's default}. If set to {@code -1}
		 * then it will use the highest possible limit (not recommended).
		 * @param maxInboundMessageSize The new maximum size in bytes allowed for incoming
		 * messages. {@code -1} for max possible. Null to use the gRPC's default.
		 *
		 * @see ManagedChannelBuilder#maxInboundMessageSize(int)
		 */
		public void setMaxInboundMessageSize(final DataSize maxInboundMessageSize) {
			if (maxInboundMessageSize == null || maxInboundMessageSize.toBytes() >= 0) {
				this.maxInboundMessageSize = maxInboundMessageSize;
			}
			else if (maxInboundMessageSize.toBytes() == -1) {
				this.maxInboundMessageSize = DataSize.ofBytes(Integer.MAX_VALUE);
			}
			else {
				throw new IllegalArgumentException("Unsupported maxInboundMessageSize: " + maxInboundMessageSize);
			}
		}

		@DataSizeUnit(DataUnit.BYTES)
		private DataSize maxInboundMetadataSize = null;

		/**
		 * Sets the maximum size of metadata in bytes allowed to be received. If not set
		 * ({@code null}) then it will default to gRPC's default. The default is
		 * implementation-dependent, but is not generally less than 8 KiB and may be
		 * unlimited. If set to {@code -1} then it will use the highest possible limit
		 * (not recommended). Integer.MAX_VALUE disables the enforcement.
		 * @return The maximum size of metadata in bytes allowed to be received or null if
		 * the default should be used.
		 *
		 * @see ManagedChannelBuilder#maxInboundMetadataSize(int) (int)
		 */
		public DataSize getMaxInboundMetadataSize() {
			return maxInboundMetadataSize;
		}

		/**
		 * Sets the maximum size of metadata in bytes allowed to be received. If not set
		 * ({@code null}) then it will default.The default is implementation-dependent,
		 * but is not generally less than 8 KiB and may be unlimited. If set to {@code -1}
		 * then it will use the highest possible limit (not recommended).
		 * Integer.MAX_VALUE disables the enforcement.
		 * @param maxInboundMetadataSize The new maximum size of metadata in bytes allowed
		 * to be received. {@code -1} for max possible. Null to use the gRPC's default.
		 *
		 * @see ManagedChannelBuilder#maxInboundMetadataSize(int) (int)
		 */
		public void setMaxInboundMetadataSize(DataSize maxInboundMetadataSize) {
			if (maxInboundMetadataSize == null || maxInboundMetadataSize.toBytes() >= 0) {
				this.maxInboundMetadataSize = maxInboundMetadataSize;
			}
			else if (maxInboundMetadataSize.toBytes() == -1) {
				this.maxInboundMetadataSize = DataSize.ofBytes(Integer.MAX_VALUE);
			}
			else {
				throw new IllegalArgumentException("Unsupported maxInboundMetadataSize: " + maxInboundMetadataSize);
			}
		}

		// --------------------------------------------------

		private String userAgent = null;

		/**
		 * Get custom User-Agent for the channel.
		 * @return custom User-Agent for the channel.
		 *
		 * @see #setUserAgent(String)
		 */
		public String getUserAgent() {
			return this.userAgent;
		}

		/**
		 * Sets custom User-Agent HTTP header.
		 * @param userAgent Custom User-Agent.
		 *
		 * @see ManagedChannelBuilder#userAgent(String)
		 */
		public void setUserAgent(final String userAgent) {
			this.userAgent = userAgent;
		}

		/**
		 * Copies the defaults from the given configuration. Values are considered
		 * "default" if they are null. Please note that the getters might return fallback
		 * values instead.
		 * @param config The config to copy the defaults from.
		 */
		public void copyDefaultsFrom(final NamedChannel config) {
			if (this == config) {
				return;
			}
			if (this.address == null) {
				this.address = config.address;
			}
			if (this.defaultLoadBalancingPolicy == null) {
				this.defaultLoadBalancingPolicy = config.defaultLoadBalancingPolicy;
			}
			if (this.enableKeepAlive == null) {
				this.enableKeepAlive = config.enableKeepAlive;
			}
			if (this.idleTimeout == null) {
				this.idleTimeout = config.idleTimeout;
			}
			if (this.keepAliveTime == null) {
				this.keepAliveTime = config.keepAliveTime;
			}
			if (this.keepAliveTimeout == null) {
				this.keepAliveTimeout = config.keepAliveTimeout;
			}
			if (this.keepAliveWithoutCalls == null) {
				this.keepAliveWithoutCalls = config.keepAliveWithoutCalls;
			}
			if (this.maxInboundMessageSize == null) {
				this.maxInboundMessageSize = config.maxInboundMessageSize;
			}
			if (this.maxInboundMetadataSize == null) {
				this.maxInboundMetadataSize = config.maxInboundMetadataSize;
			}
			if (this.userAgent == null) {
				this.userAgent = config.userAgent;
			}
			this.health.copyDefaultsFrom(config.health);
			this.ssl.copyDefaultsFrom(config.ssl);
		}

		// --------------------------------------------------

		/**
		 * Flag to say that strict SSL checks are not enabled (so the remote certificate
		 * could be anonymous).
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
			 * Whether to enable SSL support. Enabled automatically if "bundle" is
			 * provided unless specified otherwise.
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

		public static class Health {

			/**
			 * Whether to enable client-side health check for the channel.
			 */
			private Boolean enabled;

			/**
			 * Name of the service to check health on.
			 */
			private String serviceName;

			public boolean isEnabled() {
				return this.enabled != null ? this.enabled : false;
			}

			public void setEnabled(Boolean enabled) {
				this.enabled = enabled;
			}

			public String getServiceName() {
				return this.serviceName;
			}

			public void setServiceName(String serviceName) {
				this.serviceName = serviceName;
			}

			public void copyDefaultsFrom(Health config) {
				if (this.enabled == null) {
					this.enabled = config.enabled;
				}
				if (this.serviceName == null) {
					this.serviceName = config.serviceName;
				}
			}

		}

	}

	public SslBundle sslBundle(SslBundles bundles, String path) {
		NamedChannel channel = this.getChannel(path);
		if (!channel.getSsl().isEnabled()) {
			return null;
		}
		return bundles.getBundle(this.getChannel(path).getSsl().getBundle());
	}

}
