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
package org.springframework.grpc.server;

import org.springframework.util.ClassUtils;

import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;

class NettyServerFactoryHelper {

	private static final boolean AVAILABLE = ClassUtils.isPresent("io.netty.channel.epoll.Epoll", null)
			&& Epoll.isAvailable();

	public static boolean isAvailable() {
		return AVAILABLE;
	}

	public static ServerBuilder<?> forUnixDomainSocket(String path) {
		return NettyServerBuilder.forAddress(new DomainSocketAddress(path))
			.channelType(EpollServerDomainSocketChannel.class)
			.bossEventLoopGroup(new EpollEventLoopGroup(1))
			.workerEventLoopGroup(new EpollEventLoopGroup());
	}

}
