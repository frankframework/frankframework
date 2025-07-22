/*
   Copyright 2018 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.ladybug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.CheckpointType;
import nl.nn.testtool.Debugger;
import nl.nn.testtool.Report;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.StubType;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.run.ReportRunner;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.core.HasName;
import org.frankframework.core.IListener;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.DebuggerStatusChangedEvent;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.RunState;

/**
 * @author Jaco de Groot
 */
@Log4j2
public class LadybugDebugger implements ApplicationContextAware, ApplicationListener<DebuggerStatusChangedEvent>, ApplicationEventPublisherAware, Debugger, InitializingBean {
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");
	private static final String REPORT_ROOT_PREFIX = "Pipeline ";
	private static final String LADYBUG_TESTTOOL_NAME = "testTool";

	private static final String STUB_STRATEGY_STUB_ALL_SENDERS = "Stub all senders";

	private TestTool testtool;
	protected @Setter @Getter IbisManager ibisManager;
	private @Setter @Autowired List<String> testerRoles;
	private @Setter ApplicationContext applicationContext;
	private @Setter ApplicationEventPublisher applicationEventPublisher;

	protected Set<String> inRerun = new HashSet<>();

	/**
	 * TestTool can't be wired by name or type. We must fetch it from a parent (SpringBoot) context.
	 */
	@Override
	public void afterPropertiesSet() {
		if(applicationContext.containsBean(LADYBUG_TESTTOOL_NAME)) {
			testtool = applicationContext.getBean(LADYBUG_TESTTOOL_NAME, TestTool.class);
			testtool.setDebugger(this);
			log.info("configuring debugger on TestTool [{}]", testtool);
		} else {
			String message = "No Ladybug found on classpath, unable to load Ladybug debugger";
			log.info(message);
			APPLICATION_LOG.info(message);
		}
	}

	/** Get all configurations */
	private List<Adapter> getRegisteredAdapters() {
		List<Adapter> registeredAdapters = new ArrayList<>();
		for (Configuration configuration : ibisManager.getConfigurations()) {
			if(configuration.isActive()) {
				registeredAdapters.addAll(configuration.getRegisteredAdapters());
			}
		}
		return registeredAdapters;
	}

	/** Best effort attempt to locate the adapter. */
	@Nonnull
	private Adapter getRegisteredAdapter(String name) {
		int i = name.indexOf('/');
		if (i > 0) {
			String configName = name.substring(0, i);
			Configuration config = ibisManager.getConfiguration(configName);
			if (config == null) {
				throw new IllegalArgumentException("Configuration ["+configName+"] does not exist.");
			}
			String adapterName = name.substring(i + 1);
			Adapter adapter = config.getRegisteredAdapter(adapterName);
			if (adapter == null) {
				throw new IllegalArgumentException("Adapter ["+adapterName+"] does not exist in configuration ["+configName+"].");
			}
			return adapter;
		}

		List<Adapter> adapters = getRegisteredAdapters();
		for (Adapter adapter : adapters) {
			if (name.equals(adapter.getName())) {
				return adapter;
			}
		}
		throw new IllegalArgumentException("Adapter ["+name+"] not found.");
	}

	@Override
	public String rerun(String correlationId, Report originalReport, SecurityContext ingored, ReportRunner reportRunner) {
		if (!isAllowed()) {
			return "Not allowed";
		}

		List<Checkpoint> checkpoints = originalReport.getCheckpoints();
		Checkpoint checkpoint = checkpoints.get(0);
		String checkpointName = checkpoint.getName();
		if (checkpointName.startsWith(REPORT_ROOT_PREFIX)) {
			String pipelineName = checkpointName.substring(REPORT_ROOT_PREFIX.length());
			try {
				Adapter adapter = getRegisteredAdapter(pipelineName);
				RunState runState = adapter.getRunState();
				if (runState == RunState.STARTED) {
					synchronized(inRerun) {
						inRerun.add(correlationId);
					}

					try (Message inputMessage = new Message(checkpoint.getMessageWithResolvedVariables(reportRunner))) {
						// Try with resource will make sure pipeLineSession is closed and all (possibly opened)
						// streams are also closed and the generated report will not remain in progress
						try (PipeLineSession pipeLineSession = new PipeLineSession()) {
							int i = 0;
							while (checkpoints.size() > i + 1) {
								i++;
								checkpoint = checkpoints.get(i);
								checkpointName = checkpoint.getName();
								if (checkpointName.startsWith("SessionKey ")) {
									String sessionKey = checkpointName.substring("SessionKey ".length());
									if (!sessionKey.equals(PipeLineSession.CORRELATION_ID_KEY) && !sessionKey.equals(PipeLineSession.MESSAGE_ID_KEY)
											// messageId and id were used before 7.9
											&& !"messageId".equals(sessionKey) && !"id".equals(sessionKey)
											&& !sessionKey.equals(PipeLineSession.ORIGINAL_MESSAGE_KEY)) {
										pipeLineSession.put(sessionKey, checkpoint.getMessage());
									}
								} else {
									i = checkpoints.size();
								}
							}
							// Analog to test a pipeline that is using: "testmessage" + Misc.createSimpleUUID();
							String messageId = MessageUtils.generateMessageId("ladybug-testmessage");
							pipeLineSession.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
							PipeLineResult result = adapter.processMessageDirect(messageId, inputMessage, pipeLineSession);
							result.getResult().close();
						}
						return null;
					} finally {
						synchronized(inRerun) {
							inRerun.remove(correlationId);
						}
					}
				} else {
					return "Adapter in state '" + runState + "'";
				}
			} catch(Exception e) {
				log.info("an error occured while trying to rerun a ladybug report", e);
				return e.getMessage();
			}
		} else {
			return "First checkpoint isn't a pipeline";
		}
	}

	// Use Spring Security to verify if the user has the required roles.
	private boolean isAllowed() {
		return testerRoles != null && BusMessageUtils.hasAnyRole(testerRoles.toArray(new String[0]));
	}

	@Override
	public List<String> getStubStrategies() {
		List<String> stubStrategies = new ArrayList<>();
		stubStrategies.add(STUB_STRATEGY_STUB_ALL_SENDERS);
		stubStrategies.add(TestTool.STUB_STRATEGY_NEVER);
		stubStrategies.add(TestTool.STUB_STRATEGY_ALWAYS);
		return stubStrategies;
	}

	@Override
	public String getDefaultStubStrategy() {
		return STUB_STRATEGY_STUB_ALL_SENDERS;
	}

	// Called by TestTool
	@Override
	public boolean stub(Checkpoint checkpoint, String strategy) {
		return stub(checkpoint.getName(), checkpoint.getType() == CheckpointType.ENDPOINT.toInt(), strategy);
	}

	/**
	 * Called by IbisDebuggerAdvice.
	 * Verifies if the specified {@link ISender sender} should be stubbed or not.
	 */
	public boolean stubSender(ISender sender, String correlationId) {
		return stubINamedObject("Sender ", sender, correlationId);
	}

	/**
	 * Called by IbisDebuggerAdvice.
	 * Verifies if the specified {@link IListener listener} should be stubbed or not.
	 */
	public boolean stubReplyListener(IListener<?> listener, String correlationId) {
		return stubINamedObject("Listener ", listener, correlationId);
	}

	private boolean stubINamedObject(String checkpointNamePrefix, HasName namedObject, String correlationId) {
		boolean rerun;
		synchronized(inRerun) {
			rerun = inRerun.contains(correlationId);
		}
		if (rerun) {
			Checkpoint originalEndpoint = testtool.getOriginalEndpointOrAbortpointForCurrentLevel(correlationId);
			if (originalEndpoint == null) {
				// stub = stub(getCheckpointNameForINamedObject(checkpointNamePrefix, namedObject), true, getDefaultStubStrategy());
				// TODO zou ook gewoon het orginele report kunnen gebruiken (via opslaan in iets als inRerun) of inRerun ook via testtool doen?
				Report reportInProgress = testtool.getReportInProgress(correlationId);
				return stub(getCheckpointNameForNamedObject(checkpointNamePrefix, namedObject), true, (reportInProgress==null?null:reportInProgress.getStubStrategy()));
			} else {
				if (originalEndpoint.getStub() == StubType.FOLLOW_REPORT_STRATEGY.toInt()) {
					return stub(originalEndpoint, originalEndpoint.getReport().getStubStrategy());
				} else if (originalEndpoint.getStub() == StubType.NO.toInt()) {
					return false;
				} else if (originalEndpoint.getStub() == StubType.YES.toInt()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean stub(String checkpointName, boolean isEndpoint, String stubStrategy) {
		if (stubStrategy == null) {
			stubStrategy = getDefaultStubStrategy();
		}
		if (STUB_STRATEGY_STUB_ALL_SENDERS.equals(stubStrategy)) {
			// A listener will be a reply listener because it's the only type of
			// listener handled by this class. A reply listener is always linked
			// to a sender, so stub when senders should be stubbed.
			return (checkpointName.startsWith("Sender ") || checkpointName.startsWith("Listener ")) && isEndpoint;
		}
		if (TestTool.STUB_STRATEGY_ALWAYS.equals(stubStrategy)
			// Don't stub messageId as IbisDebuggerAdvice will read it as correlationId from PipeLineSession and
			// use it as correlationId parameter for checkpoints, hence these checkpoint will not be correlated to
			// the report with the correlationId used by the rerun method
			&& !"SessionKey messageId".equals(checkpointName)) {
			return true;
		}
		return false;
	}

	private static String getCheckpointNameForNamedObject(String checkpointNamePrefix, HasName object) {
		String name = object.getName();
		if (name == null) {
			name = object.getClass().getName();
			name = name.substring(name.lastIndexOf('.') + 1);
		}
		return checkpointNamePrefix + name;
	}

	// Contract for testtool state:
	// - when the state changes a DebuggerStatusChangedEvent must be fired to notify others
	// - to get notified of changes, components should listen to DebuggerStatusChangedEvents
	// IbisDebuggerAdvice stores state in AppConstants testtool.enabled for use by GUI

	@Override
	public void updateReportGeneratorStatus(boolean enabled) {
		if (applicationEventPublisher != null) {
			DebuggerStatusChangedEvent event = new DebuggerStatusChangedEvent(this, enabled);
			log.debug("sending DebuggerStatusChangedEvent [{}]", event);
			applicationEventPublisher.publishEvent(event);
		}
	}

	@Override
	public void onApplicationEvent(DebuggerStatusChangedEvent event) {
		if (testtool != null && event.getSource() != this) {
			log.debug("received DebuggerStatusChangedEvent [{}]", event);
			testtool.setReportGeneratorEnabled(event.isEnabled());
		}
	}
}
