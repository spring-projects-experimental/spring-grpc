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

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ChannelCredentialsProvider;

import io.grpc.netty.NettyChannelBuilder;

public class GrpcChannelFactoryConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class)
	@ConditionalOnMissingBean(ChannelCredentialsProvider.class)
	public static class ShadedNettyChannelFactoryConfiguration {

		@Bean
		public ChannelCredentialsProvider channelCredentialsProvider(GrpcClientProperties channels,
				SslBundles bundles) {
			return new ShadedNettyChannelCredentialsProvider(bundles, channels);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(NettyChannelBuilder.class)
	@ConditionalOnMissingBean(ChannelCredentialsProvider.class)
	public static class NettyChannelFactoryConfiguration {

		@Bean
		public ChannelCredentialsProvider channelCredentialsProvider(GrpcClientProperties channels,
				SslBundles bundles) {
			return new NettyChannelCredentialsProvider(bundles, channels);
		}

	}

}
