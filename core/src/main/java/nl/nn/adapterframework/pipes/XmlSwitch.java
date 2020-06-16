/*
   Copyright 2013, 2016, 2019, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;


/**
 * Selects an exitState, based on either the content of the input message, by means
 * of a XSLT-stylesheet, the content of a session variable or, by default, by returning the name of the root-element.
 * 
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>&lt;name of the root-element&gt;</td><td>default</td></tr>
 * <tr><td>&lt;result of transformation&gt</td><td>when {@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename} or {@link #setXpathExpression(String) xpathExpression} is specified</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 */
public class XmlSwitch extends AbstractPipe {

	public static final String XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT = "Switch: Forward Found";
	public static final String XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT = "Switch: Forward Not Found";
	private static final String DEFAULT_SERVICESELECTION_XPATH = XmlUtils.XPATH_GETROOTNODENAME;

	private String serviceSelectionStylesheetFilename=null;
	private String xpathExpression=null;
	private String namespaceDefs = null; 
	private String sessionKey=null;
	private String notFoundForwardName=null;
	private String emptyForwardName=null;
	private int xsltVersion=0; // set to 0 for auto detect.

	private TransformerPool transformerPool=null;

	/**
	 * If no {@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename} is specified, the
	 * switch uses the root node. 
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getNotFoundForwardName()!=null) {
			if (findForward(getNotFoundForwardName())==null){
				ConfigurationWarnings.add(this, log, "has a notFoundForwardName attribute. However, this forward ["+getNotFoundForwardName()+"] is not configured.");
			}
		}
		if (getEmptyForwardName()!=null) {
			if (findForward(getEmptyForwardName())==null){
				ConfigurationWarnings.add(this, log, "has a emptyForwardName attribute. However, this forward ["+getEmptyForwardName()+"] is not configured.");
			}
		}

		if (StringUtils.isNotEmpty(getXpathExpression())) {
			if (!StringUtils.isEmpty(getServiceSelectionStylesheetFilename())) {
				throw new ConfigurationException(getLogPrefix(null) + "cannot have both an xpathExpression and a serviceSelectionStylesheetFilename specified");
			}
			transformerPool = TransformerPool.configureTransformer0(getLogPrefix(null), getConfigurationClassLoader(), getNamespaceDefs(), getXpathExpression(), null, "text", false, getParameterList(), 0);
		} 
		else {
			if (!StringUtils.isEmpty(getServiceSelectionStylesheetFilename())) {
				try {
					Resource stylesheet = Resource.getResource(getConfigurationClassLoader(), getServiceSelectionStylesheetFilename());
					if (stylesheet==null) {
						throw new ConfigurationException(getLogPrefix(null) + "cannot find stylesheet ["+getServiceSelectionStylesheetFilename()+"]");
					}
					transformerPool = TransformerPool.getInstance(stylesheet, getXsltVersion());
				} catch (IOException e) {
					throw new ConfigurationException(getLogPrefix(null) + "cannot retrieve ["+ serviceSelectionStylesheetFilename + "]", e);
				} catch (TransformerConfigurationException te) {
					throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from file [" + serviceSelectionStylesheetFilename + "]", te);
				}
			} else {
				if (StringUtils.isEmpty(getSessionKey())) {
					try {
						// create a transformer that looks to the root node 
						transformerPool = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(DEFAULT_SERVICESELECTION_XPATH, "text"));
					} catch (TransformerConfigurationException te) {
						throw new ConfigurationException(getLogPrefix(null) + "got error creating XPathEvaluator from string [" + DEFAULT_SERVICESELECTION_XPATH + "]", te);
					}
				}
			}
		}
		registerEvent(XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT);
		registerEvent(XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT);
	}
	
	@Override
	public void start() throws PipeStartException {
		super.start();
		if (transformerPool!=null) {
			try {
				transformerPool.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start TransformerPool", e);
			}
		}
	}
	
	@Override
	public void stop() {
		super.stop();
		if (transformerPool!=null) {
			transformerPool.close();
		}
	}


	/**
	 * This is where the action takes place, the switching is done. Pipes may only throw a PipeRunException,
	 * to be handled by the caller of this object.<br/>
	 * As WebLogic has the problem that when an non-well formed XML stream is given to
	 * weblogic.xerces the transformer gets corrupt, on an exception the configuration is done again, so that the
	 * transformer is re-initialized.
	 */
	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		String forward="";
		PipeForward pipeForward = null;

		if (transformerPool!=null) {
			ParameterList parameterList = null;
			try {
				Map<String,Object> parametervalues = null;
				parameterList =  getParameterList();
				if (parameterList!=null) {
					message.preserve();
					parametervalues = parameterList.getValues(message, session, isNamespaceAware()).getValueMap();
				}
				if (StringUtils.isNotEmpty(getSessionKey())) {
					forward = transformerPool.transform(Message.asMessage(session.get(sessionKey)), parametervalues);
				} else {
					message.preserve();
					forward = transformerPool.transform(message, parametervalues);
				}
			} catch (Throwable e) {
				throw new PipeRunException(this, getLogPrefix(session) + "got exception on transformation", e);
			}
		} else {
			try {
				if (StringUtils.isNotEmpty(getSessionKey())) {
					forward = Message.asString(session.get(sessionKey));
				} else {
					forward = message.asString();
				}
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
			}
		}

		log.debug(getLogPrefix(session)+ "determined forward ["+forward+"]");

		
		if (StringUtils.isEmpty(forward) && getEmptyForwardName()!=null) {
			throwEvent(XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT);
			pipeForward=findForward(getEmptyForwardName());
		} else {
			
			if (findForward(forward) != null) {
				throwEvent(XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT);
				pipeForward=findForward(forward);
			}
			else {
				log.info(getLogPrefix(session)+"determined forward ["+forward+"], which is not defined. Will use ["+getNotFoundForwardName()+"] instead");
				throwEvent(XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT);
				pipeForward=findForward(getNotFoundForwardName());
			}
		}
		
		if (pipeForward==null) {
			throw new PipeRunException (this, getLogPrefix(session)+"cannot find forward or pipe named ["+forward+"]");
		}
		return new PipeRunResult(pipeForward, message);
	}
	

	@IbisDoc({"1", "stylesheet may return a string representing the forward to look up", "<i>a stylesheet that returns the name of the root-element</i>"})
	public void setServiceSelectionStylesheetFilename(String newServiceSelectionStylesheetFilename) {
		serviceSelectionStylesheetFilename = newServiceSelectionStylesheetFilename;
	}
	public String getServiceSelectionStylesheetFilename() {
		return serviceSelectionStylesheetFilename;
	}
	
	@IbisDoc({"2", "xpath-expression that returns a string representing the forward to look up. It's possible to refer to a parameter (which e.g. contains a value from a sessionkey) by using the parameter name prefixed with $", ""})
	public void setXpathExpression(String xpathExpression) {
		this.xpathExpression = xpathExpression;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}

	@IbisDoc({"3", "Namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions. For some use other cases (NOT xpathExpression), one entry can be without a prefix, that will define the default namespace.", ""})
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
	public String getNamespaceDefs() {
		return namespaceDefs;
	}

	@IbisDoc({"4", "Name of the key in the <code>PipeLineSession</code> to retrieve the input message from, if a serviceSelectionStylesheetFilename or a xpathExpression is specified. " + 
					"If no serviceSelectionStylesheetFilename or xpathExpression is specified, the value of the session variable is used as the name of the forward. " + 
					"If none of sessionKey, serviceSelectionStylesheetFilename or xpathExpression are specified, the element name of the root node of the input message is taken as the name of forward.", ""})
	public void setSessionKey(String sessionKey){
		this.sessionKey = sessionKey;
	}
	public String getSessionKey(){
		return sessionKey;
	}

	@IbisDoc({"5", "Forward returned when the pipename derived from the stylesheet could not be found.", ""})
	public void setNotFoundForwardName(String notFound){
		notFoundForwardName=notFound;
	}
	public String getNotFoundForwardName(){
		return notFoundForwardName;
	}

	@IbisDoc({"6", "Forward returned when the content, on which the switch is performed, is empty. if <code>emptyforwardname</code> is not specified, <code>notfoundforwardname</code> is used.", ""})
	public void setEmptyForwardName(String empty){
		emptyForwardName=empty;
	}
	public String getEmptyForwardName(){
		return emptyForwardName;
	}

	
	@IbisDoc({"7", "If set to <code>2</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan). <code>0</code> will auto detect", "0"})
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}
	public int getXsltVersion() {
		return xsltVersion;
	}

	@IbisDoc({"when set <code>true</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan)", "false"})
	@Deprecated
	@ConfigurationWarning("Its value is now auto detected. If necessary, replace with a setting of xsltVersion")
	public void setXslt2(boolean b) {
		xsltVersion=b?2:1;
	}
}
