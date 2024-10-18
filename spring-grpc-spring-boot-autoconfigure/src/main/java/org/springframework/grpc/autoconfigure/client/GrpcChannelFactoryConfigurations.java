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

import javax.net.ssl.SSLException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.autoconfigure.client.GrpcClientProperties.NamedChannel;
import org.springframework.grpc.client.DefaultGrpcChannelFactory;
import org.springframework.grpc.client.GrpcChannelConfigurer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.NegotiationType;
import org.springframework.grpc.client.NettyGrpcChannelFactory;
import org.springframework.grpc.client.ShadedNettyGrpcChannelFactory;
import org.springframework.grpc.client.VirtualTargets;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class GrpcChannelFactoryConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class)
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	public static class ShadedNettyChannelFactoryConfiguration {

		@Bean
		public DefaultGrpcChannelFactory defaultGrpcChannelFactory(final List<GrpcChannelConfigurer> configurers,
				GrpcClientProperties channels) {
			DefaultGrpcChannelFactory factory = new ShadedNettyGrpcChannelFactory(configurers);
			factory.setVirtualTargets(new NamedChannelVirtualTargets(channels));
			return factory;
		}

		@Bean
		public GrpcChannelConfigurer secureChannelConfigurer(GrpcClientProperties channels) {

			return (authority, input) -> {
				NamedChannel channel = channels.getChannel(authority);
				if (!authority.startsWith("unix:")
						&& input instanceof io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder builder) {
					builder.negotiationType(of(channel.getNegotiationType()));
					try {
						if (!channel.isSecure()) {
							builder.sslContext(io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts.forClient()
								.trustManager(
										io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)
								.build());
						}
					}
					catch (SSLException e) {
						throw new IllegalStateException("Failed to create SSL context", e);
					}
				}
			};

		}

		private static io.grpc.netty.shaded.io.grpc.netty.NegotiationType of(final NegotiationType negotiationType) {
			switch (negotiationType) {
				case PLAINTEXT:
					return io.grpc.netty.shaded.io.grpc.netty.NegotiationType.PLAINTEXT;
				case PLAINTEXT_UPGRADE:
					return io.grpc.netty.shaded.io.grpc.netty.NegotiationType.PLAINTEXT_UPGRADE;
				case TLS:
					return io.grpc.netty.shaded.io.grpc.netty.NegotiationType.TLS;
				default:
					throw new IllegalArgumentException("Unsupported NegotiationType: " + negotiationType);
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(NettyChannelBuilder.class)
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	public static class NettyChannelFactoryConfiguration {

		@Bean
		public DefaultGrpcChannelFactory defaultGrpcChannelFactory(final List<GrpcChannelConfigurer> configurers,
				GrpcClientProperties channels) {
			DefaultGrpcChannelFactory factory = new NettyGrpcChannelFactory(configurers);
			factory.setVirtualTargets(new NamedChannelVirtualTargets(channels));
			return factory;
		}

		@Bean
		public GrpcChannelConfigurer secureChannelConfigurer(GrpcClientProperties channels) {

			return (authority, input) -> {
				NamedChannel channel = channels.getChannel(authority);
				if (!authority.startsWith("unix:") && input instanceof NettyChannelBuilder builder) {
					builder.negotiationType(of(channel.getNegotiationType()));
					try {
						if (!channel.isSecure()) {
							builder.sslContext(GrpcSslContexts.forClient()
								.trustManager(InsecureTrustManagerFactory.INSTANCE)
								.build());
						}
					}
					catch (SSLException e) {
						throw new IllegalStateException("Failed to create SSL context", e);
					}
				}
			};

		}

		private static io.grpc.netty.NegotiationType of(final NegotiationType negotiationType) {
			switch (negotiationType) {
				case PLAINTEXT:
					return io.grpc.netty.NegotiationType.PLAINTEXT;
				case PLAINTEXT_UPGRADE:
					return io.grpc.netty.NegotiationType.PLAINTEXT_UPGRADE;
				case TLS:
					return io.grpc.netty.NegotiationType.TLS;
				default:
					throw new IllegalArgumentException("Unsupported NegotiationType: " + negotiationType);
			}
		}

	}

	static class NamedChannelVirtualTargets implements VirtualTargets {

		private final GrpcClientProperties channels;

		NamedChannelVirtualTargets(GrpcClientProperties channels) {
			this.channels = channels;
		}

		@Override
		public String getTarget(String authority) {
			NamedChannel channel = this.channels.getChannel(authority);
			String address = channels.getTarget(channel.getAddress());
			return address;
		}

	}

}
