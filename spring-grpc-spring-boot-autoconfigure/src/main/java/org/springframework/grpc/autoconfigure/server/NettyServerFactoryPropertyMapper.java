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

package org.springframework.grpc.autoconfigure.server;

import io.grpc.netty.NettyServerBuilder;

/**
 * Helper class used to map {@link GrpcServerProperties} to {@link NettyServerBuilder}.
 *
 * @author Chris Bono
 */
class NettyServerFactoryPropertyMapper extends BaseServerFactoryPropertyMapper<NettyServerBuilder> {

	NettyServerFactoryPropertyMapper(GrpcServerProperties properties) {
		super(properties);
	}

	@Override
	void customizeServerBuilder(NettyServerBuilder nettyServerBuilder) {
		super.customizeServerBuilder(nettyServerBuilder);
	}

}
