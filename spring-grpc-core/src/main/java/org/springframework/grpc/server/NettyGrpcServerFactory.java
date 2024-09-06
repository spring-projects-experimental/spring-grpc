/*
 * Copyright 2016-2024 the original author or authors.
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
 *
 * Partial copy from net.devh:grpc-spring-boot-starter.
 */

package org.springframework.grpc.server;

import java.net.InetSocketAddress;
import java.util.List;

import org.springframework.grpc.util.GrpcUtils;

import com.google.common.net.InetAddresses;

import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;

/**
 * Factory for netty based grpc servers.
 *
 * @author Michael (yidongnan@gmail.com)
 * @author Dave Syer
 */
public class NettyGrpcServerFactory extends AbstractGrpcServerFactory<NettyServerBuilder> {

	/**
	 * Creates a new netty server factory with the given properties.
	 * @param address The address to bind the server to.
	 * @param port The port to bind the server to (0 for ephemeral port).
	 * @param properties The properties used to configure the server.
	 * @param serverConfigurers The server configurers to use. Can be empty.
	 */
	public NettyGrpcServerFactory(String address, int port, final List<GrpcServerConfigurer> serverConfigurers) {
		super(address, port, serverConfigurers);
	}

	@Override
	protected NettyServerBuilder newServerBuilder() {
		if (getAddres().startsWith(GrpcUtils.DOMAIN_SOCKET_ADDRESS_PREFIX)) {
			final String path = GrpcUtils.extractDomainSocketAddressPath(getAddres());
			return NettyServerBuilder.forAddress(new DomainSocketAddress(path))
				.channelType(EpollServerDomainSocketChannel.class)
				.bossEventLoopGroup(new EpollEventLoopGroup(1))
				.workerEventLoopGroup(new EpollEventLoopGroup());
		}
		else if (GrpcUtils.ANY_IP_ADDRESS.equals(getAddres())) {
			return NettyServerBuilder.forPort(getPort());
		}
		else {
			return NettyServerBuilder
				.forAddress(new InetSocketAddress(InetAddresses.forString(getAddres()), getPort()));
		}
	}

}
