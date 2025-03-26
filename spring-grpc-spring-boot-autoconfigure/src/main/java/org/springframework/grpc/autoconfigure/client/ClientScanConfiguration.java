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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.grpc.autoconfigure.client.GrpcClientProperties.ChannelConfig;
import org.springframework.grpc.client.BlockingStubFactory;
import org.springframework.grpc.client.GrpcClient;
import org.springframework.grpc.client.GrpcClientRegistryCustomizer;
import org.springframework.grpc.client.GrpcClientRegistryPostProcessor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(GrpcClientRegistryPostProcessor.class)
@GrpcClient
public class ClientScanConfiguration {

	@Bean
	public GrpcClientRegistryCustomizer defaultGrpcClientRegistryCustomizer(BeanFactory beanFactory,
			Environment environment) {
		List<String> packages = new ArrayList<>();
		if (AutoConfigurationPackages.has(beanFactory)) {
			packages.addAll(AutoConfigurationPackages.get(beanFactory));
		}
		Binder binder = Binder.get(environment);
		boolean hasDefaultChannel = binder.bind("spring.grpc.client.default-channel", ChannelConfig.class).isBound();
		return registry -> {
			if (hasDefaultChannel) {
				registry.channel("default").scan(BlockingStubFactory.class).packages(packages.toArray(new String[0]));
			}
		};
	}

}
