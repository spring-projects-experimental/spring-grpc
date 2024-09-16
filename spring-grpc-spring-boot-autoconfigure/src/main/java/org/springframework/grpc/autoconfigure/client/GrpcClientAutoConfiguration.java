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

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.autoconfigure.client.GrpcClientProperties.NamedChannel;
import org.springframework.grpc.client.DefaultGrpcChannelFactory;
import org.springframework.grpc.client.GrpcChannelConfigurer;
import org.springframework.grpc.client.GrpcChannelFactory;

import io.grpc.ManagedChannelBuilder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GrpcClientProperties.class)
public class GrpcClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	public DefaultGrpcChannelFactory defaultGrpcChannelFactory(final List<GrpcChannelConfigurer> configurers,
			GrpcClientProperties channels) {
		return new DefaultGrpcChannelFactory(configurers) {
			@Override
			public ManagedChannelBuilder<?> newChannel(String authority) {
				if (channels.getChannels().containsKey(authority)) {
					NamedChannel channel = channels.getChannels().get(authority);
					URI address = channel.getAddress();
					if (address.getScheme().equals("static") || address.getScheme().equals("tcp")) {
						return super.newChannel(address.getAuthority());
					}
					return super.newChannel(address.toString());
				}
				return super.newChannel(authority);
			}
		};
	}

	@Bean
	public GrpcChannelConfigurer sslGrpcChannelConfigurer(GrpcClientProperties channels, SslBundles bundles) {
		return (authority, builder) -> {
			for (String name : channels.getChannels().keySet()) {
				if (authority.equals(name)) {
					NamedChannel channel = channels.getChannels().get(name);
					if (channel.getSsl().isEnabled() && channel.getSsl().getBundle() != null) {
						SslBundle bundle = bundles.getBundle(channel.getSsl().getBundle());
						if (NettyChannelFactoryHelper.isAvailable()) {
							NettyChannelFactoryHelper.sslContext(builder, bundle);
						}
						else if (ShadedNettyChannelFactoryHelper.isAvailable()) {
							ShadedNettyChannelFactoryHelper.sslContext(builder, bundle);
						}
						else {
							throw new IllegalStateException("Netty is not available");
						}
					}
					else {
						// builder.usePlaintext();
					}
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

}
