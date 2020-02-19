/*
   Copyright 2018 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.run.ReportRunner;

/**
 * @author Jaco de Groot
 */
public class Debugger implements IbisDebugger, nl.nn.testtool.Debugger {
	private static final String STUB_STRATEY_STUB_ALL_SENDERS = "Stub all senders";
	protected static final String STUB_STRATEY_NEVER = "Never";
	private static final String STUB_STRATEY_ALWAYS = "Always";

	private TestTool testTool;
	protected IbisManager ibisManager;
	private PipeDescriptionProvider pipeDescriptionProvider;
	private List<String> rerunRoles;

	protected Set inRerun = new HashSet();

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

	public Object pipeLineInput(PipeLine pipeLine, String correlationId, Object input) {
		return testTool.startpoint(correlationId, pipeLine.getClass().getName(), "Pipeline " + pipeLine.getOwner().getName(), input);
	}

	public Object pipeLineSessionKey(String correlationId, String sessionKey, Object sessionValue) {
		return testTool.inputpoint(correlationId, null, "SessionKey " + sessionKey, sessionValue);
	}

	public String pipeLineOutput(PipeLine pipeLine, String correlationId, String output) {
		return (String)testTool.endpoint(correlationId, pipeLine.getClass().getName(), "Pipeline " + pipeLine.getOwner().getName(), output);
	}

	public Throwable pipeLineAbort(PipeLine pipeLine, String correlationId, Throwable throwable) {
		testTool.abortpoint(correlationId, pipeLine.getClass().getName(), "Pipeline " + pipeLine.getOwner().getName(), throwable.getMessage());
		return throwable;
	}

	public Object pipeInput(PipeLine pipeLine, IPipe pipe, String correlationId, Object input) {
		PipeDescription pipeDescription = pipeDescriptionProvider.getPipeDescription(pipeLine, pipe);
		Object result = testTool.startpoint(correlationId, pipe.getClass().getName(), pipeDescription.getCheckpointName(), input);
		if (pipeDescription.getDescription() != null) {
			testTool.infopoint(correlationId, pipe.getClass().getName(), pipeDescription.getCheckpointName(), pipeDescription.getDescription());
			Iterator<String> iterator = pipeDescription.getResourceNames().iterator();
			while (iterator.hasNext()) {
				String resourceName = (String)iterator.next();
				testTool.infopoint(correlationId, pipe.getClass().getName(), resourceName, pipeDescriptionProvider.getResource(pipeLine, resourceName));
			}
		}
		return result;
	}
	
	public Object pipeOutput(PipeLine pipeLine, IPipe pipe, String correlationId, Object output) {
		PipeDescription pipeDescription = pipeDescriptionProvider.getPipeDescription(pipeLine, pipe);
		return testTool.endpoint(correlationId, pipe.getClass().getName(), pipeDescription.getCheckpointName(), output);
	}

	public Throwable pipeAbort(PipeLine pipeLine, IPipe pipe, String correlationId, Throwable throwable) {
		PipeDescription pipeDescription = pipeDescriptionProvider.getPipeDescription(pipeLine, pipe);
		testTool.abortpoint(correlationId, pipe.getClass().getName(), pipeDescription.getCheckpointName(), throwable.getMessage());
		return throwable;
	}

	public String senderInput(ISender sender, String correlationId, Object input) {
		return (String)testTool.startpoint(correlationId, sender.getClass().getName(), getCheckpointNameForINamedObject("Sender ", sender), input);
	}

	public String senderOutput(ISender sender, String correlationId, Object output) {
		return (String)testTool.endpoint(correlationId, sender.getClass().getName(), getCheckpointNameForINamedObject("Sender ", sender), output);
	}

	public Throwable senderAbort(ISender sender, String correlationId, Throwable throwable){
		testTool.abortpoint(correlationId, sender.getClass().getName(), getCheckpointNameForINamedObject("Sender ", sender), throwable.getMessage());
		return throwable;
	}

	public String replyListenerInput(IListener listener, String correlationId, String input) {
		return (String)testTool.startpoint(correlationId, listener.getClass().getName(), getCheckpointNameForINamedObject("Listener ", listener), input);
	}

	public String replyListenerOutput(IListener listener, String correlationId, String output) {
		return (String)testTool.endpoint(correlationId, listener.getClass().getName(), getCheckpointNameForINamedObject("Listener ", listener), output);
	}

	public Throwable replyListenerAbort(IListener listener, String correlationId, Throwable throwable){
		testTool.abortpoint(correlationId, listener.getClass().getName(), getCheckpointNameForINamedObject("Listener ", listener), throwable.getMessage());
		return throwable;
	}

	public void createThread(Object sourceObject, String threadId, String correlationId) {
		testTool.threadCreatepoint(correlationId, threadId); 
	}

	public Object startThread(Object sourceObject, String threadId, String correlationId, Object input) {
		return testTool.threadStartpoint(correlationId, threadId, sourceObject.getClass().getName(), getCheckpointNameForThread(), input);
	}

	public Object endThread(Object sourceObject, String correlationId, Object output) {
		return testTool.threadEndpoint(correlationId, sourceObject.getClass().getName(), getCheckpointNameForThread(), output);
	}

	public Throwable abortThread(Object sourceObject, String correlationId, Throwable throwable) {
		testTool.abortpoint(correlationId, null, getCheckpointNameForThread(), throwable.getMessage());
		return throwable;
	}

	public Object getInputFromSessionKey(String correlationId, String sessionKey, Object sessionValue) {
		return testTool.inputpoint(correlationId, null, "GetInputFromSessionKey " + sessionKey, sessionValue);
	}

	public Object getInputFromFixedValue(String correlationId, Object fixedValue) {
		return testTool.inputpoint(correlationId, null, "GetInputFromFixedValue", fixedValue);
	}

	public Object getEmptyInputReplacement(String correlationId, Object replacementValue) {
		return testTool.inputpoint(correlationId, null, "getEmptyInputReplacement", replacementValue);
	}

	public Object parameterResolvedTo(Parameter parameter, String correlationId, Object value) {
		return testTool.inputpoint(correlationId, null, "Parameter " + parameter.getName(), value);
	}

	public Object storeInSessionKey(String correlationId, Object sessionKey, Object result) {
		return testTool.outputpoint(correlationId, null, "SessionKey " + sessionKey.toString(), result);
	}

	public Object preserveInput(String correlationId, Object input) {
		return testTool.outputpoint(correlationId, null, "PreserveInput", input);
	}
	
	public String rerun(String correlationId, Report originalReport, SecurityContext securityContext, ReportRunner reportRunner) {
		String errorMessage = null;
		if (securityContext.isUserInRoles(rerunRoles)) {
			int i = 0;
			List checkpoints = originalReport.getCheckpoints();
			Checkpoint checkpoint = (Checkpoint)checkpoints.get(i);
			String checkpointName = checkpoint.getName();
			if (checkpointName.startsWith("Pipeline ")) {
				String pipelineName = checkpointName.substring("Pipeline ".length());
				String inputMessage = checkpoint.getMessageWithResolvedVariables(reportRunner);
				IAdapter adapter = ibisManager.getRegisteredAdapter(pipelineName);
				if (adapter != null) {
					IPipeLineSession pipeLineSession = new PipeLineSessionBase();
					while (checkpoints.size() > i + 1) {
						i++;
						checkpoint = (Checkpoint)checkpoints.get(i);
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
					synchronized(inRerun) {
						inRerun.add(correlationId);
					}
					try {
						adapter.processMessage(correlationId, inputMessage, pipeLineSession);
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

	public List getStubStrategies() {
		List stubStrategies = new ArrayList();
		stubStrategies.add(STUB_STRATEY_STUB_ALL_SENDERS);
		stubStrategies.add(STUB_STRATEY_NEVER);
		stubStrategies.add(STUB_STRATEY_ALWAYS);
		return stubStrategies;
	}

	public String getDefaultStubStrategy() {
		return STUB_STRATEY_STUB_ALL_SENDERS;
	}

	// Called by TestTool
	public boolean stub(Checkpoint checkpoint, String strategy) {
		return stub(checkpoint.getName(), checkpoint.getType() == Checkpoint.TYPE_ENDPOINT, strategy);
	}

	// Called by IbisDebuggerAdvice
	public boolean stubSender(ISender sender, String correlationId) {
		return stubINamedObject("Sender ", sender, correlationId);
	}

	// Called by IbisDebuggerAdvice
	public boolean stubReplyListener(IListener listener, String correlationId) {
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
		if (STUB_STRATEY_STUB_ALL_SENDERS.equals(stubStrategy)) {
			// A listener will be a reply listener because it's the only type of
			// listener handled by this class. A reply listener is always linked
			// to a sender, so stub when senders should be stubbed.
			if ((checkpointName.startsWith("Sender ") || checkpointName.startsWith("Listener "))
					&& isEndpoint) {
				return true;
			} else {
				return false;
			}
		} else if (STUB_STRATEY_ALWAYS.equals(stubStrategy)) {
			return true;
		} else {
			return false;
		}
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

}
