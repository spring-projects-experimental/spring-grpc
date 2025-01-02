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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.autoconfigure.client.GrpcClientProperties.NamedChannel;
import org.springframework.grpc.autoconfigure.common.codec.GrpcCodecConfiguration;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.VirtualTargets;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannelBuilder;

@AutoConfiguration
@EnableConfigurationProperties(GrpcClientProperties.class)
@Import({ GrpcCodecConfiguration.class, ClientInterceptorsConfiguration.class,
		GrpcChannelFactoryConfigurations.ShadedNettyChannelFactoryConfiguration.class,
		GrpcChannelFactoryConfigurations.NettyChannelFactoryConfiguration.class })
public class GrpcClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ChannelCredentialsProvider.class)
	NamedChannelCredentialsProvider channelCredentialsProvider(GrpcClientProperties channels, SslBundles bundles) {
		return new NamedChannelCredentialsProvider(bundles, channels);
	}

	@Bean
	<T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> clientPropertiesChannelCustomizer(
			GrpcClientProperties properties) {
		return new ClientPropertiesChannelBuilderCustomizer<>(properties);
	}

	@ConditionalOnBean(CompressorRegistry.class)
	@Bean
	<T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> compressionClientCustomizer(
			CompressorRegistry registry) {
		return (name, builder) -> builder.compressorRegistry(registry);
	}

	@ConditionalOnBean(DecompressorRegistry.class)
	@Bean
	<T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> decompressionClientCustomizer(
			DecompressorRegistry registry) {
		return (name, builder) -> builder.decompressorRegistry(registry);
	}

	@ConditionalOnMissingBean
	@Bean
	ChannelBuilderCustomizers channelBuilderCustomizers(ObjectProvider<GrpcChannelBuilderCustomizer<?>> customizers) {
		return new ChannelBuilderCustomizers(customizers.orderedStream().toList());
	}

	static class NamedChannelVirtualTargets implements VirtualTargets {

		private final GrpcClientProperties channels;

		NamedChannelVirtualTargets(GrpcClientProperties channels) {
			this.channels = channels;
		}

		@Override
		public String getTarget(String authority) {
			NamedChannel channel = this.channels.getChannel(authority);
			return this.channels.getTarget(channel);
		}

	}

}
