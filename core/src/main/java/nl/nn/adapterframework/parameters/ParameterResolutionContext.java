/*
   Copyright 2013, 2016-2017 Nationale-Nederlanden

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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;
 
/**
 * Determines the parameter values of the specified parameter during runtime
 * 
 * @author Gerrit van Brakel
 */
public class ParameterResolutionContext {
	protected Logger log = LogUtil.getLogger(this);

	private String input;
	private IPipeLineSession session;
	private Source xmlSource;
	private boolean cacheXmlSource;
	private boolean namespaceAware;
	private boolean xslt2;

	/**
	 * Construct ParameterResolutionContext with the specified parameters.
	 * 
	 * PLEASE NOTE thread safety, see the documentation of parameter
	 * cacheXmlSource.
	 *  
	 * @param input          the (xml formatted) input message
	 * @param session        the session object
	 * @param namespaceAware whether to process xml namespace aware
	 * @param xslt2          when true use xslt2
	 * @param cacheXmlSource when true (and the input message is transformed to
	 *                       a DOM object) the DOM object is cached for
	 *                       subsequent usage. Please note that a DOM object is
	 *                       not thread safe:
	 *                         https://saxonica.plan.io/boards/3/topics/6147
	 *                         https://www.saxonica.com/html/documentation/sourcedocs/thirdparty.html
	 *                       Disable caching when the ParameterResolutionContext
	 *                       is used by multiple threads.
	 */
	public ParameterResolutionContext(String input, IPipeLineSession session,
			boolean namespaceAware, boolean xslt2, boolean cacheXmlSource) {
		this.input = input;
		this.session = session;
		this.namespaceAware = namespaceAware;
		this.xslt2 = xslt2;
		this.cacheXmlSource = cacheXmlSource;
	}

	public ParameterResolutionContext(String input, IPipeLineSession session,
			boolean namespaceAware, boolean xslt2) {
		this(input, session, namespaceAware, false, true);
	}

	public ParameterResolutionContext(String input, IPipeLineSession session,
			boolean namespaceAware) {
		this(input, session, namespaceAware, false);
	}

	public ParameterResolutionContext(String input, IPipeLineSession session) {
		this(input, session, XmlUtils.isNamespaceAwareByDefault());
	}

	public ParameterResolutionContext() {
	}

	/**
	 * @param p
	 * @return value as a <link>ParameterValue<link> object
	 * @throws IbisException
	 */
	private ParameterValue getValue(ParameterValueList alreadyResolvedParameters, Parameter p) throws ParameterException {
		return new ParameterValue(p, p.getValue(alreadyResolvedParameters, this));
	}
	
	/**
	 * @param parameters
	 * @return arraylist of <link>ParameterValue<link> objects
	 */
	public ParameterValueList getValues(ParameterList parameters) throws ParameterException {
		if (parameters == null)
			return null;
		
		ParameterValueList result = new ParameterValueList();
		for (Iterator<Parameter> parmIterator= parameters.iterator(); parmIterator.hasNext(); ) {
			Parameter parm = parmIterator.next();
			String parmSessionKey = parm.getSessionKey();
			if ("*".equals(parmSessionKey)) {
				String parmName = parm.getName();
				for (Iterator<String> keyIterator = session.keySet().iterator(); keyIterator.hasNext();) {
					String key = keyIterator.next();
					if (!PipeLineSessionBase.tsReceivedKey.equals(key)) {
						if ((key.startsWith(parmName) || "*".equals(parmName))) {
							Parameter newParm = new Parameter();
							newParm.setName(key);
							newParm.setSessionKey(key);
							try {
								newParm.configure();
							} catch (ConfigurationException e) {
								throw new ParameterException(e);
							}
							result.add(getValue(result, newParm));
						}
					}
				}
			} else {
				result.add(getValue(result, parm));
			}
		}
		return result;
	}

	/**
	 * @param parameters
	 * @return map of value objects
	 */
	public Map<String,Object> getValueMap(ParameterList parameters) throws ParameterException {
		if (parameters==null) {
			return null;
		}
		Map<String, ParameterValue> paramValuesMap = getValues(parameters).getParameterValueMap();

		// convert map with parameterValue to map with value		
		Map<String,Object> result = new LinkedHashMap<String,Object>(paramValuesMap.size());
		for (Iterator<ParameterValue> it= paramValuesMap.values().iterator(); it.hasNext(); ) {
			ParameterValue pv = it.next();
			result.put(pv.getDefinition().getName(), pv.getValue());
		}
		return result;
	}
	

	public ParameterValueList forAllParameters(ParameterList parameters, IParameterHandler handler) throws ParameterException {
		ParameterValueList values = getValues(parameters);
		if (values != null) {
			values.forAllParameters(handler);
		}
		return values;
	}
		
	/**
	 * @return the DOM document parsed from the (xml formatted) input
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Source getInputSource() throws DomBuilderException {
		Source xmlSource = this.xmlSource;
		if (xmlSource == null) {
			log.debug("Constructing InputSource for ParameterResolutionContext");
			xmlSource = XmlUtils.stringToSource(input,isNamespaceAware()); 
			if (cacheXmlSource) {
				this.xmlSource = xmlSource;
			}
		}
		return xmlSource;
	}

	/**
	 * @return the (possibly xml formatted) input message
	 */
	public String getInput() {
		return input;
	}

	/**
	 * @return hashtable with session variables
	 */
	public IPipeLineSession getSession() {
		return session;
	}

	/**
	 * @param input the (xml formatted) input message
	 */
	public void setInput(String input) {
		this.input = input;
		this.xmlSource = null;
	}

	/**
	 * @param session
	 */
	public void setSession(IPipeLineSession session) {
		this.session = session;
	}

	/**
	 */
	public boolean isNamespaceAware() {
		return namespaceAware;
	}

	/**
	 * @param b
	 */
	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}

	public boolean isXslt2() {
		return xslt2;
	}

	/**
	 * @param b
	 */
	public void setXslt2(boolean b) {
		xslt2 = b;
	}
}