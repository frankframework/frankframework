/*
 * $Log: Parameter.java,v $
 * Revision 1.1  2004-10-05 09:51:17  L190409
 * moved to package parameters
 * added getValue()
 *
 * Revision 1.5  2004/08/26 09:04:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added log keyword in comment
 * made implement INamedObject
 *
 */
package nl.nn.adapterframework.parameters;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.ParameterException;
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
	public static final String version="$Id: Parameter.java,v 1.1 2004-10-05 09:51:17 L190409 Exp $";

	private String name = null;
	private String type = null;
	private String defaultValue = null;
	private String sessionKey = null;
	private String xpathExpression = null;
	private TransformerPool transformerPool = null;


	/**
	 * determines the raw value 
	 * @param p
	 * @return the raw value as object
	 * @throws IbisException
	 */
	public Object getValue(ParameterResolutionContext r) throws ParameterException {
		Object result = null;
		if (StringUtils.isNotEmpty(getSessionKey())) {
			result=r.getSession().get(getSessionKey());
		}
		else if (getTransformerPool() != null) {
			Transformer t;
			try {
				t = getTransformerPool().getTransformer();
				try {
					result = XmlUtils.transformXml(t, r.getInputSource());
				} catch (Exception e) {
					try {
						getTransformerPool().invalidateTransformer(t);
					} catch (Exception ee) {
						// ignore silently...
					}
					throw new ParameterException("Paremeter ["+getName()+"] exception on transformation to get paramtervalue", e);
				}
			} catch (TransformerConfigurationException e) {
				throw new ParameterException("Paremeter ["+getName()+"] caught exception while obtaining transformer to extract parameter value", e);
			}
		}
		// if value is null then return specified default value
		return (result == null) ? getDefaultValue() : result; 
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
	public void setXpathExpression(String xpathExpression) throws TransformerException {
		this.xpathExpression = xpathExpression;
		if (this.xpathExpression != null)
			this.transformerPool = new TransformerPool(XmlUtils.createXPathEvaluatorSource(xpathExpression));
	}

}
