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
package org.frankframework.ibistesttool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.core.IListener;
import org.frankframework.core.INamedObject;
import org.frankframework.core.IPipe;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.management.bus.DebuggerStatusChangedEvent;
import org.frankframework.parameters.IParameter;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.RunState;
import org.frankframework.util.StringUtil;
import org.frankframework.util.UUIDUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.run.ReportRunner;

/**
 * @author Jaco de Groot
 */
@Log4j2
public class Debugger implements IbisDebugger, nl.nn.testtool.Debugger, ApplicationListener<DebuggerStatusChangedEvent>, InitializingBean {
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");

	private static final String STUB_STRATEGY_STUB_ALL_SENDERS = "Stub all senders";

	private TestTool testTool;
	protected @Setter @Getter IbisManager ibisManager;
	private PipeDescriptionProvider pipeDescriptionProvider;
	private List<String> testerRoles;

	protected Set<String> inRerun = new HashSet<>();

	public void setTestTool(TestTool testTool) {
		testTool.setDebugger(this);
		this.testTool = testTool;
		log.info("configuring debugger on TestTool [{}]", testTool);
	}

	@Override
	public void afterPropertiesSet() {
		if(testTool == null) {
			log.info("no TestTool found on classpath, skipping testtool wireing.");
			APPLICATION_LOG.info("No TestTool found on classpath, skipping testtool wireing.");
		}
	}

	public void setPipeDescriptionProvider(PipeDescriptionProvider pipeDescriptionProvider) {
		this.pipeDescriptionProvider = pipeDescriptionProvider;
	}

	public void setTesterRoles(List<String> testerRoles) {
		this.testerRoles = testerRoles;
	}

	@Override
	public Message pipelineInput(PipeLine pipeLine, String correlationId, Message input) {
		return testTool.startpoint(correlationId, pipeLine.getClass().getName(), "Pipeline " + pipeLine.getOwner().getName(), input);
	}

	@Override
	public Object pipelineSessionKey(String correlationId, String sessionKey, Object sessionValue) {
		return testTool.inputpoint(correlationId, null, "SessionKey " + sessionKey, sessionValue);
	}

	@Override
	public Message pipelineOutput(PipeLine pipeLine, String correlationId, Message output) {
		return testTool.endpoint(correlationId, pipeLine.getClass().getName(), "Pipeline " + pipeLine.getOwner().getName(), output);
	}

	@Override
	public Message pipelineAbort(PipeLine pipeLine, String correlationId, Message output) {
		return testTool.abortpoint(correlationId, pipeLine.getClass().getName(), "Pipeline " + pipeLine.getOwner().getName(), output);
	}

	@Override
	public Throwable pipelineAbort(PipeLine pipeLine, String correlationId, Throwable throwable) {
		testTool.abortpoint(correlationId, pipeLine.getClass().getName(), "Pipeline " + pipeLine.getOwner().getName(), throwable);
		return throwable;
	}

	@Override
	public <T> T pipeInput(PipeLine pipeLine, IPipe pipe, String correlationId, T input) {
		PipeDescription pipeDescription = pipeDescriptionProvider.getPipeDescription(pipeLine, pipe);
		T result = testTool.startpoint(correlationId, pipe.getClass().getName(), pipeDescription.getCheckpointName(), input);
		if (pipeDescription.getDescription() != null) {
			testTool.infopoint(correlationId, pipe.getClass().getName(), pipeDescription.getCheckpointName(), pipeDescription.getDescription());
			Iterator<String> iterator = pipeDescription.getResourceNames().iterator();
			while (iterator.hasNext()) {
				String resourceName = iterator.next();
				testTool.infopoint(correlationId, pipe.getClass().getName(), resourceName, pipeDescriptionProvider.getResource(pipeLine, resourceName));
			}
		}
		return result;
	}

	@Override
	public <T> T pipeOutput(PipeLine pipeLine, IPipe pipe, String correlationId, T output) {
		PipeDescription pipeDescription = pipeDescriptionProvider.getPipeDescription(pipeLine, pipe);
		return testTool.endpoint(correlationId, pipe.getClass().getName(), pipeDescription.getCheckpointName(), output);
	}

	@Override
	public Throwable pipeAbort(PipeLine pipeLine, IPipe pipe, String correlationId, Throwable throwable) {
		PipeDescription pipeDescription = pipeDescriptionProvider.getPipeDescription(pipeLine, pipe);
		testTool.abortpoint(correlationId, pipe.getClass().getName(), pipeDescription.getCheckpointName(), throwable);
		return throwable;
	}

	@Override
	public <T> T senderInput(ISender sender, String correlationId, T input) {
		return testTool.startpoint(correlationId, sender.getClass().getName(), getCheckpointNameForINamedObject("Sender ", sender), input);
	}

	@Override
	public <T> T senderOutput(ISender sender, String correlationId, T output) {
		return testTool.endpoint(correlationId, sender.getClass().getName(), getCheckpointNameForINamedObject("Sender ", sender), output);
	}

	@Override
	public Throwable senderAbort(ISender sender, String correlationId, Throwable throwable){
		testTool.abortpoint(correlationId, sender.getClass().getName(), getCheckpointNameForINamedObject("Sender ", sender), throwable);
		return throwable;
	}

	@Override
	public String replyListenerInput(IListener<?> listener, String correlationId, String input) {
		return testTool.startpoint(correlationId, listener.getClass().getName(), getCheckpointNameForINamedObject("Listener ", listener), input);
	}

	@Override
	public <M> M replyListenerOutput(IListener<M> listener, String correlationId, M output) {
		return testTool.endpoint(correlationId, listener.getClass().getName(), getCheckpointNameForINamedObject("Listener ", listener), output);
	}

	@Override
	public Throwable replyListenerAbort(IListener<?> listener, String correlationId, Throwable throwable){
		testTool.abortpoint(correlationId, listener.getClass().getName(), getCheckpointNameForINamedObject("Listener ", listener), throwable);
		return throwable;
	}

	@Override
	public void createThread(Object sourceObject, String threadId, String correlationId) {
		testTool.threadCreatepoint(correlationId, threadId);
	}

	@Override
	public void cancelThread(Object sourceObject, String threadId, String correlationId) {
		testTool.close(correlationId, threadId);
	}

	@Override
	public Object startThread(Object sourceObject, String threadId, String correlationId, Object input) {
		return testTool.threadStartpoint(correlationId, threadId, sourceObject.getClass().getName(), getCheckpointNameForThread(), input);
	}

	@Override
	public Object endThread(Object sourceObject, String correlationId, Object output) {
		return testTool.threadEndpoint(correlationId, sourceObject.getClass().getName(), getCheckpointNameForThread(), output);
	}

	@Override
	public Throwable abortThread(Object sourceObject, String correlationId, Throwable throwable) {
		testTool.abortpoint(correlationId, null, getCheckpointNameForThread(), throwable);
		return throwable;
	}

	@Override
	public Object getInputFromSessionKey(String correlationId, String sessionKey, Object sessionValue) {
		return testTool.inputpoint(correlationId, null, "GetInputFromSessionKey " + sessionKey, sessionValue);
	}

	@Override
	public Object getInputFromFixedValue(String correlationId, Object fixedValue) {
		return testTool.inputpoint(correlationId, null, "GetInputFromFixedValue", fixedValue);
	}

	@Override
	public Object getEmptyInputReplacement(String correlationId, Object replacementValue) {
		return testTool.inputpoint(correlationId, null, "getEmptyInputReplacement", replacementValue);
	}

	public Object capturedInput(String correlationId, String value) {
		return testTool.inputpoint(correlationId, null, "Captured input ", value);
	}

	@Override
	public Object parameterResolvedTo(IParameter parameter, String correlationId, Object value) {
		if (parameter.isHidden()) {
			log.debug("hiding parameter [{}] value", parameter::getName);
			String hiddenValue;
			try {
				hiddenValue = StringUtil.hide(MessageUtils.asString(value));
			} catch (IOException e) {
				hiddenValue = "IOException while hiding value for parameter " + parameter.getName() + ": " + e.getMessage();
				log.warn(hiddenValue, e);
			}
			testTool.inputpoint(correlationId, null, "Parameter " + parameter.getName(), hiddenValue);
			return value;
		}
		return testTool.inputpoint(correlationId, null, "Parameter " + parameter.getName(), value);
	}

	@Override
	public Object storeInSessionKey(String correlationId, String sessionKey, Object result) {
		return testTool.outputpoint(correlationId, null, "SessionKey " + sessionKey, result);
	}

	@Override
	public <T> T showInputValue(String correlationId, String label, T value) {
		return testTool.inputpoint(correlationId, null, label, value);
	}

	@Override
	public <T> T showOutputValue(String correlationId, String label, T value) {
		return testTool.outputpoint(correlationId, null, label, value);
	}

	@Override
	public Message preserveInput(String correlationId, Message input) {
		return testTool.outputpoint(correlationId, null, "PreserveInput", input);
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
	private Adapter getRegisteredAdapter(String name) {
		List<Adapter> adapters = getRegisteredAdapters();
		for (Adapter adapter : adapters) {
			if (name.equals(adapter.getName())) {
				return adapter;
			}
		}
		return null;
	}

	@Override
	public String rerun(String correlationId, Report originalReport, SecurityContext securityContext, ReportRunner reportRunner) {
		if (securityContext.isUserInRoles(testerRoles)) {
			int i = 0;
			List<Checkpoint> checkpoints = originalReport.getCheckpoints();
			Checkpoint checkpoint = checkpoints.get(i);
			String checkpointName = checkpoint.getName();
			if (checkpointName.startsWith("Pipeline ")) {
				String pipelineName = checkpointName.substring("Pipeline ".length());
				Message inputMessage = new Message(checkpoint.getMessageWithResolvedVariables(reportRunner));
				Adapter adapter = getRegisteredAdapter(pipelineName);
				if (adapter != null) {
					RunState runState = adapter.getRunState();
					if (runState == RunState.STARTED) {
						synchronized(inRerun) {
							inRerun.add(correlationId);
						}
						try {
							// Try with resource will make sure pipeLineSession is closed and all (possibly opened)
							// streams are also closed and the generated report will not remain in progress
							try (PipeLineSession pipeLineSession = new PipeLineSession()) {
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
								String messageId = "ladybug-testmessage" + UUIDUtil.createSimpleUUID();
								pipeLineSession.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
								PipeLineResult result = adapter.processMessageDirect(messageId, inputMessage, pipeLineSession);
								try {
									result.getResult().close();
								} catch (IOException e) {
									return "IOException': unable to close response message";
								}
							}
						} finally {
							synchronized(inRerun) {
								inRerun.remove(correlationId);
							}
						}
					} else {
						return "Adapter in state '" + runState + "'";
					}
				} else {
					return "Adapter '" + pipelineName + "' not found";
				}
			} else {
				return "First checkpoint isn't a pipeline";
			}
		} else {
			return "Not allowed";
		}
		return null;
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
		return stub(checkpoint.getName(), checkpoint.getType() == Checkpoint.TYPE_ENDPOINT, strategy);
	}

	// Called by IbisDebuggerAdvice
	@Override
	public boolean stubSender(ISender sender, String correlationId) {
		return stubINamedObject("Sender ", sender, correlationId);
	}

	// Called by IbisDebuggerAdvice
	@Override
	public boolean stubReplyListener(IListener<?> listener, String correlationId) {
		return stubINamedObject("Listener ", listener, correlationId);
	}

	private boolean stubINamedObject(String checkpointNamePrefix, INamedObject namedObject, String correlationId) {
		boolean rerun;
		synchronized(inRerun) {
			rerun = inRerun.contains(correlationId);
		}
		if (rerun) {
			Checkpoint originalEndpoint = testTool.getOriginalEndpointOrAbortpointForCurrentLevel(correlationId);
			if (originalEndpoint == null) {
//				stub = stub(getCheckpointNameForINamedObject(checkpointNamePrefix, namedObject), true, getDefaultStubStrategy());
				// TODO zou ook gewoon het orginele report kunnen gebruiken (via opslaan in iets als inRerun) of inRerun ook via testTool doen?
				Report reportInProgress = testTool.getReportInProgress(correlationId);
				return stub(getCheckpointNameForINamedObject(checkpointNamePrefix, namedObject), true, (reportInProgress==null?null:reportInProgress.getStubStrategy()));
			} else {
				if (originalEndpoint.getStub() == Checkpoint.STUB_FOLLOW_REPORT_STRATEGY) {
					return stub(originalEndpoint, originalEndpoint.getReport().getStubStrategy());
				} else if (originalEndpoint.getStub() == Checkpoint.STUB_NO) {
					return false;
				} else if (originalEndpoint.getStub() == Checkpoint.STUB_YES) {
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

	private static String getCheckpointNameForINamedObject(String checkpointNamePrefix, INamedObject object) {
		String name = object.getName();
		if (name == null) {
			name = object.getClass().getName();
			name = name.substring(name.lastIndexOf('.') + 1);
		}
		return checkpointNamePrefix + name;
	}

	private static String getCheckpointNameForThread() {
		// The checkpoint name for threads should not be unique, otherwise the
		// Test Tool isn't able to match those checkpoints when executed by
		// different threads (for example when comparing reports or determining
		// whether a checkpoint needs to be stubbed).
		String name = "Thread";
		String threadName = Thread.currentThread().getName();
		if (threadName != null && threadName.startsWith("SimpleAsyncTaskExecutor-")) {
			name = "Thread SimpleAsyncTaskExecutor";
		}
		return name;
	}

	// Contract for testtool state:
	// - when the state changes a DebuggerStatusChangedEvent must be fired to notify others
	// - to get notified of changes, components should listen to DebuggerStatusChangedEvents
	// IbisDebuggerAdvice stores state in AppConstants testtool.enabled for use by GUI

	@Override
	public void updateReportGeneratorStatus(boolean enabled) {
		if (ibisManager != null && ibisManager.getApplicationEventPublisher() != null) {
			DebuggerStatusChangedEvent event = new DebuggerStatusChangedEvent(this, enabled);
			log.debug("sending DebuggerStatusChangedEvent [{}]", event);
			ibisManager.getApplicationEventPublisher().publishEvent(event);
		}
	}

	@Override
	public void onApplicationEvent(DebuggerStatusChangedEvent event) {
		if (testTool != null && event.getSource() != this) {
			log.debug("received DebuggerStatusChangedEvent [{}]", event);
			testTool.setReportGeneratorEnabled(event.isEnabled());
		}
	}
}
