package org.frankframework.ladybug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.Nullable;

import lombok.Data;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.Rerunner;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.run.ReportRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.util.JacksonUtils;
import org.frankframework.util.UUIDUtil;

public class SpringBusRerunner implements Rerunner {
	private Set<String> inRerun = new HashSet<>();

	private static final String ORIGINAL_MESSAGE_KEY = "originalMessage";
	private static final String MESSAGE_ID_KEY       = "mid";
	private static final String CORRELATION_ID_KEY   = "cid";
	private static final String REPORT_ROOT_PREFIX = "Pipeline ";

	@Autowired
	private ApplicationContext applicationContext;

	private OutboundGateway getGateway() {
		return applicationContext.getBean("outboundGateway", OutboundGateway.class);
	}

	@Override
	public String rerun(String correlationId, Report originalReport, SecurityContext securityContext, ReportRunner reportRunner) {
		String user = BusMessageUtils.getUserPrincipalName();
		if(user == null) {
			return "No user found, action not permitted!";
		}

		int i = 0;
		List<Checkpoint> checkpoints = originalReport.getCheckpoints();
		Checkpoint checkpoint = checkpoints.get(i);
		String checkpointName = checkpoint.getName();
		if (!checkpointName.startsWith(REPORT_ROOT_PREFIX)) {
			return "First checkpoint isn't a pipeline, unable to rerun report!";
		}

		String inputMessage = checkpoint.getMessageWithResolvedVariables(reportRunner);
		String adapterWithConfigName = checkpointName.substring(REPORT_ROOT_PREFIX.length());
		int index = adapterWithConfigName.indexOf('/');
		if(index < 0) {
			return "adapter name invalid";
		}
		String configName = adapterWithConfigName.substring(0, index);
		String adapterName = adapterWithConfigName.substring(index+1);

		synchronized(inRerun) {
			inRerun.add(correlationId);
		}
		try {
			Map<String, String> threadContext = new HashMap<>();
			// Try with resource will make sure pipeLineSession is closed and all (possibly opened)
			// streams are also closed and the generated report will not remain in progress
			while (checkpoints.size() > i + 1) {
				i++;
				checkpoint = checkpoints.get(i);
				checkpointName = checkpoint.getName();
				if (checkpointName.startsWith("SessionKey ")) {
					String sessionKey = checkpointName.substring("SessionKey ".length());
					if (shouldCopySessionKey(sessionKey)) {
						threadContext.put(sessionKey, checkpoint.getMessage());
					}
				} else {
					i = checkpoints.size();
				}
			}

			// Analog to test a pipeline that is using: "testmessage" + Misc.createSimpleUUID();
			String messageId = "ladybug-testmessage" + UUIDUtil.createSimpleUUID();
			threadContext.put(MESSAGE_ID_KEY, messageId);
			threadContext.put(CORRELATION_ID_KEY, correlationId);

			MessageBuilder<String> builder = createRequestMessage(BusTopic.TEST_PIPELINE, BusAction.UPLOAD, inputMessage, null);
			builder.setHeader(BusMessageUtils.HEADER_PREFIX+BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configName);
			builder.setHeader(BusMessageUtils.HEADER_PREFIX+BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapterName);
			builder.setHeader(BusMessageUtils.HEADER_PREFIX+"sessionKeys", toJson(threadContext));

			return processRequest(builder.build()); //null implies success
		} finally {
			synchronized(inRerun) {
				inRerun.remove(correlationId);
			}
		}
	}

	protected String toJson(Map<String, String> threadContext) {
		List<PostedSessionKey> remappedContext = threadContext.entrySet()
				.stream()
				.map(PostedSessionKey::new)
				.toList();

		return JacksonUtils.convertToJson(remappedContext);
	}

	static record PostedSessionKey(String key, String value) {
		public PostedSessionKey(Map.Entry<String,String> entry) {
			this(entry.getKey(), entry.getValue());
		}
	}

	private MessageBuilder<String> createRequestMessage(BusTopic topic, BusAction action, String payload, @Nullable UUID uuid) {
		MessageBuilder<String> builder = MessageBuilder.withPayload(payload);
		builder.setHeader(BusTopic.TOPIC_HEADER_NAME, topic.name());
		builder.setHeader(BusAction.ACTION_HEADER_NAME, action.name());

		// Optional target parameter, to target a specific backend node.
		if(uuid != null) {
			builder.setHeader(BusMessageUtils.HEADER_TARGET_KEY, uuid);
		}

		return builder;
	}

	private String processRequest(Message<String> request) {
		try {
			Message<?> message = getGateway().sendSyncMessage(request);
			if (message == null) {
				return "did not receive a reply from backend node";
			}
		} catch (BusException e) {
			return "Exception processing rerun: " + e.getMessage();
		}
		return null; // we received a message, assume all is well. null implies no error.
	}

	private boolean shouldCopySessionKey(String sessionKey) {
		return !sessionKey.equals(CORRELATION_ID_KEY) && !sessionKey.equals(MESSAGE_ID_KEY)
				// messageId and id were used before 7.9
				&& !"messageId".equals(sessionKey) && !"id".equals(sessionKey)
				&& !sessionKey.equals(ORIGINAL_MESSAGE_KEY);
	}
}
