/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package org.frankframework.senders;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.http.HttpSender;
import org.frankframework.receivers.JavaListener;
import org.frankframework.stream.Message;

import lombok.Getter;
import nl.nn.adapterframework.dispatcher.DispatcherManager;

/**
 * Posts a message to another IBIS-adapter or application in the same JVM using IbisServiceDispatcher.
 *
 * An IbisJavaSender makes a call to a Receiver with a {@link JavaListener}
 * or any other application in the same JVM that has registered a <code>RequestProcessor</code> with the IbisServiceDispatcher.
 *
 * <h4>configuring IbisJavaSender and JavaListener</h4>
 * <ul>
 *   <li>Define a SenderPipe with an IbisJavaSender</li>
 *   <li>Set the attribute <code>serviceName</code> to <i>yourExternalServiceName</i></li>
 * </ul>
 * In the Adapter to be called:
 * <ul>
 *   <li>Define a Receiver with a JavaListener</li>
 *   <li>Set the attribute <code>serviceName</code> to <i>yourExternalServiceName</i></li>
 * </ul>
 * N.B. Please make sure that the IbisServiceDispatcher-1.4.jar or newer is present on the class path of the server.
 *
 * @ff.parameters All parameters are copied to the PipeLineSession of the service called.
 * @ff.forward "&lt;Exit.code&gt;" default
 *
 * @author  Gerrit van Brakel
 * @since   4.4.5
 */
@Category("Advanced")
public class IbisJavaSender extends SenderWithParametersBase implements HasPhysicalDestination {

	private static final String MULTIPART_RESPONSE_CONTENT_TYPE = "application/octet-stream";
	private static final String MULTIPART_RESPONSE_CHARSET = "UTF-8";

	private final @Getter String domain = "JVM";

	private @Getter String serviceName;
	private @Getter String serviceNameSessionKey;
	private @Getter String returnedSessionKeys = ""; // do not initialize with null, returned session keys must be set explicitly
	private @Getter boolean multipartResponse = false;
	private @Getter String dispatchType = "default";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getServiceName()) && StringUtils.isEmpty(getServiceNameSessionKey())) {
			throw new ConfigurationException(getLogPrefix()+"must specify serviceName or serviceNameSessionKey");
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		if (StringUtils.isNotEmpty(getServiceNameSessionKey())) {
			return "JavaListenerSessionKey "+getServiceNameSessionKey();
		}
		return "JavaListener "+getServiceName();
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		String result;
		try (PipeLineSession subAdapterSession = new PipeLineSession()) {
			subAdapterSession.put(PipeLineSession.MANUAL_RETRY_KEY, session.get(PipeLineSession.MANUAL_RETRY_KEY, false));
			try {
				if (paramList != null) {
					subAdapterSession.putAll(paramList.getValues(message, session).getValueMap());
				}
				DispatcherManager dm;
				Class<?> c = Class.forName("nl.nn.adapterframework.dispatcher.DispatcherManagerFactory");

				if (getDispatchType().equalsIgnoreCase("DLL")) {
					String version = nl.nn.adapterframework.dispatcher.Version.version;
					if (version.contains("IbisServiceDispatcher 1.3"))
						throw new SenderException("IBIS-ServiceDispatcher out of date! Please update to version 1.4 or higher");

					Method getDispatcherManager = c.getMethod("getDispatcherManager", String.class);
					dm = (DispatcherManager) getDispatcherManager.invoke(null, getDispatchType());
				} else {
					Method getDispatcherManager = c.getMethod("getDispatcherManager");
					dm = (DispatcherManager) getDispatcherManager.invoke(null, (Object[]) null);
				}

				String serviceName;
				if (StringUtils.isNotEmpty(getServiceNameSessionKey())) {
					serviceName = session.getString(getServiceNameSessionKey());
				} else {
					serviceName = getServiceName();
				}

				String correlationID = session.getCorrelationId();
				result = dm.processRequest(serviceName, correlationID, message.asString(), subAdapterSession);
				if (isMultipartResponse()) {
					return new SenderResult(HttpSender.handleMultipartResponse(MULTIPART_RESPONSE_CONTENT_TYPE, new ByteArrayInputStream(result.getBytes(MULTIPART_RESPONSE_CHARSET)), session));
				}
			} catch (ParameterException e) {
				throw new SenderException(getLogPrefix() + "exception evaluating parameters", e);
			} catch (Exception e) {
				throw new SenderException(getLogPrefix() + "exception processing message using request processor [" + serviceName + "]", e);
			} finally {
				if (log.isDebugEnabled() && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
					log.debug("returning values of session keys [" + getReturnedSessionKeys() + "]");
				}
				subAdapterSession.mergeToParentSession(getReturnedSessionKeys(), session);
			}
			ExitState exitState = (ExitState) subAdapterSession.remove(PipeLineSession.EXIT_STATE_CONTEXT_KEY);
			Object exitCode = subAdapterSession.remove(PipeLineSession.EXIT_CODE_CONTEXT_KEY);
			String forwardName = exitCode != null ? exitCode.toString() : null;
			return new SenderResult(exitState == null || exitState == ExitState.SUCCESS, new Message(result), "exitState=" + exitState, forwardName);
		}
	}

	/**
	 * ServiceName of the {@link JavaListener} that should be called.
	 */
	public void setServiceName(String string) {
		serviceName = string;
	}

	/**
	 * Key of session variable to specify ServiceName of the JavaListener that should be called.
	 */
	public void setServiceNameSessionKey(String string) {
		serviceNameSessionKey = string;
	}

	/**
	 * Comma separated list of keys of session variables that will be returned to caller, for correct results as well as for erroneous results.
	 * The set of available sessionKeys to be returned might be limited by the returnedSessionKeys attribute of the corresponding JavaListener.
	 */
	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}

	/**
	 * Currently used to mimic the HttpSender when it is stubbed locally. It could be useful in other situations too although currently the response string is used which isn't streamed, it would be better to pass the multipart as an input stream in the context map in which case content type and charset could also be passed
	 * @ff.default false
	 */
	public void setMultipartResponse(boolean b) {
		multipartResponse = b;
	}

	/**
	 * Set to 'DLL' to make the dispatcher communicate with a DLL set on the classpath
	 */
	public void setDispatchType(String type) throws ConfigurationException {
		if(StringUtils.isNotEmpty(type)) {
			if("DLL".equalsIgnoreCase(type)) {
				dispatchType = type;
			} else {
				throw new ConfigurationException(getLogPrefix()+"the attribute 'setDispatchType' only supports the value 'DLL'");
			}
		}
	}

}
