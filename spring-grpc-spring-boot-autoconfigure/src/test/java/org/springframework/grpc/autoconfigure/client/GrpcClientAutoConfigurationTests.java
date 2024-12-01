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

package org.springframework.grpc.autoconfigure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.autoconfigure.client.GrpcClientAutoConfiguration.NamedChannelVirtualTargets;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.DefaultGrpcChannelFactory;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.GrpcChannelFactory;

import io.grpc.Codec;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannelBuilder;

/**
 * Tests for {@link GrpcClientAutoConfiguration}.
 *
 * @author Chris Bono
 */
class GrpcClientAutoConfigurationTests {

	private ApplicationContextRunner contextRunner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcClientAutoConfiguration.class, SslAutoConfiguration.class));
	}

	@Test
	void whenHasUserDefinedChannelFactoryDoesNotAutoConfigureBean() {
		GrpcChannelFactory customChannelFactory = mock(GrpcChannelFactory.class);
		this.contextRunner()
			.withBean("customChannelFactory", GrpcChannelFactory.class, () -> customChannelFactory)
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class).isSameAs(customChannelFactory));
	}

	@Test
	void channelFactoryAutoConfiguredAsExpected() {
		this.contextRunner()
			.run((context) -> assertThat(context).getBean(DefaultGrpcChannelFactory.class)
				.hasFieldOrPropertyWithValue("credentials", context.getBean(NamedChannelCredentialsProvider.class))
				.extracting("targets")
				.isInstanceOf(NamedChannelVirtualTargets.class));
	}

	@Test
	void whenHasUserDefinedCredentialsProviderDoesNotAutoConfigureBean() {
		ChannelCredentialsProvider customCredentialsProvider = mock(ChannelCredentialsProvider.class);
		this.contextRunner()
			.withBean("customCredentialsProvider", ChannelCredentialsProvider.class, () -> customCredentialsProvider)
			.run((context) -> assertThat(context).getBean(ChannelCredentialsProvider.class)
				.isSameAs(customCredentialsProvider));
	}

	@Test
	void credentialsProviderAutoConfiguredAsExpected() {
		this.contextRunner()
			.run((context) -> assertThat(context).getBean(NamedChannelCredentialsProvider.class)
				.hasFieldOrPropertyWithValue("channels", context.getBean(GrpcClientProperties.class))
				.extracting("bundles")
				.isInstanceOf(SslBundles.class));
	}

	@Test
	void baseChannelCustomizerAutoConfiguredWithHealthAsExpected() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.client.channels.test.health.enabled=true",
					"spring.grpc.client.channels.test.health.service-name=my-service")
			.run((context) -> {
				assertThat(context).getBean("baseGrpcChannelBuilderCustomizer", GrpcChannelBuilderCustomizer.class)
					.isNotNull();
				var customizer = context.getBean("baseGrpcChannelBuilderCustomizer",
						GrpcChannelBuilderCustomizer.class);
				ManagedChannelBuilder<?> builder = Mockito.mock();
				customizer.customize("test", builder);
				Map<String, ?> healthCheckConfig = Map.of("healthCheckConfig", Map.of("serviceName", "my-service"));
				verify(builder).defaultServiceConfig(healthCheckConfig);
			});
	}

	@Test
	void baseChannelCustomizerAutoConfiguredWithoutHealthAsExpected() {
		this.contextRunner().run((context) -> {
			assertThat(context).getBean("baseGrpcChannelBuilderCustomizer", GrpcChannelBuilderCustomizer.class)
				.isNotNull();
			var customizer = context.getBean("baseGrpcChannelBuilderCustomizer", GrpcChannelBuilderCustomizer.class);
			ManagedChannelBuilder<?> builder = Mockito.mock();
			customizer.customize("test", builder);
			verify(builder, never()).defaultServiceConfig(anyMap());
		});
	}

	@Test
	void whenNoCompressorRegistryAutoConfigurationIsSkipped() {
		// Codec class guards the imported GrpcCodecConfiguration which provides the
		// registry
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(Codec.class))
			.run((context) -> assertThat(context)
				.getBean("compressionClientCustomizer", GrpcChannelBuilderCustomizer.class)
				.isNull());
	}

	@Test
	void compressionCustomizerAutoConfiguredAsExpected() {
		this.contextRunner().run((context) -> {
			assertThat(context).getBean("compressionClientCustomizer", GrpcChannelBuilderCustomizer.class).isNotNull();
			var customizer = context.getBean("compressionClientCustomizer", GrpcChannelBuilderCustomizer.class);
			var compressorRegistry = context.getBean(CompressorRegistry.class);
			ManagedChannelBuilder<?> builder = Mockito.mock();
			customizer.customize("testChannel", builder);
			verify(builder).compressorRegistry(compressorRegistry);
		});
	}

	@Test
	void whenNoDecompressorRegistryAutoConfigurationIsSkipped() {
		// Codec class guards the imported GrpcCodecConfiguration which provides the
		// registry
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(Codec.class))
			.run((context) -> assertThat(context)
				.getBean("decompressionClientCustomizer", GrpcChannelBuilderCustomizer.class)
				.isNull());
	}

	@Test
	void decompressionCustomizerAutoConfiguredAsExpected() {
		this.contextRunner().run((context) -> {
			assertThat(context).getBean("decompressionClientCustomizer", GrpcChannelBuilderCustomizer.class)
				.isNotNull();
			var customizer = context.getBean("decompressionClientCustomizer", GrpcChannelBuilderCustomizer.class);
			var decompressorRegistry = context.getBean(DecompressorRegistry.class);
			ManagedChannelBuilder<?> builder = Mockito.mock();
			customizer.customize("testChannel", builder);
			verify(builder).decompressorRegistry(decompressorRegistry);
		});
	}

}
