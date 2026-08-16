/*
   Copyright 2026 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.kubernetes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;

import org.frankframework.lifecycle.events.ApplicationMessageEvent;
import org.frankframework.lifecycle.events.ConfigurationMessageEvent;
import org.frankframework.lifecycle.events.MessageEvent;
import org.frankframework.lifecycle.events.MessageEventLevel;

@EnableKubernetesMockClient(crud = true)
class KubernetesEventPublisherTest {

	/**
	 * Reproduces the CI failure ("No default constructor found"): Spring's component-scan
	 * (org.frankframework.lifecycle.IbisInitializer is meta-annotated {@code @Component}) resolves
	 * this bean through the same annotation-driven constructor autowiring as
	 * AnnotationConfigApplicationContext#register. With two declared constructors and neither
	 * annotated @Autowired, Spring's implicit single-constructor rule doesn't apply, so it falls back
	 * to a no-arg constructor that doesn't exist.
	 */
	@Test
	void isInstantiableAsASpringBeanFromEnvironmentAlone() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
			ctx.getBeanFactory().registerSingleton("environment", new MockEnvironment().withProperty("POD_NAME", "ff-pod-1"));
			ctx.register(KubernetesEventPublisher.class);
			ctx.refresh();

			assertTrue(ctx.getBean(KubernetesEventPublisher.class) != null);
		}
	}

	KubernetesClient client; // injected by the mock extension — instance field => fresh server per test

	private static final MockEnvironment POD_ENV = new MockEnvironment().withProperty("POD_NAME", "ff-pod-1");

	@SuppressWarnings("rawtypes")
	private static MessageEvent<?> event(Class<? extends MessageEvent> type, String message, MessageEventLevel level) {
		MessageEvent<?> event = mock(type);
		// getMessage()/getLevel() are non-final Lombok getters; getTimestamp() is final on Spring's
		// ApplicationEvent and must not be stubbed (a mock returns 0, which is unused here).
		when(event.getMessage()).thenReturn(message);
		when(event.getLevel()).thenReturn(level);
		return event;
	}

	@Test
	void warnAndErrorConfigurationAndApplicationEventsAreApplicable() {
		assertTrue(KubernetesEventPublisher.isApplicable(
				event(ConfigurationMessageEvent.class, "Configuration [x] aborted starting; boom", MessageEventLevel.WARN)));
		assertTrue(KubernetesEventPublisher.isApplicable(
				event(ApplicationMessageEvent.class, "failed to load", MessageEventLevel.ERROR)));
		assertFalse(KubernetesEventPublisher.isApplicable(
				event(ConfigurationMessageEvent.class, "started", MessageEventLevel.INFO)));
	}

	@Test
	void reasonReflectsAbortAndSeverity() {
		assertEquals(KubernetesEventPublisher.REASON_ABORTED, KubernetesEventPublisher.reasonFor(
				event(ConfigurationMessageEvent.class, "Configuration [x] aborted starting; boom", MessageEventLevel.WARN)));
		assertEquals(KubernetesEventPublisher.REASON_ERROR, KubernetesEventPublisher.reasonFor(
				event(ApplicationMessageEvent.class, "failed to load", MessageEventLevel.ERROR)));
		assertEquals(KubernetesEventPublisher.REASON_WARNING, KubernetesEventPublisher.reasonFor(
				event(ConfigurationMessageEvent.class, "name does not match", MessageEventLevel.WARN)));
	}

	@Test
	void toEventShapesAWarningEventOnThePod() {
		Event kubernetesEvent = KubernetesEventPublisher.toEvent(
				event(ConfigurationMessageEvent.class, "Configuration [x] aborted starting; boom", MessageEventLevel.WARN),
				"ff-pod-1", "frank");

		assertEquals("Warning", kubernetesEvent.getType());
		assertEquals(KubernetesEventPublisher.REASON_ABORTED, kubernetesEvent.getReason());
		assertEquals("Pod", kubernetesEvent.getInvolvedObject().getKind());
		assertEquals("ff-pod-1", kubernetesEvent.getInvolvedObject().getName());
		assertEquals("frank", kubernetesEvent.getInvolvedObject().getNamespace());
		assertEquals("frankframework", kubernetesEvent.getReportingComponent());
	}

	@Test
	void applicableEventIsPostedToTheCluster() {
		KubernetesEventPublisher publisher = new KubernetesEventPublisher(client, POD_ENV);
		String namespace = client.getNamespace();

		publisher.onApplicationEvent(
				event(ConfigurationMessageEvent.class, "Configuration [x] aborted starting; boom", MessageEventLevel.WARN));

		List<Event> events = client.v1().events().inNamespace(namespace).list().getItems();
		assertEquals(1, events.size());
		assertEquals(KubernetesEventPublisher.REASON_ABORTED, events.get(0).getReason());
		assertEquals("Warning", events.get(0).getType());
	}

	@Test
	void nonApplicableEventIsNotPosted() {
		KubernetesEventPublisher publisher = new KubernetesEventPublisher(client, POD_ENV);
		String namespace = client.getNamespace();

		publisher.onApplicationEvent(event(ConfigurationMessageEvent.class, "started", MessageEventLevel.INFO));

		assertTrue(client.v1().events().inNamespace(namespace).list().getItems().isEmpty());
	}

	@Test
	void nullClientNoOps() {
		KubernetesEventPublisher publisher = new KubernetesEventPublisher(null, POD_ENV);
		// must not throw
		publisher.onApplicationEvent(
				event(ConfigurationMessageEvent.class, "Configuration [x] aborted starting; boom", MessageEventLevel.WARN));
	}
}
