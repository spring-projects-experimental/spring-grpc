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
package org.springframework.grpc.test;

import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class InProcessTransportEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (AnnotationUtils.findAnnotation(application.getMainApplicationClass(),
				AutoConfigureInProcessTransport.class) == null) {
			return;
		}
		MapPropertySource inProcessTransportPropertySource = new MapPropertySource("inProcessTransportPropertySource",
				Collections.singletonMap("spring.grpc.inprocess.enabled", "true"));
		environment.getPropertySources().addFirst(inProcessTransportPropertySource);
	}

}