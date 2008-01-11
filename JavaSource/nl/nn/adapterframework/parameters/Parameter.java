/*
 * $Log: Parameter.java,v $
 * Revision 1.26  2008-01-11 09:45:50  europe\L190409
 * added type domdoc
 *
 * Revision 1.25  2007/10/08 13:31:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.24  2007/08/03 09:08:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved tp configuration to TransformerPool
 *
 * Revision 1.23  2007/06/14 08:49:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for parameter type=number
 *
 * Revision 1.22  2007/05/24 11:52:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.21  2007/05/24 09:54:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed parsing of date-types
 *
 * Revision 1.20  2007/05/16 11:45:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for date & type types, (first use: jdbc)
 *
 * Revision 1.19  2007/05/09 09:26:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed node-support
 *
 * Revision 1.18  2007/05/08 15:59:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added type=node support
 *
 * Revision 1.17  2007/02/12 13:59:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.16  2006/11/06 08:19:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added value-attribute as fixed-value
 *
 * Revision 1.15  2005/10/24 09:59:24  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.14  2005/10/17 11:43:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * namespace-awareness configurable
 *
 * Revision 1.13  2005/08/11 14:57:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved handling of default values for empty transformation results
 *
 * Revision 1.12  2005/06/02 11:45:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed version string
 *
 * Revision 1.11  2005/06/02 11:44:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

/**
 * Generic parameter definition.
 * 
 * A parameter resembles an attribute. However, while attributes get their value at configuration-time,
 * parameters get their value at the time of processing the message. Value can be retrieved from the message itself,
 * a fixed value, or from the pipelineSession. If this does not result in a value (or if neither of these is specified), a default value 
 * can be specified. If an XPathExpression or stylesheet is specified, it will be applied to the message, the value retrieved
 * from the pipelineSession or the fixed value specified.
 * <br/>
 * * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>name of the class, mostly a class that extends this class</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setType(String) type}</td><td>
 * <ul>
 * 	<li><code>string</code>: renders the contents of the first node (in combination with xslt or xpath)</li>
 * 	<li><code>xml</code>:  renders a xml-nodeset as an xml-string (in combination with xslt or xpath)</li>
 * 	<li><code>node</code>: renders the contents of the first node as a nodeset that can be used as such when passed as xslt-parameter (only for XSLT 1.0). Please note that the nodeset may contain multiple nodes, without a common root node</li>
 * 	<li><code>domdoc</code>: renders xml as a DOM document; similar to <code>node</code> with the distinction that there is always a common root node (required for XSLT 2.0)</li>
 * 	<li><code>date</code>: converts the result to a Date, by default using formatString <code>yyyy-MM-dd</code>. When applied as a JDBC parameter, the method setDate() is used</li>
 * 	<li><code>time</code>: converts the result to a Date, by default using formatString <code>HH:mm:ss</code>. When applied as a JDBC parameter, the method setTime() is used</li>
 * 	<li><code>datetime</code>: converts the result to a Date, by default using formatString <code>yyyy-MM-dd HH:mm:ss</code>. When applied as a JDBC parameter, the method setTimestamp() is used</li>
 * 	<li><code>number</code>: converts the result to a Number, using decimalSeparator and groupingSeparator. When applied as a JDBC parameter, the method setDouble() is used</li>
 * </ul>
 * </td><td>string</td></tr>
 * <tr><td>{@link #setFormatString(String) formatString}</td><td>used in combination with types <code>date</code>, <code>time</code> and <code>datetime</code></td><td>depends on type</td></tr>
 * <tr><td>{@link #setDecimalSeparator(String) decimalSeparator}</td><td>used in combination with type <code>number</code></td><td>system default</td></tr>
 * <tr><td>{@link #setGroupingSeparator(String) groupingSeparator}</td><td>used in combination with type <code>number</code></td><td>system default</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>&nbsp;</td><td>Key of a PipeLineSession-variable. Is specified, the value of the PipeLineSession variable is used as input for the XpathExpression or Stylesheet, instead of the current input message. If no xpathExpression or Stylesheet are specified, the value itself is returned.</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>The xpath expression. </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>Reference to a resource with the stylesheet</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultValue(String) defaultValue}</td><td>If the result of sessionKey, XpathExpressen and/or Stylesheet returns null or an empty String, this value is returned</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPattern(String) pattern}</td><td>Value of parameter is determined using substitution and formating. If fname is a parameter or session variable that resolves to Eric, then the pattern 'Hi {fname}, hoe gaat het?' resolves to 'Hi Eric, hoe gaat het?' </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setValue(String) value}</td><td>A fixed value</td><td>&nbsp;</td></tr>
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
 * N.B. to obtain a fixed value: a non-existing 'dummy' <code>sessionKey</code> in combination with the fixed value in <code>DefaultValue</code> is used traditionally.
 * The current version of parameter supports the 'value' attribute, that is sufficient to set a fixed value.    
 * @author Gerrit van Brakel
 */
public class Parameter implements INamedObject, IWithParameters {
	public static final String version="$RCSfile: Parameter.java,v $ $Revision: 1.26 $ $Date: 2008-01-11 09:45:50 $";
	protected Logger log = LogUtil.getLogger(this);

	public final static String TYPE_XML="xml";
	public final static String TYPE_NODE="node";
	public final static String TYPE_DOMDOC="domdoc";
	public final static String TYPE_DATE="date";
	public final static String TYPE_TIME="time";
	public final static String TYPE_DATETIME="datetime";
	public final static String TYPE_NUMBER="number";
	
	public final static String TYPE_DATE_PATTERN="yyyy-MM-dd";
	public final static String TYPE_TIME_PATTERN="HH:mm:ss";
	public final static String TYPE_DATETIME_PATTERN="yyyy-MM-dd HH:mm:ss";

	private String name = null;
	private String type = null;
	private String sessionKey = null;
	private String xpathExpression = null; 
	private String styleSheetName = null;
	private String pattern = null; 
	private String defaultValue = null;
	private String value = null;
	private String formatString = null;
	private String decimalSeparator = null;
	private String groupingSeparator = null;

	private DecimalFormatSymbols decimalFormatSymbols = null;
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
		if (StringUtils.isNotEmpty(getXpathExpression()) || 
		    StringUtils.isNotEmpty(styleSheetName)) {
			if (paramList!=null) {
				paramList.configure();
			}
			String outputType=TYPE_XML.equalsIgnoreCase(getType()) ||
							  TYPE_NODE.equalsIgnoreCase(getType()) || 
							  TYPE_DOMDOC.equalsIgnoreCase(getType())?"xml":"text";
			boolean includeXmlDeclaration=false;
			
			transformerPool=TransformerPool.configureTransformer("Parameter ["+getName()+"]",getXpathExpression(), styleSheetName,outputType,includeXmlDeclaration,paramList);
	    } else {
			if (paramList!=null && StringUtils.isEmpty(getXpathExpression())) {
				throw new ConfigurationException("Parameter ["+getName()+"] can only have parameters itself if a styleSheetName or xpathExpression is specified");
			}
	    }
		if (TYPE_DATE.equals(getType()) & StringUtils.isEmpty(getFormatString())) {
			setFormatString(TYPE_DATE_PATTERN);
		}
		if (TYPE_DATETIME.equals(getType()) & StringUtils.isEmpty(getFormatString())) {
			setFormatString(TYPE_DATETIME_PATTERN);
		}
		if (TYPE_TIME.equals(getType()) & StringUtils.isEmpty(getFormatString())) {
			setFormatString(TYPE_TIME_PATTERN);
		}
		if (TYPE_NUMBER.equals(getType())) {
			decimalFormatSymbols = new DecimalFormatSymbols();
			if (StringUtils.isNotEmpty(getDecimalSeparator())) {
				decimalFormatSymbols.setDecimalSeparator(getDecimalSeparator().charAt(0));
			}
			if (StringUtils.isNotEmpty(getGroupingSeparator())) {
				decimalFormatSymbols.setGroupingSeparator(getGroupingSeparator().charAt(0));
			}
		}
		configured = true;
	}

	private Object transform(Source xmlSource, ParameterResolutionContext prc) throws ParameterException, TransformerException, IOException {
		TransformerPool pool = getTransformerPool();
		if (TYPE_NODE.equals(getType()) || TYPE_DOMDOC.equals(getType())) {
			
			DOMResult transformResult = new DOMResult();
			pool.transform(xmlSource,transformResult,prc.getValueMap(paramList));
			Node result=transformResult.getNode();
			if (result!=null && TYPE_NODE.equals(getType())) {
				result=result.getFirstChild();
			}			
			if (log.isDebugEnabled()) { if (result!=null) log.debug("Returning Node result ["+result.getClass().getName()+"]["+result+"]: "+ ToStringBuilder.reflectionToString(result)); } 
			return result;

		} else {
			return pool.transform(xmlSource,prc.getValueMap(paramList));
		}
	}

	/**
	 * determines the raw value 
	 * @param p
	 * @return the raw value as object
	 * @throws IbisException
	 */
	Object getValue(ParameterValueList alreadyResolvedParameters, ParameterResolutionContext prc) throws ParameterException {
		Object result = null;
		log.debug("Calculating value for Parameter ["+getName()+"]");
		if (!configured) {
			throw new ParameterException("Parameter ["+getName()+"] not configured");
		}
		
		TransformerPool pool = getTransformerPool();
		if (pool != null) {
			try {
				Object transformResult=null;
				Source source=null;
				if (StringUtils.isNotEmpty(getValue())) {
					source = XmlUtils.stringToSourceForSingleUse(getValue(), prc.isNamespaceAware());
				} else if (StringUtils.isNotEmpty(getSessionKey())) {
					String sourceString = (String)(prc.getSession().get(getSessionKey()));
					if (StringUtils.isNotEmpty(sourceString)) {
						log.debug("Parameter ["+getName()+"] using sessionvariable ["+getSessionKey()+"] as source for transformation");
						source = XmlUtils.stringToSourceForSingleUse(sourceString, prc.isNamespaceAware());
					} else {
						log.debug("Parameter ["+getName()+"] sessionvariable ["+getSessionKey()+"] empty, no transformation will be performed");
					}
				} else if (StringUtils.isNotEmpty(getPattern())) {
					String sourceString = format(alreadyResolvedParameters, prc);
					if (StringUtils.isNotEmpty(sourceString)) {
						log.debug("Parameter ["+getName()+"] using pattern ["+getPattern()+"] as source for transformation");
						source = XmlUtils.stringToSourceForSingleUse(sourceString, prc.isNamespaceAware());
					} else {
						log.debug("Parameter ["+getName()+"] pattern ["+getPattern()+"] empty, no transformation will be performed");
					}
				} else {
					source = prc.getInputSource();
				}
				if (source!=null) {
					transformResult = transform(source,prc);
				}
				if (!(transformResult instanceof String) || StringUtils.isNotEmpty((String)transformResult)) {
						result = transformResult;
				}
			} catch (Exception e) {
				throw new ParameterException("Parameter ["+getName()+"] exception on transformation to get parametervalue", e);
			}
		} else {
			if (StringUtils.isNotEmpty(getSessionKey())) {
				result=prc.getSession().get(getSessionKey());
			} else if (StringUtils.isNotEmpty(getPattern())) {
				result=format(alreadyResolvedParameters, prc); 								
			} else if (StringUtils.isNotEmpty(getValue())) {
				result = getValue();
			} else {
				result=prc.getInput();
			}
		}
		if (result != null) {
			if (log.isDebugEnabled()) log.debug("Parameter ["+getName()+"] resolved to ["+result+"]");
		} else {
			// if value is null then return specified default value
			log.debug("Parameter ["+getName()+"] resolved to defaultvalue ["+getDefaultValue()+"]");
			result=getDefaultValue();
		}
		if (result !=null && result instanceof String) {
			if (TYPE_NODE.equals(getType())) {
				try {
					result=XmlUtils.buildNode((String)result,prc.isNamespaceAware());
					if (log.isDebugEnabled()) log.debug("final result ["+result.getClass().getName()+"]["+result+"]");
				} catch (DomBuilderException e) {
					throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+result+"] to XML nodeset",e);
				}
			}
			if (TYPE_DOMDOC.equals(getType())) {
				try {
					result=XmlUtils.buildDomDocument((String)result,prc.isNamespaceAware());
					if (log.isDebugEnabled()) log.debug("final result ["+result.getClass().getName()+"]["+result+"]");
				} catch (DomBuilderException e) {
					throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+result+"] to XML document",e);
				}
			}
			if (TYPE_DATE.equals(getType()) || TYPE_DATETIME.equals(getType()) || TYPE_TIME.equals(getType())) {
				log.debug("Parameter ["+getName()+"] converting result ["+result+"] to date using formatString ["+getFormatString()+"]" );
				DateFormat df = new SimpleDateFormat(getFormatString());
				try {
					result = df.parseObject((String)result);
				} catch (ParseException e) {
					throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+result+"] to Date using formatString ["+getFormatString()+"]",e);
				}
			}
			if (TYPE_NUMBER.equals(getType())) {
				log.debug("Parameter ["+getName()+"] converting result ["+result+"] to number decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]" );
				DecimalFormat df = new DecimalFormat();
				df.setDecimalFormatSymbols(decimalFormatSymbols);
				try {
					Number n = df.parse((String)result);
					result = n;
				} catch (ParseException e) {
					throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+result+"] to number decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]",e);
				}
			}
		}
		return result; 
	}

	private String format(ParameterValueList alreadyResolvedParameters, ParameterResolutionContext prc) throws ParameterException {
		int startNdx = -1;
		int endNdx = 0;

		// replace the named parameter with numbered parameters
		StringBuffer formatPattern = new StringBuffer();
		List params = new ArrayList();
		int paramPosition = 0;
		while(endNdx != -1) {
			// get name of parameter in pattern to be substituted 
			startNdx = pattern.indexOf("{", endNdx);
			if (startNdx == -1) {
				formatPattern.append(pattern.substring(endNdx));
				break;
			}
			else if (endNdx != -1) {
				formatPattern.append(pattern.substring(endNdx, startNdx));
			}
			int tmpEndNdx = pattern.indexOf("}", startNdx);
			endNdx = pattern.indexOf(",", startNdx);
			if (endNdx == -1 || endNdx > tmpEndNdx) {
				endNdx = tmpEndNdx;
			}
			if (endNdx == -1) {
				throw new ParameterException(new ParseException("Bracket is not closed", startNdx));
			}
			String substitutionName = pattern.substring(startNdx + 1, endNdx);
			
			// get value
			Object substitutionValue = getValueForFormatting(alreadyResolvedParameters, prc, substitutionName);
			params.add(substitutionValue);
			formatPattern.append('{').append(paramPosition++);
		}
		
		return MessageFormat.format(formatPattern.toString(), params.toArray());
	}
	
	private Object getValueForFormatting(ParameterValueList alreadyResolvedParameters, ParameterResolutionContext prc, String name) throws ParameterException {
		ParameterValue paramValue = alreadyResolvedParameters.getParameterValue(name);
		Object substitutionValue = paramValue == null ? null : paramValue.getValue();
		  
		if (substitutionValue == null) {
			substitutionValue = prc.getSession().get(name);
		}
		if (substitutionValue == null) {
			if ("now".equals(name.toLowerCase())) {
				substitutionValue = new Date();
			}
			else if ("uid".equals(name.toLowerCase())) {
				substitutionValue = Misc.createSimpleUUID();
			}
		}
		if (substitutionValue == null) {
			throw new ParameterException("Parameter with name [" + name + "] in pattern" + pattern + " can not be resolved");
		}
		return substitutionValue;		
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

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public String toString() {
		return "name=["+name+"] defaultValue=["+defaultValue+"] sessionKey=["+sessionKey+"]" + "xpathExpression=["+xpathExpression+ "]" + "type=["+type+ "]" + "value=["+value+ "]";

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


	/**
	 * @param string with pattern to be used, follows MessageFormat syntax with named parameters
	 */
	public void setPattern(String string) {
		pattern = string;
	}
	public String getPattern() {
		return pattern;
	}

	public void setFormatString(String string) {
		formatString = string;
	}
	public String getFormatString() {
		return formatString;
	}

	public void setDecimalSeparator(String string) {
		decimalSeparator = string;
	}
	public String getDecimalSeparator() {
		return decimalSeparator;
	}

	public void setGroupingSeparator(String string) {
		groupingSeparator = string;
	}
	public String getGroupingSeparator() {
		return groupingSeparator;
	}
}
