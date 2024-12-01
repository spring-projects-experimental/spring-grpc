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
 * */

package org.springframework.grpc.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.grpc.internal.ApplicationContextBeanLookupUtils;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;

/**
 * Configure a {@link ManagedChannelBuilder} with client interceptors.
 *
 * @author Chris Bono
 */
public class ClientInterceptorsConfigurer implements InitializingBean {

	private final ApplicationContext applicationContext;

	private List<ClientInterceptor> globalInterceptors;

	public ClientInterceptorsConfigurer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Configure a {@link ManagedChannelBuilder} with client interceptors.
	 * @param builder the builder to configure
	 * @param interceptors the non-null list of interceptors to be applied to the channel
	 * @param mergeWithGlobalInterceptors whether the provided interceptors should be
	 * blended with the global interceptors.
	 */
	protected void configureInterceptors(ManagedChannelBuilder<?> builder, List<ClientInterceptor> interceptors,
			boolean mergeWithGlobalInterceptors) {
		// Add global interceptors first
		List<ClientInterceptor> allInterceptors = new ArrayList<>(this.globalInterceptors);
		// Add specific interceptors
		allInterceptors.addAll(interceptors);
		if (mergeWithGlobalInterceptors) {
			ApplicationContextBeanLookupUtils.sortBeansIncludingOrderAnnotation(this.applicationContext,
					ClientInterceptor.class, allInterceptors);
		}
		Collections.reverse(allInterceptors);
		builder.intercept(allInterceptors);
	}

	@Override
	public void afterPropertiesSet() {
		this.globalInterceptors = findGlobalInterceptors();
	}

	private List<ClientInterceptor> findGlobalInterceptors() {
		return ApplicationContextBeanLookupUtils.getBeansWithAnnotation(this.applicationContext,
				ClientInterceptor.class, GlobalClientInterceptor.class);
	}

}
