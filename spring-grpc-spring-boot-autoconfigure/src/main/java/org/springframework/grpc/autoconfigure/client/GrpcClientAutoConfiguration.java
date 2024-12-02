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
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.autoconfigure.common.codec.GrpcCodecConfiguration;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.DefaultGrpcChannelFactory;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.NamedChannelRegistry;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GrpcClientProperties.class)
@Import(GrpcCodecConfiguration.class)
public class GrpcClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ClientInterceptorsConfigurer clientInterceptorsConfigurer(ApplicationContext applicationContext) {
		return new ClientInterceptorsConfigurer(applicationContext);
	}

	@Bean
	NamedChannelRegistry namedChannelRegistry(GrpcClientProperties properties) {
		org.springframework.grpc.client.NamedChannel defaultChannel = mapDefaultChannelFromProperties();
		Map<String, org.springframework.grpc.client.NamedChannel> configuredChannels = mapConfiguredChannelsFromProperties();
		return new NamedChannelRegistry(defaultChannel, configuredChannels);
	}

	@Bean
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	public DefaultGrpcChannelFactory defaultGrpcChannelFactory(List<GrpcChannelBuilderCustomizer> customizers,
			NamedChannelRegistry namedChannelRegistry,
			ClientInterceptorsConfigurer interceptorsConfigurer, ChannelCredentialsProvider credentials,
			SslBundles ignored) {
		DefaultGrpcChannelFactory factory = new DefaultGrpcChannelFactory(customizers, interceptorsConfigurer);
		factory.setCredentialsProvider(credentials);
		factory.setVirtualTargets(namedChannelRegistry);
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(ChannelCredentialsProvider.class)
	public NamedChannelCredentialsProvider channelCredentialsProvider(GrpcClientProperties channels,
			SslBundles bundles) {
		return new NamedChannelCredentialsProvider(bundles, channels);
	}

	@Bean
	public GrpcChannelBuilderCustomizer clientPropertiesChannelCustomizer(GrpcClientProperties properties) {
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

}
