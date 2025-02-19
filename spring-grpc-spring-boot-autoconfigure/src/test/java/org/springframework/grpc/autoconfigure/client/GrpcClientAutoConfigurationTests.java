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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.NettyGrpcChannelFactory;
import org.springframework.grpc.client.ShadedNettyGrpcChannelFactory;

import io.grpc.Codec;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;

/**
 * Tests for {@link GrpcClientAutoConfiguration}.
 *
 * @author Chris Bono
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class GrpcClientAutoConfigurationTests {

	private ApplicationContextRunner contextRunner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcClientAutoConfiguration.class, SslAutoConfiguration.class));
	}

	@Test
	void whenGrpcStubNotOnClasspathThenAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(AbstractStub.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.client.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner().run((context) -> assertThat(context).hasSingleBean(GrpcClientAutoConfiguration.class));
	}

	@Test
	void whenClientEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.client.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcClientAutoConfiguration.class));
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
				.hasFieldOrPropertyWithValue("properties", context.getBean(GrpcClientProperties.class))
				.extracting("bundles")
				.isInstanceOf(SslBundles.class));
	}

	@Test
	void clientPropertiesAutoConfiguredResolvesPlaceholders() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.client.channels.c1.address=my-server-${channelName}:8888",
					"channelName=foo")
			.run((context) -> assertThat(context).getBean(GrpcClientProperties.class)
				.satisfies((properties) -> assertThat(properties.getTarget("c1")).isEqualTo("my-server-foo:8888")));
	}

	@Test
	void clientPropertiesChannelCustomizerAutoConfiguredWithHealthAsExpected() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.client.channels.test.health.enabled=true",
					"spring.grpc.client.channels.test.health.service-name=my-service")
			.run((context) -> {
				assertThat(context).getBean("clientPropertiesChannelCustomizer", GrpcChannelBuilderCustomizer.class)
					.isNotNull();
				var customizer = context.getBean("clientPropertiesChannelCustomizer",
						GrpcChannelBuilderCustomizer.class);
				ManagedChannelBuilder<?> builder = Mockito.mock();
				customizer.customize("test", builder);
				Map<String, ?> healthCheckConfig = Map.of("healthCheckConfig", Map.of("serviceName", "my-service"));
				verify(builder).defaultServiceConfig(healthCheckConfig);
			});
	}

	@Test
	void clientPropertiesChannelCustomizerAutoConfiguredWithoutHealthAsExpected() {
		this.contextRunner().run((context) -> {
			assertThat(context).getBean("clientPropertiesChannelCustomizer", GrpcChannelBuilderCustomizer.class)
				.isNotNull();
			var customizer = context.getBean("clientPropertiesChannelCustomizer", GrpcChannelBuilderCustomizer.class);
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

	@Test
	void whenHasUserDefinedChannelBuilderCustomizersDoesNotAutoConfigureBean() {
		ChannelBuilderCustomizers customCustomizers = mock(ChannelBuilderCustomizers.class);
		this.contextRunner()
			.withBean("customCustomizers", ChannelBuilderCustomizers.class, () -> customCustomizers)
			.run((context) -> assertThat(context).getBean(ChannelBuilderCustomizers.class).isSameAs(customCustomizers));
	}

	@Test
	void channelBuilderCustomizersAutoConfiguredAsExpected() {
		this.contextRunner()
			.withUserConfiguration(ChannelBuilderCustomizersConfig.class)
			.run((context) -> assertThat(context).getBean(ChannelBuilderCustomizers.class)
				.extracting("customizers", InstanceOfAssertFactories.list(GrpcChannelBuilderCustomizer.class))
				.contains(ChannelBuilderCustomizersConfig.CUSTOMIZER_BAR,
						ChannelBuilderCustomizersConfig.CUSTOMIZER_FOO));
	}

	@Test
	void whenHasUserDefinedChannelFactoryDoesNotAutoConfigureBean() {
		GrpcChannelFactory customChannelFactory = mock(GrpcChannelFactory.class);
		this.contextRunner()
			.withBean("customChannelFactory", GrpcChannelFactory.class, () -> customChannelFactory)
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class).isSameAs(customChannelFactory));
	}

	@Test
	void whenShadedAndNonShadedNettyOnClasspathShadedNettyFactoryIsAutoConfigured() {
		this.contextRunner()
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(ShadedNettyGrpcChannelFactory.class));
	}

	@Test
	void whenOnlyNonShadedNettyOnClasspathNonShadedNettyFactoryIsAutoConfigured() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class))
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(NettyGrpcChannelFactory.class));
	}

	@Test
	void shadedNettyChannelFactoryAutoConfiguredAsExpected() {
		channelFactoryAutoConfiguredAsExpected(this.contextRunner(), ShadedNettyGrpcChannelFactory.class);
	}

	@Test
	void nettyChannelFactoryAutoConfiguredAsExpected() {
		channelFactoryAutoConfiguredAsExpected(this.contextRunner()
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class)),
				NettyGrpcChannelFactory.class);
	}

	@Test
	void noChannelFactoryAutoConfiguredAsExpected() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(NettyChannelBuilder.class,
					io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcChannelFactory.class));
	}

	private void channelFactoryAutoConfiguredAsExpected(ApplicationContextRunner contextRunner,
			Class<?> expectedChannelFactoryType) {
		contextRunner.withPropertyValues("spring.grpc.server.port=0")
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(expectedChannelFactoryType)
				.hasFieldOrPropertyWithValue("credentials", context.getBean(NamedChannelCredentialsProvider.class))
				.extracting("targets")
				.isInstanceOf(GrpcClientProperties.class));
	}

	@Test
	void shadedNettyChannelFactoryAutoConfiguredWithCustomizers() {
		io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder builder = mock();
		channelFactoryAutoConfiguredWithCustomizers(this.contextRunner(), builder, ShadedNettyGrpcChannelFactory.class);
	}

	@Test
	void nettyChannelFactoryAutoConfiguredWithCustomizers() {
		NettyChannelBuilder builder = mock();
		channelFactoryAutoConfiguredWithCustomizers(this.contextRunner()
			.withClassLoader(new FilteredClassLoader(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class)),
				builder, NettyGrpcChannelFactory.class);
	}

	@SuppressWarnings("unchecked")
	private <T extends ManagedChannelBuilder<T>> void channelFactoryAutoConfiguredWithCustomizers(
			ApplicationContextRunner contextRunner, ManagedChannelBuilder<T> mockChannelBuilder,
			Class<?> expectedChannelFactoryType) {
		GrpcChannelBuilderCustomizer<T> customizer1 = (__, b) -> b.keepAliveTime(40L, TimeUnit.SECONDS);
		GrpcChannelBuilderCustomizer<T> customizer2 = (__, b) -> b.keepAliveTime(50L, TimeUnit.SECONDS);
		ChannelBuilderCustomizers customizers = new ChannelBuilderCustomizers(List.of(customizer1, customizer2));
		contextRunner.withPropertyValues("spring.grpc.server.port=0")
			.withBean("channelBuilderCustomizers", ChannelBuilderCustomizers.class, () -> customizers)
			.run((context) -> assertThat(context).getBean(GrpcChannelFactory.class)
				.isInstanceOf(expectedChannelFactoryType)
				.extracting("globalCustomizers", InstanceOfAssertFactories.list(GrpcChannelBuilderCustomizer.class))
				.satisfies((allCustomizers) -> {
					allCustomizers.forEach((c) -> c.customize("channel1", mockChannelBuilder));
					InOrder ordered = inOrder(mockChannelBuilder);
					ordered.verify(mockChannelBuilder).keepAliveTime(40L, TimeUnit.SECONDS);
					ordered.verify(mockChannelBuilder).keepAliveTime(50L, TimeUnit.SECONDS);
				}));
	}

	@Configuration(proxyBeanMethods = false)
	static class ChannelBuilderCustomizersConfig {

		static GrpcChannelBuilderCustomizer<?> CUSTOMIZER_FOO = mock();

		static GrpcChannelBuilderCustomizer<?> CUSTOMIZER_BAR = mock();

		@Bean
		@Order(200)
		GrpcChannelBuilderCustomizer<?> customizerFoo() {
			return CUSTOMIZER_FOO;
		}

		@Bean
		@Order(100)
		GrpcChannelBuilderCustomizer<?> customizerBar() {
			return CUSTOMIZER_BAR;
		}

	}

}
