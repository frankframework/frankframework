/*
 * $Log: Parameter.java,v $
 * Revision 1.11  2005-06-02 11:44:48  europe\L190409
 * return current input value if no xslt, xpath or sessionkey are specified
 *
 * Revision 1.10  2005/04/26 09:33:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * different handling of empy transform-result, so that it is null instead of an empty string
 *
 * Revision 1.9  2005/03/07 11:10:05  Johan Verrips <johan.verrips@ibissource.org>
 * Javadoc geupdate
 *
 * Revision 1.8  2005/01/13 08:08:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Xslt parameter handling by Maps instead of by Ibis parameter system
 *
 * Revision 1.7  2004/10/25 08:32:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * parameters for parameters
 *
 * Revision 1.6  2004/10/19 15:27:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved transformation to pool
 *
 * Revision 1.5  2004/10/19 13:50:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * dual action parameters: allows to use session-variable as transformer-input
 *
 * Revision 1.4  2004/10/19 08:09:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * omit xml declaration
 *
 * Revision 1.3  2004/10/14 16:04:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.2  2004/10/12 15:06:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added asCollection() method
 *
 * Revision 1.1  2004/10/05 09:51:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved to package parameters
 * added getValue()
 *
 * Revision 1.5  2004/08/26 09:04:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added log keyword in comment
 * made implement INamedObject
 *
 */
package nl.nn.adapterframework.parameters;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Generic parameter definition.
 * 
 * A parameter resembles an attribute. However, while attributes get their value at configuration-time,
 * parameters get their value at the time of processing the message. Value can be retrieved from the message itself,
 * or from the pipelineSession. If this does not result in a value (or if neither of these is specified), a default value 
 * can be specified. If an XPathExpression or stylesheet is specified, it will be applied to the message or the value retrieved
 * from the pipelineSession.
 * <br/>
 * * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>name of the class, mostly a class that extends this class</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setType(String) type}</td><td>"string" or "xml". "xml" renders a xml-nodeset as an xml-string; "string" renders the contents of the first node</td><td>string</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>&nbsp;</td><td>Key of a PipeLineSession-variable. Is specified, the value of the PipeLineSession variable is used as input for the XpathExpression or Stylesheet, instead of the current input message. If no xpathExpression or Stylesheet are specified, the value itself is returned.</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>The xpath expression. </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>Reference to a respirce with the stylesheet</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultValue(String) defaultValue}</td><td>If the result of sessionKey, XpathExpressen and/or Stylesheet returns null or an empty String, this value is returned</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * Examples:
 * <pre>
 * 
 * stored under SessionKey 'TransportInfo':
 *  &lt;transportinfo&gt;
 *   &lt;to&gt;***@zonnet.nl&lt;/to&gt;
 *   &lt;to&gt;***@zonnet.nl&lt;/to&gt;
 *   &lt;cc&gt;***@zonnet.nl&lt;/cc&gt;
 *  &lt;/transportinfo&gt;
 * 
 * to obtain all 'to' addressees as a parameter:
 * sessionKey="TransportInfo"
 * xpathExpression="transportinfo/to"
 * type="xml"
 * 
 * Result:
 *   &lt;to&gt;***@zonnet.nl&lt;/to&gt;
 *   &lt;to&gt;***@zonnet.nl&lt;/to&gt;
 * </pre>
 * 
 * N.B. to obtain a fixed value: use a non-existing 'dummy' <code>sessionKey</code> in combination with the fixed value in <code>DefaultValue</code>.  
 * @author Richard Punt / Gerrit van Brakel
 */
public class Parameter implements INamedObject, IWithParameters {
	public static final String version="$Id: Parameter.java,v 1.11 2005-06-02 11:44:48 europe\L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());

	private String name = null;
	private String type = null;
	private String sessionKey = null;
	private String xpathExpression = null; 
	private String styleSheetName = null; 
	private String defaultValue = null;

	private TransformerPool transformerPool = null;
	protected ParameterList paramList = null;
	private boolean configured = false;

	public void addParameter(Parameter p) { 
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getXpathExpression())) {
			if (StringUtils.isNotEmpty(styleSheetName)) {
				throw new ConfigurationException("Parameter ["+getName()+"] cannot have both an xpathExpression and a styleSheetName specified");
			}
			try {
				String xsltSource;
				if ("xml".equalsIgnoreCase(getType())) {
					xsltSource = XmlUtils.createXPathEvaluatorSource("",getXpathExpression(),"xml", false); 
				} else {
					xsltSource = XmlUtils.createXPathEvaluatorSource(getXpathExpression(),"text");
				}
				transformerPool = new TransformerPool(xsltSource);
			} 
			catch (TransformerConfigurationException te) {
				throw new ConfigurationException("Parameter ["+getName()+"] got error creating transformer from xpathExpression [" + getXpathExpression() + "]", te);
			}
		} 
		if (StringUtils.isNotEmpty(styleSheetName)) {
			URL styleSheetUrl = ClassUtils.getResourceURL(this, styleSheetName); 
			try {
				transformerPool = new TransformerPool(styleSheetUrl);
			} catch (IOException e) {
				throw new ConfigurationException("Parameter ["+getName()+"] cannot retrieve ["+ styleSheetName + "]", e);
			} catch (TransformerConfigurationException te) {
				throw new ConfigurationException("Parameter ["+getName()+"] got error creating transformer from [" + styleSheetUrl.toString() + "]", te);
			}
			if (paramList!=null) {
				paramList.configure();
			}
		}  else {
			if (paramList!=null) {
				throw new ConfigurationException("Parameter ["+getName()+"] can only have parameters itself if a styleSheetName is specified");
			}
		}
		configured = true;
	}

	/**
	 * determines the raw value 
	 * @param p
	 * @return the raw value as object
	 * @throws IbisException
	 */
	public Object getValue(ParameterResolutionContext prc) throws ParameterException {
		Object result = null;
		log.debug("Calculating value for Parameter ["+getName()+"]");
		if (!configured) {
			throw new ParameterException("Parameter ["+getName()+"] not configured");
		}
		
		TransformerPool pool = getTransformerPool();
		if (pool != null) {
			try {
				if (StringUtils.isNotEmpty(getSessionKey())) {
					String source = (String)(prc.getSession().get(getSessionKey()));
					if (StringUtils.isNotEmpty(source)) {
						log.debug("Parameter ["+getName()+"] using sessionvariable ["+getSessionKey()+"] as source for transformation");
						result = pool.transform(source,null);
					} else {
						log.debug("Parameter ["+getName()+"] sessionvariable ["+getSessionKey()+"] empty, no transformation will be performed");
					}
				} else {
					String transformResult = pool.transform(prc.getInputSource(),prc.getValueMap(paramList));
					if (StringUtils.isNotEmpty(transformResult)) {
						result = transformResult;
					}
				}
			} catch (Exception e) {
				throw new ParameterException("Parameter ["+getName()+"] exception on transformation to get parametervalue", e);
			}
		} else {
			if (StringUtils.isNotEmpty(getSessionKey())) {
				result=prc.getSession().get(getSessionKey());
			} else {
				result=prc.getInput();
			}
		}
		if (result != null) {
			log.debug("Parameter ["+getName()+"] resolved to ["+result+"]");
			return result;
		}
		log.debug("Parameter ["+getName()+"] resolved to defaultvalue ["+getDefaultValue()+"]");
		// if value is null then return specified default value
		return getDefaultValue(); 
	}


	public void setName(String parameterName) {
		name = parameterName;
	}

	public String getName() {
		return name;
	}

	public void setDefaultValue(String rhs) {
		defaultValue = rhs;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	TransformerPool getTransformerPool() {
		return transformerPool;
	}

	public void setSessionKey(String rhs) {
		sessionKey = rhs;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public String toString() {
		return "name=["+name+"] defaultValue=["+defaultValue+"] sessionKey=["+sessionKey+"]" + "xpathExpression=["+xpathExpression+ "]"  + "type=["+type+ "]";

	}

	/**
	 * @return type of the parameter
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the xpath expression to extract the parameter value from the (xml formatted) input
	 */
	public String getXpathExpression() {
		return xpathExpression;
	}

	/**
	 * @param type of the parameter
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param xpathExpression to extract the parameter value from the (xml formatted) input 
	 */
	public void setXpathExpression(String xpathExpression) {
		this.xpathExpression = xpathExpression;
	}

	/**
	 * Specify the stylesheet to use
	 */
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}

}
