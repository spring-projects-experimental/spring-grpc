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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link GrpcClientFactory} before it is used. The registry is used by the application
 * context very early in its lifecycle, so customizers should not refer directly to other
 * beans. It is better to use a lazy lookup via the {@link ApplicationContext} or an
 * {@link ObjectProvider}
 *
 * @author Dave Syer
 */
public interface GrpcClientFactoryCustomizer {

	void customize(GrpcClientFactory registry);

}
