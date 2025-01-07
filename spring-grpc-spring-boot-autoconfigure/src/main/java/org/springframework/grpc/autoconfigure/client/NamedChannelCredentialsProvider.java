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
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.NamedChannel;
import org.springframework.grpc.client.NamedChannelRegistry;
import org.springframework.grpc.client.NegotiationType;
import org.springframework.grpc.internal.InsecureTrustManagerFactory;

import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.TlsChannelCredentials;

public class NamedChannelCredentialsProvider implements ChannelCredentialsProvider {

	private final SslBundles bundles;

	private final NamedChannelRegistry channelRegistry;

	public NamedChannelCredentialsProvider(SslBundles bundles, NamedChannelRegistry channelRegistry) {
		this.bundles = bundles;
		this.channelRegistry = channelRegistry;
	}

	@Override
	public ChannelCredentials getChannelCredentials(String path) {
		NamedChannel channel = this.channelRegistry.getChannel(path);
		if (!channel.getSsl().isEnabled() && channel.getNegotiationType() == NegotiationType.PLAINTEXT) {
			return InsecureChannelCredentials.create();
		}
		SslBundle bundle = channel.getSsl().isEnabled() ? this.bundles.getBundle(channel.getSsl().getBundle()) : null;
		if (bundle != null) {
			TrustManagerFactory trustManagers = channel.isSecure() ? bundle.getManagers().getTrustManagerFactory()
					: InsecureTrustManagerFactory.INSTANCE;
			return TlsChannelCredentials.newBuilder()
				.keyManager(bundle.getManagers().getKeyManagerFactory().getKeyManagers())
				.trustManager(trustManagers.getTrustManagers())
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
