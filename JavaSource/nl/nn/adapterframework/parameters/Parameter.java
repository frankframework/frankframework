/*
 * $Log: Parameter.java,v $
 * Revision 1.2  2004-10-12 15:06:48  L190409
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

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
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
 * can be specified.
 * 
 * @author Richard Punt / Gerrit van Brakel
 */
public class Parameter implements INamedObject {
	public static final String version="$Id: Parameter.java,v 1.2 2004-10-12 15:06:48 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());

	private String name = null;
	private String type = null;
	private String sessionKey = null;
	private String xpathExpression = null; 
	private String styleSheetName = null; 
	private String defaultValue = null;

	private TransformerPool transformerPool = null;
	private boolean configured = false;

	public void configure() throws ConfigurationException {
		if (!StringUtils.isEmpty(getSessionKey())) {
			if (!StringUtils.isEmpty(getXpathExpression()) || !StringUtils.isEmpty(styleSheetName)) {
				throw new ConfigurationException("Parameter ["+getName()+"] can have only one of sessionKey ["+getSessionKey()+"], xpathExpression ["+getXpathExpression()+"] and styleSheetName ["+styleSheetName+"]");
			}
		} else {
			if (!StringUtils.isEmpty(getXpathExpression())) {
				if (!StringUtils.isEmpty(styleSheetName)) {
					throw new ConfigurationException("Parameter ["+getName()+"] cannot have both an xpathExpression and a styleSheetName specified");
				}
				try {
					String xsltSource;
					if ("xml".equalsIgnoreCase(getType())) {
						xsltSource = XmlUtils.createXPathEvaluatorSource(getXpathExpression(),"xml"); 
					} else {
						xsltSource = XmlUtils.createXPathEvaluatorSource(getXpathExpression(),"text"); 
					}
					transformerPool = new TransformerPool(xsltSource);
				} 
				catch (TransformerConfigurationException te) {
					throw new ConfigurationException("Parameter ["+getName()+"] got error creating transformer from xpathExpression [" + getXpathExpression() + "]", te);
				}
			} 
			if (!StringUtils.isEmpty(styleSheetName)) {
				URL styleSheetUrl = ClassUtils.getResourceURL(this, styleSheetName); 
				try {
					transformerPool = new TransformerPool(styleSheetUrl);
				} catch (IOException e) {
					throw new ConfigurationException("Parameter ["+getName()+"] cannot retrieve ["+ styleSheetName + "]", e);
				} catch (TransformerConfigurationException te) {
					throw new ConfigurationException("Parameter ["+getName()+"] got error creating transformer from [" + styleSheetUrl.toString() + "]", te);
				}
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
	public Object getValue(ParameterResolutionContext r) throws ParameterException {
		Object result = null;
		if (!configured) {
			throw new ParameterException("Parameter ["+getName()+"] not configured");
		}
		if (StringUtils.isNotEmpty(getSessionKey())) {
			result=r.getSession().get(getSessionKey());
		}
		else {
			TransformerPool pool = getTransformerPool();
			if (pool != null) {
				Transformer t;
				try {
					t = pool.getTransformer();
					try {
						result = XmlUtils.transformXml(t, r.getInputSource());
					} catch (Exception e) {
						try {
							pool.invalidateTransformer(t);
						} catch (Exception ee) {
							// ignore silently...
						}
						throw new ParameterException("Parameter ["+getName()+"] exception on transformation to get paramtervalue", e);
					} finally {
						pool.releaseTransformer(t);
					}
				} catch (TransformerConfigurationException e) {
					throw new ParameterException("Parameter ["+getName()+"] caught exception while obtaining transformer to extract parameter value", e);
				}
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
