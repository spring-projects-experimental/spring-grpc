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

import javax.net.ssl.TrustManagerFactory;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.grpc.autoconfigure.client.GrpcClientProperties.NamedChannel;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.NegotiationType;

import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.TlsChannelCredentials;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class NettyChannelCredentialsProvider implements ChannelCredentialsProvider {

	private final GrpcClientProperties channels;

	private final SslBundles bundles;

	public NettyChannelCredentialsProvider(SslBundles bundles, GrpcClientProperties channels) {
		this.bundles = bundles;
		this.channels = channels;
	}

	@Override
	public ChannelCredentials getChannelCredentials(String path) {
		SslBundle bundle = channels.sslBundle(bundles, path);
		NamedChannel channel = channels.getChannel(path);
		if (!channel.getSsl().isEnabled() && channel.getNegotiationType() == NegotiationType.PLAINTEXT) {
			return InsecureChannelCredentials.create();
		}
		if (bundle != null) {
			TrustManagerFactory trustManager = channel.isSecure() ? bundle.getManagers().getTrustManagerFactory()
					: InsecureTrustManagerFactory.INSTANCE;
			return TlsChannelCredentials.newBuilder()
				.keyManager(bundle.getManagers().getKeyManagerFactory().getKeyManagers())
				.trustManager(trustManager.getTrustManagers())
				.build();
		}
		else {
			if (channel.isSecure()) {
				return TlsChannelCredentials.create();
			}
			else {
				return TlsChannelCredentials.newBuilder()
					.trustManager(InsecureTrustManagerFactory.INSTANCE.getTrustManagers())
					.build();
			}
		}
	}

}
