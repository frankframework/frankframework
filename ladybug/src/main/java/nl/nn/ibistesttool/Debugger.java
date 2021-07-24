/*
   Copyright 2018 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
package nl.nn.ibistesttool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.webcontrol.api.DebuggerStatusChangedEvent;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.run.ReportRunner;

/**
 * @author Jaco de Groot
 */
public class Debugger implements IbisDebugger, nl.nn.testtool.Debugger, ApplicationListener<DebuggerStatusChangedEvent>, ApplicationEventPublisherAware {
	private static final String STUB_STRATEGY_STUB_ALL_SENDERS = "Stub all senders";
	protected static final String STUB_STRATEGY_NEVER = "Never";
	private static final String STUB_STRATEGY_ALWAYS = "Always";

	private TestTool testTool;
	protected IbisManager ibisManager;
	private PipeDescriptionProvider pipeDescriptionProvider;
	private List<String> rerunRoles;

	protected Set<String> inRerun = new HashSet<String>();
	private ApplicationEventPublisher applicationEventPublisher;

	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setIbisManager(IbisManager ibisManager) {
		this.ibisManager = ibisManager;
	}

	public void setPipeDescriptionProvider(PipeDescriptionProvider pipeDescriptionProvider) {
		this.pipeDescriptionProvider = pipeDescriptionProvider;
	}

	public void setRerunRoles(List<String> rerunRoles) {
		this.rerunRoles = rerunRoles;
	}

	@Override
	public Message pipeLineInput(PipeLine pipeLine, String correlationId, Message input) {
		return testTool.startpoint(correlationId, pipeLine.getClass().getName(), "Pipeline " + pipeLine.getOwner().getName(), input);
	}

	@Override
	public Object pipeLineSessionKey(String correlationId, String sessionKey, Object sessionValue) {
		return testTool.inputpoint(correlationId, null, "SessionKey " + sessionKey, sessionValue);
	}

	@Override
	public Message pipeLineOutput(PipeLine pipeLine, String correlationId, Message output) {
		return testTool.endpoint(correlationId, pipeLine.getClass().getName(), "Pipeline " + pipeLine.getOwner().getName(), output);
	}

	@Override
	public Throwable pipeLineAbort(PipeLine pipeLine, String correlationId, Throwable throwable) {
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
	public String replyListenerOutput(IListener<?> listener, String correlationId, String output) {
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
	public Object parameterResolvedTo(Parameter parameter, String correlationId, Object value) {
		return testTool.inputpoint(correlationId, null, "Parameter " + parameter.getName(), value);
	}
	@Override
	public Object storeInSessionKey(String correlationId, String sessionKey, Object result) {
		return testTool.outputpoint(correlationId, null, "SessionKey " + sessionKey, result);
	}

	@Override
	public <T> T showValue(String correlationId, String label, T value) {
		return testTool.outputpoint(correlationId, null, label, value);
	}

	
	@Override
	public Message preserveInput(String correlationId, Message input) {
		return testTool.outputpoint(correlationId, null, "PreserveInput", input);
	}
	
	@Override
	public String rerun(String correlationId, Report originalReport, SecurityContext securityContext, ReportRunner reportRunner) {
		String errorMessage = null;
		if (securityContext.isUserInRoles(rerunRoles)) {
			int i = 0;
			List<Checkpoint> checkpoints = originalReport.getCheckpoints();
			Checkpoint checkpoint = checkpoints.get(i);
			String checkpointName = checkpoint.getName();
			if (checkpointName.startsWith("Pipeline ")) {
				String pipelineName = checkpointName.substring("Pipeline ".length());
				Message inputMessage = new Message(checkpoint.getMessageWithResolvedVariables(reportRunner));
				IAdapter adapter = ibisManager.getRegisteredAdapter(pipelineName);
				if (adapter != null) {
					synchronized(inRerun) {
						inRerun.add(correlationId);
					}
					try {
						// Try with resource will make sure pipeLineSession is closed and all (possibly opened) streams
						// are also closed and the generated report will not remain in progress
						try (PipeLineSession pipeLineSession = new PipeLineSession()) {
							while (checkpoints.size() > i + 1) {
								i++;
								checkpoint = checkpoints.get(i);
								checkpointName = checkpoint.getName();
								if (checkpointName.startsWith("SessionKey ")) {
									String sessionKey = checkpointName.substring("SessionKey ".length());
									if (!sessionKey.equals("messageId") && !sessionKey.equals("originalMessage")) {
										pipeLineSession.put(sessionKey, checkpoint.getMessage());
									}
								} else {
									i = checkpoints.size();
								}
							}
							adapter.processMessage(correlationId, inputMessage, pipeLineSession);
						}
					} finally {
						synchronized(inRerun) {
							inRerun.remove(correlationId);
						}
					}
				} else {
					errorMessage = "Adapter '" + pipelineName + "' not found";
				}
			} else {
				errorMessage = "First checkpoint isn't a pipeline";
			}
		} else {
			errorMessage = "Not allowed";
		}
		return errorMessage;
	}

	@Override
	public List<String> getStubStrategies() {
		List<String> stubStrategies = new ArrayList<String>();
		stubStrategies.add(STUB_STRATEGY_STUB_ALL_SENDERS);
		stubStrategies.add(STUB_STRATEGY_NEVER);
		stubStrategies.add(STUB_STRATEGY_ALWAYS);
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
		boolean stub = false;
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
				stub = stub(getCheckpointNameForINamedObject(checkpointNamePrefix, namedObject), true, (reportInProgress==null?null:reportInProgress.getStubStrategy()));
			} else {
				if (originalEndpoint.getStub() == Checkpoint.STUB_FOLLOW_REPORT_STRATEGY) {
					stub = stub(originalEndpoint, originalEndpoint.getReport().getStubStrategy());
				} else if (originalEndpoint.getStub() == Checkpoint.STUB_NO) {
					stub = false;
				} else if (originalEndpoint.getStub() == Checkpoint.STUB_YES) {
					stub = true;
				}
			}
		} else {
			stub = false;
		}
		return stub;
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
		if (STUB_STRATEGY_ALWAYS.equals(stubStrategy)
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
	// - to get notified of canges, components should listen to DebuggerStatusChangedEvents
	// IbisDebuggerAdvice stores state in appconstants testtool.enabled for use by GUI

	@Override
	public void updateReportGeneratorStatus(boolean enabled) {
		if (applicationEventPublisher != null) {
			DebuggerStatusChangedEvent event = new DebuggerStatusChangedEvent(this, enabled);
			applicationEventPublisher.publishEvent(event);
		}
	}

	@Override
	public void onApplicationEvent(DebuggerStatusChangedEvent event) {
		if (event.getSource()!=this) {
			testTool.setReportGeneratorEnabled(event.isEnabled());
		}
	}
	
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
