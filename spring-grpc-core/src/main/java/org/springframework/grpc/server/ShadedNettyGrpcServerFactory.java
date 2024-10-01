/*
 * Copyright 2024-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.grpc.server;

import java.net.InetSocketAddress;
import java.util.List;

import com.google.common.net.InetAddresses;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;

/**
 * {@link GrpcServerFactory} that can be used to create a shaded Netty-based gRPC server.
 *
 * @author David Syer
 * @author Chris Bono
 */
public class ShadedNettyGrpcServerFactory extends BaseGrpcServerFactory<NettyServerBuilder> {

	private static final String ANY_IP_ADDRESS = "*";

	public ShadedNettyGrpcServerFactory(String address, int port,
			List<ServerBuilderCustomizer<NettyServerBuilder>> serverBuilderCustomizers) {
		super(address, port, serverBuilderCustomizers);
	}

	/**
	 * Creates a new server builder.
	 * @return The newly created server builder.
	 */
	protected NettyServerBuilder newServerBuilder() {
		String address = getAddress();
		int port = getPort();
		if (address != null) {
			if (address.startsWith("unix:")) {
				String path = address.substring(5);
				return NettyServerBuilder.forAddress(new DomainSocketAddress(path))
					.channelType(EpollServerDomainSocketChannel.class)
					.bossEventLoopGroup(new EpollEventLoopGroup(1))
					.workerEventLoopGroup(new EpollEventLoopGroup());
			}
			if (!ANY_IP_ADDRESS.equals(address)) {
				return NettyServerBuilder.forAddress(new InetSocketAddress(InetAddresses.forString(address), port));
			}
			// TODO: Add more support for address resolution
		}
		return super.newServerBuilder();
	}

}
