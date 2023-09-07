/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.dispatcher.DispatcherManager;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.stream.Message;

/**
 * Posts a message to another IBIS-adapter or application in the same JVM using IbisServiceDispatcher.
 *
 * An IbisJavaSender makes a call to a Receiver with a {@link JavaListener}
 * or any other application in the same JVM that has registered a <code>RequestProcessor</code> with the IbisServiceDispatcher.
 *
 * Any parameters are copied to the PipeLineSession of the service called.
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
 * N.B. Please make sure that the IbisServiceDispatcher-1.1.jar is present on the class path of the server.
 *
 * @author  Gerrit van Brakel
 * @since   4.4.5
 */
@Category("Advanced")
public class IbisJavaSender extends SenderWithParametersBase implements HasPhysicalDestination {

	private static final String MULTIPART_RESPONSE_CONTENT_TYPE = "application/octet-stream";
	private static final String MULTIPART_RESPONSE_CHARSET = "UTF-8";

	private final @Getter(onMethod = @__(@Override)) String domain = "JVM";

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
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		String result;
		try (PipeLineSession context = new PipeLineSession()) {
			try {
				if (paramList != null) {
					context.putAll(paramList.getValues(message, session).getValueMap());
				}
				DispatcherManager dm;
				Class c = Class.forName("nl.nn.adapterframework.dispatcher.DispatcherManagerFactory");

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
					serviceName = session.getMessage(getServiceNameSessionKey()).asString();
				} else {
					serviceName = getServiceName();
				}

				String correlationID = session == null ? null : session.getMessage(PipeLineSession.businessCorrelationIdKey).asString();
				result = dm.processRequest(serviceName, correlationID, message.asString(), context);
				if (isMultipartResponse()) {
					return HttpSender.handleMultipartResponse(MULTIPART_RESPONSE_CONTENT_TYPE, new ByteArrayInputStream(result.getBytes(MULTIPART_RESPONSE_CHARSET)), session);
				}

			} catch (ParameterException e) {
				throw new SenderException(getLogPrefix() + "exception evaluating parameters", e);
			} catch (Exception e) {
				throw new SenderException(getLogPrefix() + "exception processing message using request processor [" + serviceName + "]", e);
			} finally {
				if (log.isDebugEnabled() && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
					log.debug("returning values of session keys [" + getReturnedSessionKeys() + "]");
				}
				if (session != null) {
					PipeLineSession.mergeToParentSession(getReturnedSessionKeys(), context, session, this);
				}
			}
			return new Message(result);
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
			if(type.equalsIgnoreCase("DLL")) {
				dispatchType = type;
			} else {
				throw new ConfigurationException(getLogPrefix()+"the attribute 'setDispatchType' only supports the value 'DLL'");
			}
		}
	}

}
