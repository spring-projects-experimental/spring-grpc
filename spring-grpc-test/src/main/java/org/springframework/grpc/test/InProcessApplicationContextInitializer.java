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

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

/**
 * An {@link ApplicationContextInitializer} that configures and registers an in-process
 * gRPC {@link Server} and {@link ManagedChannel} within a Spring
 * {@link ConfigurableApplicationContext}. This initializer is intended for testing
 * environments, allowing gRPC communication within the same process without network
 * overhead.
 *
 * @author Andrei Lisa
 */
public class InProcessApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final String PROPERTY_SOURCE_NAME = "spring.grpc.inprocess";

	private static final String CHANNEL_NAME = "grpcInProcessChannel";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		String inProcessEnabled = applicationContext.getEnvironment().getProperty(PROPERTY_SOURCE_NAME);

		if ("true".equalsIgnoreCase(inProcessEnabled) && isJarOnClasspath()) {
			try {
				String serverName = InProcessServerBuilder.generateName();

				Server grpcServer = InProcessServerBuilder.forName(serverName).directExecutor().build().start();

				ManagedChannel grpcChannel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
				applicationContext.getBeanFactory().registerSingleton(CHANNEL_NAME, grpcChannel);

				applicationContext.addApplicationListener(new InProcessServerShutdownListener(grpcServer, grpcChannel));

			}
			catch (Exception e) {
				throw new RuntimeException("Failed to initialize in-process gRPC server", e);
			}
		}
	}

	private boolean isJarOnClasspath() {
		try {
			Class.forName("io.grpc.inprocess.InProcessChannelBuilder");
			return true;
		}
		catch (ClassNotFoundException e) {
			return false;
		}
	}

}
