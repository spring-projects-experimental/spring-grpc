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

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/*
 * @author Andrei Lisa
 */
public class InProcessApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final String PROPERTY_SOURCE_NAME = "spring.grpc.inprocess";

	private static final String CHANNEL_NAME = "grpcInProcessChannel";

	private static Server grpcServer;

	private static ManagedChannel grpcChannel;

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		String inProcessEnabled = System.getProperty(PROPERTY_SOURCE_NAME, "true");

		if ("true".equalsIgnoreCase(inProcessEnabled) && isJarOnClasspath()) {
			try {
				String serverName = InProcessServerBuilder.generateName();

				grpcServer = InProcessServerBuilder.forName(serverName).directExecutor().build().start();

				grpcChannel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
				applicationContext.getBeanFactory().registerSingleton(CHANNEL_NAME, grpcChannel);

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

	public static void shutdown() {
		if (grpcChannel != null)
			grpcChannel.shutdownNow();
		if (grpcServer != null)
			grpcServer.shutdownNow();
	}

}
