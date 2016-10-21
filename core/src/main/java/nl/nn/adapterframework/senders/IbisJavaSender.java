/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
import java.util.HashMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.dispatcher.DispatcherManager;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;

/**
 * Posts a message to another IBIS-adapter or application in the same JVM using IbisServiceDispatcher.
 *
 * An IbisJavaSender makes a call to a Receiver with a {@link nl.nn.adapterframework.receivers.JavaListener JavaListener}
 * or any other application in the same JVM that has registered a <code>RequestProcessor</code> with the IbisServiceDispatcher.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.senders.IbisJavaSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>serviceName of the 
 * {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReturnedSessionKeys(String) returnedSessionKeys}</td><td>comma separated list of keys of session variables that should be returned to caller, for correct results as well as for erronous results. (Only for listeners that support it, like JavaListener)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMultipartResponse(boolean) multipartResponse}</td><td>currently used to mimic the HttpSender when it is stubbed locally. It could be useful in other situations too although currently the response string is used which isn't streamed, it would be better to pass the multipart as an input stream in the context map in which case content type and charset could also be passed</td><td>false</td></tr>
 * <tr><td>{@link #setMultipartResponseContentType(String) multipartResponseContentType}</td><td></td><td>application/octet-stream</td></tr>
 * <tr><td>{@link #setMultipartResponseCharset(String) multipartResponseCharset}</td><td></td><td>UTF-8</td></tr>
 * </table>
 * </p>
 * Any parameters are copied to the PipeLineSession of the service called.
 * 
 * <h4>configuring IbisJavaSender and JavaListener</h4>
 * <ul>
 *   <li>Define a GenericMessageSendingPipe with an IbisJavaSender</li>
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
public class IbisJavaSender extends SenderWithParametersBase implements HasPhysicalDestination {
	private String name;
	private String serviceName;
	private String returnedSessionKeys = null;
	private boolean multipartResponse = false;
	private String multipartResponseContentType = "application/octet-stream";
	private String multipartResponseCharset = "UTF-8";

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getServiceName())) {
			throw new ConfigurationException(getLogPrefix()+"must specify serviceName");
		}
	}

	public String getPhysicalDestinationName() {
		return "JavaListener "+getServiceName();
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		String result = null;
		HashMap context = null;
		try {
			if (paramList!=null) {
				context = prc.getValueMap(paramList);
			} else {
				context=new HashMap();
			}
			DispatcherManager dm = DispatcherManagerFactory.getDispatcherManager();
			result = dm.processRequest(getServiceName(),correlationID, message, context);
			if (isMultipartResponse()) {
				return HttpSender.handleMultipartResponse(multipartResponseContentType,
						new ByteArrayInputStream(result.getBytes(multipartResponseCharset)),
						prc, null);
			}
		
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"exception evaluating parameters",e);
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"exception processing message using request processor ["+getServiceName()+"]",e);
		} finally {
			if (log.isDebugEnabled() && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
				log.debug("returning values of session keys ["+getReturnedSessionKeys()+"]");
			}
			if (prc!=null) {
				Misc.copyContext(getReturnedSessionKeys(),context, prc.getSession());
			}
		}
		return result;
	}

	public void setName(String name) {
		this.name=name;
	}
	public String getName() {
		return name;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String string) {
		serviceName = string;
	}

	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}

	public String getReturnedSessionKeys() {
		return returnedSessionKeys;
	}

	public void setMultipartResponse(boolean b) {
		multipartResponse = b;
	}

	public boolean isMultipartResponse() {
		return multipartResponse;
	}

}
