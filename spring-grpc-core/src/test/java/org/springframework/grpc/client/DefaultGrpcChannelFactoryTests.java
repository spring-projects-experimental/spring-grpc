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
package org.springframework.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.grpc.client.DefaultGrpcChannelFactory.DisposableChannelBuilder;

import io.grpc.ClientInterceptor;

/**
 * Tests for {@link DefaultGrpcChannelFactory}.
 */
class DefaultGrpcChannelFactoryTests {

	@Test
	void createChannelWithoutClientSpecificInterceptorsInvokesInterceptorConfigurer() {
		ClientInterceptorsConfigurer configurer = mock();
		var channelName = "testChannel";
		var channelFactory = new DefaultGrpcChannelFactory(List.of(), configurer);
		channelFactory.createChannel(channelName);
		// Get the actual builder that should be passed into the configurer
		assertThat(channelFactory)
			.extracting("builders", InstanceOfAssertFactories.map(String.class, DisposableChannelBuilder.class))
			.hasEntrySatisfying(channelName,
					(builder) -> verify(configurer).configureInterceptors(builder.delegate(), List.of(), false));
	}

	@Test
	void createChannelWithClientSpecificInterceptorsInvokesInterceptorConfigurer() {
		ClientInterceptorsConfigurer configurer = mock();
		var channelName = "testChannel";
		var channelFactory = new DefaultGrpcChannelFactory(List.of(), configurer);
		var interceptor = mock(ClientInterceptor.class);
		var interceptors = List.of(interceptor);
		channelFactory.createChannel(channelName, interceptors, true);
		// Get the actual builder that should be passed into the configurer
		assertThat(channelFactory)
			.extracting("builders", InstanceOfAssertFactories.map(String.class, DisposableChannelBuilder.class))
			.hasEntrySatisfying(channelName,
					(builder) -> verify(configurer).configureInterceptors(builder.delegate(), interceptors, true));
	}

}
