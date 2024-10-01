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

import java.io.IOException;
import java.util.Iterator;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import nl.nn.testtool.TestTool;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import org.frankframework.core.IListener;
import org.frankframework.core.INamedObject;
import org.frankframework.core.IPipe;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.IParameter;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StringUtil;

/**
 * Interface between the AOP config and the Ladybug. Takes case of boilerplate code such as report name, checkpoint names, pipe descriptions.
 */
@Log4j2
public class LadybugReportGenerator implements InitializingBean {
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");
	private static final String REPORT_ROOT_PREFIX = "Pipeline ";

	private TestTool testTool;
	private @Setter @Autowired PipeDescriptionProvider pipeDescriptionProvider;

	@Autowired
	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
		log.info("configuring TestTool on LadybugReportGenerator [{}]", testTool);
	}

	@Override
	public void afterPropertiesSet() {
		if(testTool == null || pipeDescriptionProvider == null) {
			log.info("no TestTool or pipeDescriptionProvider found on classpath, skipping testtool wireing.");
			APPLICATION_LOG.info("No TestTool or pipeDescriptionProvider found on classpath, skipping testtool wireing.");
		}
	}

	// combines the config and adapter-names so each report is unique and can be 'rerun'.
	private String getName(PipeLine pipeLine) {
		String adapterName = pipeLine.getAdapter().getName();
		String configName = pipeLine.getAdapter().getConfiguration().getName();
		return REPORT_ROOT_PREFIX + configName + "/" + adapterName;
	}

	public Message pipelineInput(PipeLine pipeLine, String correlationId, Message input) {
		return testTool.startpoint(correlationId, pipeLine.getClass().getName(), getName(pipeLine), input);
	}

	public Message pipelineOutput(PipeLine pipeLine, String correlationId, Message output) {
		return testTool.endpoint(correlationId, pipeLine.getClass().getName(), getName(pipeLine), output);
	}

	public Message pipelineAbort(PipeLine pipeLine, String correlationId, Message output) {
		return testTool.abortpoint(correlationId, pipeLine.getClass().getName(), getName(pipeLine), output);
	}

	public Throwable pipelineAbort(PipeLine pipeLine, String correlationId, Throwable throwable) {
		testTool.abortpoint(correlationId, pipeLine.getClass().getName(), getName(pipeLine), throwable);
		return throwable;
	}

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

	public <T> T pipeOutput(PipeLine pipeLine, IPipe pipe, String correlationId, T output) {
		PipeDescription pipeDescription = pipeDescriptionProvider.getPipeDescription(pipeLine, pipe);
		return testTool.endpoint(correlationId, pipe.getClass().getName(), pipeDescription.getCheckpointName(), output);
	}

	public Throwable pipeAbort(PipeLine pipeLine, IPipe pipe, String correlationId, Throwable throwable) {
		PipeDescription pipeDescription = pipeDescriptionProvider.getPipeDescription(pipeLine, pipe);
		testTool.abortpoint(correlationId, pipe.getClass().getName(), pipeDescription.getCheckpointName(), throwable);
		return throwable;
	}

	public <T> T senderInput(ISender sender, String correlationId, T input) {
		return testTool.startpoint(correlationId, sender.getClass().getName(), getCheckpointNameForINamedObject("Sender ", sender), input);
	}

	public <T> T senderOutput(ISender sender, String correlationId, T output) {
		return testTool.endpoint(correlationId, sender.getClass().getName(), getCheckpointNameForINamedObject("Sender ", sender), output);
	}

	public Throwable senderAbort(ISender sender, String correlationId, Throwable throwable){
		testTool.abortpoint(correlationId, sender.getClass().getName(), getCheckpointNameForINamedObject("Sender ", sender), throwable);
		return throwable;
	}

	public String replyListenerInput(IListener<?> listener, String correlationId, String input) {
		return testTool.startpoint(correlationId, listener.getClass().getName(), getCheckpointNameForINamedObject("Listener ", listener), input);
	}

	public <M> M replyListenerOutput(IListener<M> listener, String correlationId, M output) {
		return testTool.endpoint(correlationId, listener.getClass().getName(), getCheckpointNameForINamedObject("Listener ", listener), output);
	}

	public Throwable replyListenerAbort(IListener<?> listener, String correlationId, Throwable throwable){
		testTool.abortpoint(correlationId, listener.getClass().getName(), getCheckpointNameForINamedObject("Listener ", listener), throwable);
		return throwable;
	}

	public void createThread(Object sourceObject, String threadId, String correlationId) {
		testTool.threadCreatepoint(correlationId, threadId);
	}

	public void cancelThread(Object sourceObject, String threadId, String correlationId) {
		testTool.close(correlationId, threadId);
	}

	public Object startThread(Object sourceObject, String threadId, String correlationId, Object input) {
		return testTool.threadStartpoint(correlationId, threadId, sourceObject.getClass().getName(), getCheckpointNameForThread(), input);
	}

	public Object endThread(Object sourceObject, String correlationId, Object output) {
		return testTool.threadEndpoint(correlationId, sourceObject.getClass().getName(), getCheckpointNameForThread(), output);
	}

	public Throwable abortThread(Object sourceObject, String correlationId, Throwable throwable) {
		testTool.abortpoint(correlationId, null, getCheckpointNameForThread(), throwable);
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

	public Object sessionOutputPoint(String correlationId, String sessionKey, Object result) {
		String name = "SessionKey " + sessionKey;
		if(sessionKey.startsWith(PipeLineSession.SYSTEM_MANAGED_RESOURCE_PREFIX)) {
			name = "SystemKey " + sessionKey;
		}

		return testTool.outputpoint(correlationId, null, name, result);
	}

	public Object sessionInputPoint(String correlationId, String sessionKey, Object result) {
		String name = "SessionKey " + sessionKey;
		if(sessionKey.startsWith(PipeLineSession.SYSTEM_MANAGED_RESOURCE_PREFIX)) {
			name = "SystemKey " + sessionKey;
		}
		return testTool.inputpoint(correlationId, null, name, result);
	}

	public <T> T showInputValue(String correlationId, String label, T value) {
		return testTool.inputpoint(correlationId, null, label, value);
	}

	public <T> T showOutputValue(String correlationId, String label, T value) {
		return testTool.outputpoint(correlationId, null, label, value);
	}

	public Message preserveInput(String correlationId, Message input) {
		return testTool.outputpoint(correlationId, null, "PreserveInput", input);
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
