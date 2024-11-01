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

package org.springframework.grpc.sample;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.grpc.test.InProcessApplicationContextInitializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/*
 * @author Andrei Lisa
 */

@SpringBootTest
class InProcessApplicationContextInitializerTests {

	private AnnotationConfigApplicationContext context;

	@BeforeEach
	public void setUp() {
		System.clearProperty("spring.grpc.inprocess");
		context = new AnnotationConfigApplicationContext();
	}

	@AfterEach
	public void cleanUp() {
		InProcessApplicationContextInitializer.shutdown();
		context.close();
	}

	@Nested
	class WhenDefaultEnabled {

		@Test
		void shouldInitializeInProcessServer() {
			new InProcessApplicationContextInitializer().initialize(context);
			context.refresh();

			ManagedChannel channel = context.getBean("grpcInProcessChannel", ManagedChannel.class);
			assertThat(channel).isNotNull().isInstanceOf(ManagedChannel.class);
		}

	}

	@Nested
	class WhenDisabledByProperty {

		@Test
		void shouldNotInitializeInProcessServer() {
			System.setProperty("spring.grpc.inprocess", "false");
			new InProcessApplicationContextInitializer().initialize(context);
			context.refresh();
			assertThatThrownBy(() -> {
				context.getBean("grpcInProcessChannel", ManagedChannel.class);
			}).isInstanceOf(NoSuchBeanDefinitionException.class);
		}

	}

	@Nested
	class WhenShutdownIsCalled {

		@Test
		void shouldShutdownInProcessServer() {
			new InProcessApplicationContextInitializer().initialize(context);
			context.refresh();

			ManagedChannel channel = context.getBean("grpcInProcessChannel", ManagedChannel.class);
			assertThat(channel).isNotNull();

			InProcessApplicationContextInitializer.shutdown();

			assertThat(channel).isNotNull();
			assertThat(channel.isShutdown()).isTrue();
		}

	}

}
