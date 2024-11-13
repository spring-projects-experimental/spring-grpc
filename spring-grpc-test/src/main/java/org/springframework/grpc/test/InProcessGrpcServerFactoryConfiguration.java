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

package org.springframework.grpc.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the {@link InProcessGrpcServerFactory} bean. This configuration
 * ensures that the InProcessGrpcServerFactory is only created if it's not already
 * available as a bean and if the necessary conditions are met.
 *
 * @author Andrei Lisa
 */
@Configuration(proxyBeanMethods = false)
public class InProcessGrpcServerFactoryConfiguration {

	@Value("${spring.grpc.inprocess.enabled:true}")
	private String inProcessEnabled;

	@Bean
	InProcessGrpcServerFactory grpcInProcessServerFactory(ConfigurableApplicationContext applicationContext) {
		boolean enabled = Boolean.parseBoolean(inProcessEnabled) && isJarOnClasspath();
		if (enabled) {
			InProcessGrpcServerFactory factory = new InProcessGrpcServerFactory(applicationContext, enabled);
			try {
				factory.afterPropertiesSet();
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to initialize the InProcessGrpcServerFactory", e);
			}
			return factory;
		}
		else {
			return null;
		}
	}

	private static boolean isJarOnClasspath() {
		try {
			Class.forName("io.grpc.inprocess.InProcessServerBuilder");
			return true;
		}
		catch (ClassNotFoundException e) {
			return false;
		}
	}

}
