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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.lifecycle.events.ApplicationMessageEvent;
import org.frankframework.lifecycle.events.ConfigurationMessageEvent;
import org.frankframework.lifecycle.events.MessageEvent;
import org.frankframework.lifecycle.events.MessageEventLevel;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

/**
 * Publishes {@link MessageEvent}s that signal a configuration or application startup problem as
 * Kubernetes core/v1 <em>Warning</em> Events on the pod, when Frank!Framework runs inside a
 * Kubernetes cluster. This surfaces problems such as a configuration failing to start
 * ("aborted starting") to cluster-level tooling (<code>kubectl get events</code>, dashboards,
 * operators) without log scraping, and catches non-crashing warnings that a crash-based signal would
 * miss.
 *
 * <p>Only {@link ConfigurationMessageEvent} and {@link ApplicationMessageEvent}s at
 * {@link MessageEventLevel#WARN} or {@link MessageEventLevel#ERROR} are published. Kubernetes Events
 * only have the types {@code Normal} and {@code Warning}, so both map to {@code Warning}; the severity
 * is carried in the Event <code>reason</code>.</p>
 *
 * <p>It reuses the same in-cluster Kubernetes access as {@code AbstractKubernetesCredentialProvider}
 * (the fabric8 client). At startup it builds a client and probes the API; if there is no in-cluster
 * access (local runs, plain servlet containers) it is a silent no-op. Set
 * {@code management.kubernetes.events.enabled=false} to disable it entirely.</p>
 *
 * <p><b>RBAC:</b> the pod's ServiceAccount needs {@code create} on the core {@code events} resource in
 * its own namespace — a different grant than reading secrets. Without it the API returns 403, which is
 * caught and logged, leaving startup unaffected.</p>
 */
@IbisInitializer
public class KubernetesEventPublisher implements ApplicationListener<MessageEvent<?>>, DisposableBean {

	private static final Logger LOG = LogUtil.getLogger(KubernetesEventPublisher.class);

	static final String ENABLED_KEY = "management.kubernetes.events.enabled";
	static final String REASON_ABORTED = "ConfigurationAborted";
	static final String REASON_ERROR = "ConfigurationError";
	static final String REASON_WARNING = "ConfigurationWarning";

	private static final String DEFAULT_NAMESPACE = "default";
	private static final int TIMEOUT_MILLIS = 3_000;
	private static final DateTimeFormatter K8S_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

	private final KubernetesClient client; // null => disabled (no in-cluster access), all calls no-op
	private final String namespace;
	private final String podName;

	public KubernetesEventPublisher() {
		this(buildInClusterClientOrNull(), System::getenv);
	}

	/** Package-private constructor allowing the client and environment to be injected for testing. */
	KubernetesEventPublisher(KubernetesClient client, UnaryOperator<String> env) {
		this.client = client;
		this.namespace = client == null ? DEFAULT_NAMESPACE : Optional.ofNullable(client.getNamespace()).orElse(DEFAULT_NAMESPACE);
		this.podName = resolvePodName(env);
		if (client == null) {
			LOG.info("Kubernetes event publishing disabled: no in-cluster API access");
		} else {
			LOG.info("Kubernetes event publishing enabled for pod [{}] in namespace [{}]", podName, namespace);
		}
	}

	@Override
	public void onApplicationEvent(@NonNull MessageEvent<?> event) {
		if (client == null || !isApplicable(event)) {
			return;
		}
		Event kubernetesEvent = toEvent(event, podName, namespace);
		try {
			client.v1().events().inNamespace(namespace).resource(kubernetesEvent).create();
		} catch (Exception e) {
			// Reporting a problem must never disturb the framework lifecycle.
			LOG.warn("could not publish Kubernetes event [{}]: {}", kubernetesEvent.getReason(), e.toString());
		}
	}

	@Override
	public void destroy() {
		if (client != null) {
			client.close();
		}
	}

	static boolean isApplicable(MessageEvent<?> event) {
		boolean relevantType = event instanceof ConfigurationMessageEvent || event instanceof ApplicationMessageEvent;
		MessageEventLevel level = event.getLevel();
		return relevantType && (level == MessageEventLevel.WARN || level == MessageEventLevel.ERROR);
	}

	static String reasonFor(MessageEvent<?> event) {
		String message = event.getMessage();
		if (message != null && message.contains("aborted starting")) {
			return REASON_ABORTED;
		}
		return event.getLevel() == MessageEventLevel.ERROR ? REASON_ERROR : REASON_WARNING;
	}

	static Event toEvent(MessageEvent<?> event, String podName, String namespace) {
		String time = K8S_TIME.format(Instant.ofEpochMilli(event.getTimestamp()));
		return new EventBuilder()
				.withNewMetadata()
					.withGenerateName("frankframework-")
					.withNamespace(namespace)
				.endMetadata()
				.withType("Warning")
				.withReason(reasonFor(event))
				.withMessage(event.getMessage())
				.withNewInvolvedObject()
					.withKind("Pod")
					.withName(podName)
					.withNamespace(namespace)
				.endInvolvedObject()
				.withNewSource()
					.withComponent("frankframework")
					.withHost(podName)
				.endSource()
				.withReportingComponent("frankframework")
				.withReportingInstance(podName)
				.withFirstTimestamp(time)
				.withLastTimestamp(time)
				.withCount(1)
				.build();
	}

	private static KubernetesClient buildInClusterClientOrNull() {
		if (!AppConstants.getInstance().getBoolean(ENABLED_KEY, true)) {
			return null;
		}
		try {
			KubernetesClient client = new KubernetesClientBuilder()
					.editOrNewConfig()
						.withConnectionTimeout(TIMEOUT_MILLIS)
						.withRequestTimeout(TIMEOUT_MILLIS)
						.withRequestRetryBackoffLimit(0)
					.endConfig()
					.build();
			client.getKubernetesVersion(); // probe: throws when there is no in-cluster access
			return client;
		} catch (Exception e) {
			LOG.info("no in-cluster Kubernetes API access ({}); event publishing stays disabled", e.toString());
			return null;
		}
	}

	private static String resolvePodName(UnaryOperator<String> env) {
		String name = env.apply("POD_NAME");
		if (name == null || name.isBlank()) {
			name = env.apply("HOSTNAME");
		}
		return (name == null || name.isBlank()) ? "unknown" : name;
	}
}
