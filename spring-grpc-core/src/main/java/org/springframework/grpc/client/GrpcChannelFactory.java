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

package org.springframework.grpc.client;

import io.grpc.ManagedChannelBuilder;

/**
 * Factory interface for creating {@link ManagedChannelBuilder} instances for a given
 * authority.
 *
 * @author Dave Syer
 * @see ManagedChannelBuilder
 */
public interface GrpcChannelFactory {

	/**
	 * Creates a {@link ManagedChannelBuilder} for the given authority.
	 * @param authority the target authority for the channel
	 * @return a {@link ManagedChannelBuilder} configured for the given authority
	 */
	ManagedChannelBuilder<?> createChannel(String authority);

}
