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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.autoconfigure.client.GrpcClientProperties.NamedChannel;
import org.springframework.grpc.autoconfigure.common.codec.GrpcCodecConfiguration;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.DefaultGrpcChannelFactory;
import org.springframework.grpc.client.GrpcChannelConfigurer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.VirtualTargets;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GrpcClientProperties.class)
@Import(GrpcCodecConfiguration.class)
public class GrpcClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	public DefaultGrpcChannelFactory defaultGrpcChannelFactory(final List<GrpcChannelConfigurer> configurers,
			ChannelCredentialsProvider credentials, GrpcClientProperties channels, SslBundles bundles) {
		DefaultGrpcChannelFactory factory = new DefaultGrpcChannelFactory(configurers);
		factory.setCredentialsProvider(credentials);
		factory.setVirtualTargets(new NamedChannelVirtualTargets(channels));
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(ChannelCredentialsProvider.class)
	public ChannelCredentialsProvider channelCredentialsProvider(GrpcClientProperties channels, SslBundles bundles) {
		return new NamedChannelCredentialsProvider(bundles, channels);
	}

	@Bean
	public GrpcChannelConfigurer sslGrpcChannelConfigurer(GrpcClientProperties channels) {
		return (authority, builder) -> {
			for (String name : channels.getChannels().keySet()) {
				if (authority.equals(name)) {
					NamedChannel channel = channels.getChannels().get(name);
					if (channel.getUserAgent() != null) {
						builder.userAgent(channel.getUserAgent());
					}
					if (channel.getDefaultLoadBalancingPolicy() != null) {
						builder.defaultLoadBalancingPolicy(channel.getDefaultLoadBalancingPolicy());
					}
					if (channel.getMaxInboundMessageSize() != null) {
						builder.maxInboundMessageSize((int) channel.getMaxInboundMessageSize().toBytes());
					}
					if (channel.getMaxInboundMetadataSize() != null) {
						builder.maxInboundMetadataSize((int) channel.getMaxInboundMetadataSize().toBytes());
					}
					if (channel.getKeepAliveTime() != null) {
						builder.keepAliveTime(channel.getKeepAliveTime().toNanos(), TimeUnit.NANOSECONDS);
					}
					if (channel.getKeepAliveTimeout() != null) {
						builder.keepAliveTimeout(channel.getKeepAliveTimeout().toNanos(), TimeUnit.NANOSECONDS);
					}
					builder.keepAliveWithoutCalls(channel.isKeepAliveWithoutCalls());
					if (channel.getIdleTimeout() != null) {
						builder.idleTimeout(channel.getIdleTimeout().toNanos(), TimeUnit.NANOSECONDS);
					}
				}
			}
		};
	}

	@ConditionalOnBean(CompressorRegistry.class)
	@Bean
	GrpcChannelConfigurer compressionClientConfigurer(CompressorRegistry registry) {
		return (name, builder) -> builder.compressorRegistry(registry);
	}

	@ConditionalOnBean(DecompressorRegistry.class)
	@Bean
	GrpcChannelConfigurer decompressionClientConfigurer(DecompressorRegistry registry) {
		return (name, builder) -> builder.decompressorRegistry(registry);
	}

	static class NamedChannelVirtualTargets implements VirtualTargets {

		private final GrpcClientProperties channels;

		NamedChannelVirtualTargets(GrpcClientProperties channels) {
			this.channels = channels;
		}

		@Override
		public String getTarget(String authority) {
			NamedChannel channel = this.channels.getChannel(authority);
			return channels.getTarget(channel.getAddress());
		}

	}

}
