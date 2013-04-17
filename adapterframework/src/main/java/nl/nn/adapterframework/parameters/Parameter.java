/*
   Copyright 2013 Nationale-Nederlanden

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
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.pipes.PutSystemDateInSession;
import nl.nn.adapterframework.util.DateUtils;
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
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the parameter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setType(String) type}</td><td>
 * <ul>
 * 	<li><code>string</code>: renders the contents of the first node (in combination with xslt or xpath)</li>
 * 	<li><code>xml</code>:  renders a xml-nodeset as an xml-string (in combination with xslt or xpath)</li>
 * 	<li><code>node</code>: renders the CONTENTS of the first node as a nodeset that can be used as such when passed as xslt-parameter (only for XSLT 1.0). Please note that the nodeset may contain multiple nodes, without a common root node. N.B. The result is the set of children of what you might expect it to be...</li>
 * 	<li><code>domdoc</code>: renders xml as a DOM document; similar to <code>node</code> with the distinction that there is always a common root node (required for XSLT 2.0)</li>
 * 	<li><code>date</code>: converts the result to a Date, by default using formatString <code>yyyy-MM-dd</code>. When applied as a JDBC parameter, the method setDate() is used</li>
 * 	<li><code>time</code>: converts the result to a Date, by default using formatString <code>HH:mm:ss</code>. When applied as a JDBC parameter, the method setTime() is used</li>
 * 	<li><code>datetime</code>: converts the result to a Date, by default using formatString <code>yyyy-MM-dd HH:mm:ss</code>. When applied as a JDBC parameter, the method setTimestamp() is used</li>
 * 	<li><code>timestamp</code>: similar to datetime, except for the formatString that is <code>yyyy-MM-dd HH:mm:ss.SSS</code> by default</li>
 * 	<li><code>xmldatetime</code>: converts the result from a XML dateTime to a Date. When applied as a JDBC parameter, the method setTimestamp() is used</li>
 * 	<li><code>number</code>: converts the result to a Number, using decimalSeparator and groupingSeparator. When applied as a JDBC parameter, the method setDouble() is used</li>
 * </ul>
 * </td><td>string</td></tr>
 * <tr><td>{@link #setFormatString(String) formatString}</td><td>used in combination with types <code>date</code>, <code>time</code> and <code>datetime</code></td><td>depends on type</td></tr>
 * <tr><td>{@link #setDecimalSeparator(String) decimalSeparator}</td><td>used in combination with type <code>number</code></td><td>system default</td></tr>
 * <tr><td>{@link #setGroupingSeparator(String) groupingSeparator}</td><td>used in combination with type <code>number</code></td><td>system default</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>Key of a PipeLineSession-variable. Is specified, the value of the PipeLineSession variable is used as input for the XpathExpression or Stylesheet, instead of the current input message. If no xpathExpression or Stylesheet are specified, the value itself is returned.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>The xpath expression to extract the parameter value from the (xml formatted) input or session-variable.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceDefs(String) namespaceDefs}</td><td>namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>URL to a stylesheet that wil be applied to the contents of the message or the value of the session-variable.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveNamespaces(boolean) removeNamespaces}</td><td>when set <code>true</code> namespaces (and prefixes) in the input message are removed before the stylesheet/xpathExpression is executed</td><td>false</td></tr>
 * <tr><td>{@link #setDefaultValue(String) defaultValue}</td><td>If the result of sessionKey, XpathExpressen and/or Stylesheet returns null or an empty String, this value is returned</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPattern(String) pattern}</td><td>Value of parameter is determined using substitution and formating. The expression can contain references to session-variables or other parameters using {name-of-parameter} and is formatted using java.text.MessageFormat. {now}, {uid}, {hostname} and {fixeddate} are named constants that can be used in the expression. If fname is a parameter or session variable that resolves to Eric, then the pattern 'Hi {fname}, hoe gaat het?' resolves to 'Hi Eric, hoe gaat het?'. A guid can be generated using {hostname}_{uid}, see also <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/rmi/server/UID.html">http://java.sun.com/j2se/1.4.2/docs/api/java/rmi/server/UID.html</a> for more information about (g)uid's.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setValue(String) value}</td><td>A fixed value</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHidden(boolean) hidden}</td><td>if set to <code>true</code>, the value of the parameter will not be shown in the log (replaced by asterisks)</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setMaxLength(String) maxLength}</td><td>if set (>=0) and the length of the value of the parameter exceeds this maximum length, the length is trimmed to this maximum length</td><td>-1</td></tr>
 * <tr><td>{@link #setMinInclusive(String) minInclusive}</td><td>used in combination with type <code>number</code>; if set and the value of the parameter exceeds this minimum value, this minimum value is taken</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxInclusive(String) maxInclusive}</td><td>used in combination with type <code>number</code>; if set and the value of the parameter exceeds this maximum value, this maximum value is taken</td><td>&nbsp;</td></tr>
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
	public static final String version="$RCSfile: Parameter.java,v $ $Revision: 1.47 $ $Date: 2012-12-13 10:41:59 $";
	protected Logger log = LogUtil.getLogger(this);

	public final static String TYPE_XML="xml";
	public final static String TYPE_NODE="node";
	public final static String TYPE_DOMDOC="domdoc";
	public final static String TYPE_DATE="date";
	public final static String TYPE_TIME="time";
	public final static String TYPE_DATETIME="datetime";
	public final static String TYPE_TIMESTAMP="timestamp";
	public final static String TYPE_XMLDATETIME="xmldatetime";
	public final static String TYPE_NUMBER="number";
	
	public final static String TYPE_DATE_PATTERN="yyyy-MM-dd";
	public final static String TYPE_TIME_PATTERN="HH:mm:ss";
	public final static String TYPE_DATETIME_PATTERN="yyyy-MM-dd HH:mm:ss";
	public final static String TYPE_TIMESTAMP_PATTERN=DateUtils.FORMAT_FULL_GENERIC;

	private String name = null;
	private String type = null;
	private String sessionKey = null;
	private String xpathExpression = null; 
	private String namespaceDefs = null; 
	private String styleSheetName = null;
	private String pattern = null; 
	private String defaultValue = null;
	private String value = null;
	private String formatString = null;
	private String decimalSeparator = null;
	private String groupingSeparator = null;
	private int maxLength = -1;
	private String minInclusiveString = null;
	private String maxInclusiveString = null;
	private Number minInclusive;
	private Number maxInclusive;
	private boolean hidden = false;
	private boolean removeNamespaces=false;

	private DecimalFormatSymbols decimalFormatSymbols = null;
	private TransformerPool transformerPool = null;
	private TransformerPool transformerPoolRemoveNamespaces;
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
			
			transformerPool=TransformerPool.configureTransformer("Parameter ["+getName()+"] ",getNamespaceDefs(),getXpathExpression(), styleSheetName,outputType,includeXmlDeclaration,paramList);
	    } else {
			if (paramList!=null && StringUtils.isEmpty(getXpathExpression())) {
				throw new ConfigurationException("Parameter ["+getName()+"] can only have parameters itself if a styleSheetName or xpathExpression is specified");
			}
	    }
		if (isRemoveNamespaces()) {
			String removeNamespaces_xslt = XmlUtils.makeRemoveNamespacesXslt(true,false);
			try {
				transformerPoolRemoveNamespaces = new TransformerPool(removeNamespaces_xslt);
			} catch (TransformerConfigurationException te) {
				throw new ConfigurationException("Got error creating transformer from removeNamespaces", te);
			}
		}
		if (TYPE_DATE.equals(getType()) && StringUtils.isEmpty(getFormatString())) {
			setFormatString(TYPE_DATE_PATTERN);
		}
		if (TYPE_DATETIME.equals(getType()) && StringUtils.isEmpty(getFormatString())) {
			setFormatString(TYPE_DATETIME_PATTERN);
		}
		if (TYPE_TIMESTAMP.equals(getType()) && StringUtils.isEmpty(getFormatString())) {
			setFormatString(TYPE_TIMESTAMP_PATTERN);
		}
		if (TYPE_TIME.equals(getType()) && StringUtils.isEmpty(getFormatString())) {
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

		if (getMinInclusive()!=null || getMaxInclusive()!=null) {
			if (!TYPE_NUMBER.equals(getType())) {
				throw new ConfigurationException("minInclusive and minInclusive only allowed in combination with type ["+TYPE_NUMBER+"]");
			}
			if (getMinInclusive()!=null) {
				DecimalFormat df = new DecimalFormat();
				df.setDecimalFormatSymbols(decimalFormatSymbols);
				try {
					minInclusive = df.parse(getMinInclusive());
				} catch (ParseException e) {
					throw new ConfigurationException("Attribute [minInclusive] could not parse result ["+getMinInclusive()+"] to number decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]",e);
				}
			}
			if (getMaxInclusive()!=null) {
				DecimalFormat df = new DecimalFormat();
				df.setDecimalFormatSymbols(decimalFormatSymbols);
				try {
					maxInclusive = df.parse(getMaxInclusive());
				} catch (ParseException e) {
					throw new ConfigurationException("Attribute [maxInclusive] could not parse result ["+getMinInclusive()+"] to number decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]",e);
				}
			}
		}
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
	public Object getValue(ParameterValueList alreadyResolvedParameters, ParameterResolutionContext prc) throws ParameterException {
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
					if (transformerPoolRemoveNamespaces != null) {
						String rnResult = transformerPoolRemoveNamespaces.transform(source, null);
						source = XmlUtils.stringToSource(rnResult);
					}
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
			if (log.isDebugEnabled()) {
				log.debug("Parameter ["+getName()+"] resolved to ["+(isHidden()?hide(result.toString()):result)+"]");
			}
		} else {
			// if value is null then return specified default value
			log.debug("Parameter ["+getName()+"] resolved to defaultvalue ["+(isHidden()?hide(getDefaultValue()):getDefaultValue())+"]");
			result=getDefaultValue();
		}
		if (result !=null && result instanceof String) {
			if (getMaxLength()>=0) {
				if (result.toString().length()>getMaxLength()) {
					log.debug("Trimming parameter ["+getName()+"] because length ["+result.toString().length()+"] exceeds maxLength ["+getMaxLength()+"]" );
					result = result.toString().substring(0, getMaxLength());
				}
			}
			if (TYPE_NODE.equals(getType())) {
				try {
					result=XmlUtils.buildNode((String)result,prc. isNamespaceAware());
					if (log.isDebugEnabled()) log.debug("final result ["+result.getClass().getName()+"]["+result+"]");
				} catch (DomBuilderException e) {
					throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+result+"] to XML nodeset",e);
				}
			}
			if (TYPE_DOMDOC.equals(getType())) {
				try {
					result=XmlUtils.buildDomDocument((String)result,prc.isNamespaceAware(),prc.isXslt2());
					if (log.isDebugEnabled()) log.debug("final result ["+result.getClass().getName()+"]["+result+"]");
				} catch (DomBuilderException e) {
					throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+result+"] to XML document",e);
				}
			}
			if (TYPE_DATE.equals(getType()) || TYPE_DATETIME.equals(getType()) || TYPE_TIMESTAMP.equals(getType()) || TYPE_TIME.equals(getType())) {
				log.debug("Parameter ["+getName()+"] converting result ["+result+"] to date using formatString ["+getFormatString()+"]" );
				DateFormat df = new SimpleDateFormat(getFormatString());
				try {
					result = df.parseObject((String)result);
				} catch (ParseException e) {
					throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+result+"] to Date using formatString ["+getFormatString()+"]",e);
				}
			}
			if (TYPE_XMLDATETIME.equals(getType())) {
				log.debug("Parameter ["+getName()+"] converting result ["+result+"] from xml dateTime to date" );
				result = DateUtils.parseXmlDateTime((String)result);
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
		if (result !=null) {
			if (getMinInclusive()!=null || getMaxInclusive()!=null) {
				if (getMinInclusive()!=null) {
					if (((Number)result).floatValue()<minInclusive.floatValue()) {
						log.debug("Replacing parameter ["+getName()+"] because value ["+result+"] exceeds minInclusive ["+getMinInclusive()+"]" );
						result = minInclusive;
					}
				}
				if (getMaxInclusive()!=null) {
					if (((Number)result).floatValue()>maxInclusive.floatValue()) {
						log.debug("Replacing parameter ["+getName()+"] because value ["+result+"] exceeds maxInclusive ["+getMaxInclusive()+"]" );
						result = maxInclusive;
					}
				}
			}
		}
		
		return result; 
	}

	private String hide(String string) {
		String hiddenString = "";
		for (int i = 0; i < string.toString().length(); i++) {
			hiddenString = hiddenString + "*";
		}
		return hiddenString;
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
			} else if ("uid".equals(name.toLowerCase())) {
				substitutionValue = Misc.createSimpleUUID();
			} else if ("hostname".equals(name.toLowerCase())) {
				substitutionValue = Misc.getHostname();
			} else if ("fixeddate".equals(name.toLowerCase())) {
				if (!ConfigurationUtils.stubConfiguration()) {
					throw new ParameterException("Parameter pattern [" + name + "] only allowed in stub mode");
				}
				Date d;
				SimpleDateFormat formatterFrom = new SimpleDateFormat(PutSystemDateInSession.FORMAT_FIXEDDATETIME);
				String fixedDateTime = (String)prc.getSession().get(PutSystemDateInSession.FIXEDDATE_STUB4TESTTOOL_KEY);
				if (StringUtils.isEmpty(fixedDateTime)) {
					fixedDateTime = PutSystemDateInSession.FIXEDDATETIME;
				}
				try {
					d = formatterFrom.parse(fixedDateTime);
				} catch (ParseException e) {
					throw new ParameterException("Cannot parse fixed date ["+PutSystemDateInSession.FIXEDDATETIME+"] with format ["+PutSystemDateInSession.FORMAT_FIXEDDATETIME+"]",e);
				}
				substitutionValue = d;
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
		return "Paramter name=["+name+"] defaultValue=["+defaultValue+"] sessionKey=["+sessionKey+"] xpathExpression=["+xpathExpression+ "] type=["+type+ "] value=["+value+ "]";
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

	public void setHidden(boolean b) {
		hidden = b;
	}
	public boolean isHidden() {
		return hidden;
	}

	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
	public String getNamespaceDefs() {
		return namespaceDefs;
	}

	public void setRemoveNamespaces(boolean b) {
		removeNamespaces = b;
	}
	public boolean isRemoveNamespaces() {
		return removeNamespaces;
	}

	public void setMaxLength(int i) {
		maxLength = i;
	}
	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxInclusive(String string) {
		maxInclusiveString = string;
	}
	public String getMaxInclusive() {
		return maxInclusiveString;
	}

	public void setMinInclusive(String string) {
		minInclusiveString = string;
	}
	public String getMinInclusive() {
		return minInclusiveString;
	}
}
