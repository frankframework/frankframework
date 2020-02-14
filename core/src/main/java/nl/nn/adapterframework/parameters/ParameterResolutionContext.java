/*
   Copyright 2013, 2016-2017,2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.parameters;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;
 
/**
 * Enables to determine the parameter values of the parameters during runtime.
 * 
 * @author Gerrit van Brakel
 */
public class ParameterResolutionContext {
	protected Logger log = LogUtil.getLogger(this);

	private Message message;
	private IPipeLineSession session;
	private Map<Boolean,Source> xmlSource;
	private boolean namespaceAware;

	/**
	 * Construct ParameterResolutionContext with the specified parameters.
	 * 
	 * PLEASE NOTE thread safety, see the documentation of parameter
	 * singleThreadOnly.
	 * @param input TODO
	 * @param session        the session object
	 * @param namespaceAware whether to process xml namespace aware
	 * @param singleThreadOnly when true (and the input message is transformed to
	 *                       a DOM object) the DOM object is cached for
	 *                       subsequent usage. Please note that a DOM object is
	 *                       not thread safe:
	 *                         https://saxonica.plan.io/boards/3/topics/6147
	 *                         https://www.saxonica.com/html/documentation/sourcedocs/thirdparty.html
	 *                       Disable caching when the ParameterResolutionContext
	 *                       is used by multiple threads.
	 */

	public ParameterResolutionContext(Object input, IPipeLineSession session, boolean namespaceAware, boolean singleThreadOnly) {
	if (input instanceof Message) {
		this.message=(Message)input;
	} else {
		this.message= new Message(input);
	}
	this.session = session;
	this.namespaceAware = namespaceAware;
	if (singleThreadOnly) {
		xmlSource=new HashMap<Boolean,Source>();
	}
}

	public ParameterResolutionContext(Object input, IPipeLineSession session, boolean namespaceAware) {
		this(input, session, namespaceAware, true);
	}

	public ParameterResolutionContext(Object input, IPipeLineSession session) {
		this(input, session, XmlUtils.isNamespaceAwareByDefault());
	}

	public ParameterResolutionContext() {
	}

	/**
	 * Returns an array list of <link>ParameterValue<link> objects
	 */
	public ParameterValueList getValues(ParameterList parameters) throws ParameterException {
		if (parameters == null) {
			return null;
		}
		return parameters.getValues(getMessage(), getSession(), isNamespaceAware());
	}

	/**
	 * Returns a Map of value objects
	 */
	public Map<String,Object> getValueMap(ParameterList parameters) throws ParameterException {
		if (parameters==null) {
			return null;
		}
		return parameters.getValues(getMessage(), getSession(), isNamespaceAware()).getValueMap();
	}
	

	
	public Source getInputSource(boolean namespaceAware) throws DomBuilderException {
		Source result = xmlSource!=null?xmlSource.get(namespaceAware):null;
		if (result == null) {
			log.debug("Constructing InputSource for ParameterResolutionContext");
			try {
				result = XmlUtils.stringToSource(message.asString(),namespaceAware);
			} catch (IOException e) {
				throw new DomBuilderException(e);
			} 
			if (xmlSource!=null) {
				xmlSource.put(namespaceAware, result);
			}
		}
		return result;
	}


	/**
	 * Returns hashtable with session variables
	 */
	public IPipeLineSession getSession() {
		return session;
	}
	public void setSession(IPipeLineSession session) {
		this.session = session;
	}

	@Deprecated 
	public boolean isNamespaceAware() {
		return namespaceAware;
	}
	@Deprecated 
	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}

	public Message getMessage() {
		return message;
	}

}