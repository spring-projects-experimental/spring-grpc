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

import javax.net.ssl.SSLException;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.ClassUtils;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContextBuilder;

class NettyChannelFactoryHelper {

	private static final boolean AVAILABLE = ClassUtils.isPresent("io.grpc.netty.NettyChannelBuilder", null);

	public static boolean isAvailable() {
		return AVAILABLE;
	}

	public static void sslContext(ManagedChannelBuilder<?> builder, SslBundle bundle) {
		if (builder instanceof NettyChannelBuilder nettyBuilder) {
			try {
				nettyBuilder.sslContext(
						SslContextBuilder.forClient().keyManager(bundle.getManagers().getKeyManagerFactory()).build());
			}
			catch (SSLException e) {
				throw new IllegalStateException("Failed to create SSL context", e);
			}
		}
	}

}
