package nl.nn.adapterframework.core;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.util.XmlUtils;

/**
 * Generic parameter definition
 * 
 * @author Richard Punt
 * @version Id
 */
public class Parameter {

	/**
	 * Default constructor 
	 */
	public Parameter() {
		super();
	}

	private String name = null;
	private String type = null;
	private String defaultValue = null;
	private String sessionKey = null;
	private String xpathExpression = null;
	private Transformer transformer = null;

	public void setName(String rhs) {
		name = rhs;
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

	Transformer getTransformer() {
		return transformer;
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
			this.transformer = XmlUtils.createXPathEvaluator(xpathExpression);
	}

}
