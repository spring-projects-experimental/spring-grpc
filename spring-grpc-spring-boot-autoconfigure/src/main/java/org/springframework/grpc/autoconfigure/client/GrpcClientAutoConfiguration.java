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
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.DefaultGrpcChannelFactory;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.VirtualTargets;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GrpcClientProperties.class)
@Import({ GrpcCodecConfiguration.class, ClientInterceptorsConfiguration.class })
public class GrpcClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	DefaultGrpcChannelFactory defaultGrpcChannelFactory(List<GrpcChannelBuilderCustomizer> customizers,
			ClientInterceptorsConfigurer interceptorsConfigurer, ChannelCredentialsProvider credentials,
			GrpcClientProperties channels, SslBundles ignored) {
		DefaultGrpcChannelFactory factory = new DefaultGrpcChannelFactory(customizers, interceptorsConfigurer);
		factory.setCredentialsProvider(credentials);
		factory.setVirtualTargets(new NamedChannelVirtualTargets(channels));
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(ChannelCredentialsProvider.class)
	NamedChannelCredentialsProvider channelCredentialsProvider(GrpcClientProperties channels, SslBundles bundles) {
		return new NamedChannelCredentialsProvider(bundles, channels);
	}

	@Bean
	GrpcChannelBuilderCustomizer clientPropertiesChannelCustomizer(GrpcClientProperties properties) {
		return new ClientPropertiesChannelBuilderCustomizer(properties);
	}

	@ConditionalOnBean(CompressorRegistry.class)
	@Bean
	GrpcChannelBuilderCustomizer compressionClientCustomizer(CompressorRegistry registry) {
		return (name, builder) -> builder.compressorRegistry(registry);
	}

	@ConditionalOnBean(DecompressorRegistry.class)
	@Bean
	GrpcChannelBuilderCustomizer decompressionClientCustomizer(DecompressorRegistry registry) {
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
			return this.channels.getTarget(channel.getAddress());
		}

	}

}
