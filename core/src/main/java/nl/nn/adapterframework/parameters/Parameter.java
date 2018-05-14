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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.pipes.PutSystemDateInSession;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

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
 * 	<li><code>integer</code>: converts the result to an Integer</li>
 * 	<li><code>inputstream</code>: only applicable as a JDBC parameter, the method setBinaryStream() is used</li>
 * 	<li><code>list</code>: converts a List&lt;String&gt; object to a xml-string (&lt;items&gt;&lt;item&gt;...&lt;/item&gt;&lt;item&gt;...&lt;/item&gt;&lt;/items&gt;)</li>
 * 	<li><code>map</code>: converts a Map&lt;String, String&gt; object to a xml-string (&lt;items&gt;&lt;item name="..."&gt;...&lt;/item&gt;&lt;item name="..."&gt;...&lt;/item&gt;&lt;/items&gt;)</li>
 * </ul>
 * </td><td>string</td></tr>
 * <tr><td>{@link #setFormatString(String) formatString}</td><td>used in combination with types <code>date</code>, <code>time</code> and <code>datetime</code></td><td>depends on type</td></tr>
 * <tr><td>{@link #setDecimalSeparator(String) decimalSeparator}</td><td>used in combination with type <code>number</code></td><td>system default</td></tr>
 * <tr><td>{@link #setGroupingSeparator(String) groupingSeparator}</td><td>used in combination with type <code>number</code></td><td>system default</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>Key of a PipeLineSession-variable. Is specified, the value of the PipeLineSession variable is used as input for the XpathExpression or Stylesheet, instead of the current input message. If no xpathExpression or Stylesheet are specified, the value itself is returned. If the value "*" is specified, all existing sessionKeys are added as parameter of which the name starts with the name of this parameter. If also the name of the parameter has the value "*" then all existing sessionKeys are added as parameter (except "tsReceived")</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSessionKeyXPath(String) sessionKeyXPath}</td><td>Instead of a fixed <code>sessionKey</code> it's also possible to use a xpath expression to extract the name of the <code>sessionKey</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>The xpath expression to extract the parameter value from the (xml formatted) input or session-variable.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceDefs(String) namespaceDefs}</td><td>namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>URL to a stylesheet that wil be applied to the contents of the message or the value of the session-variable.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveNamespaces(boolean) removeNamespaces}</td><td>when set <code>true</code> namespaces (and prefixes) in the input message are removed before the stylesheet/xpathExpression is executed</td><td>false</td></tr>
 * <tr><td>{@link #setDefaultValue(String) defaultValue}</td><td>If the result of sessionKey, XpathExpressen and/or Stylesheet returns null or an empty String, this value is returned</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultValueMethods(String) defaultValueMethods}</td><td>Comma separated list of methods (defaultValue, sessionKey, pattern, value or input) to use as default value. Used in the order they appear until a non-null value is found.</td><td>defaultValue</td></tr>
 * <tr><td>{@link #setPattern(String) pattern}</td><td>Value of parameter is determined using substitution and formating. The expression can contain references to session-variables or other parameters using {name-of-parameter} and is formatted using java.text.MessageFormat. {now}, {uid}, {uuid}, {hostname} and {fixeddate} are named constants that can be used in the expression. If fname is a parameter or session variable that resolves to Eric, then the pattern 'Hi {fname}, hoe gaat het?' resolves to 'Hi Eric, hoe gaat het?'. A guid can be generated using {hostname}_{uid}, see also <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/rmi/server/UID.html">http://java.sun.com/j2se/1.4.2/docs/api/java/rmi/server/UID.html</a> for more information about (g)uid's or <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/util/UUID.html">http://docs.oracle.com/javase/1.5.0/docs/api/java/util/UUID.html</a> for more information about uuid's.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setValue(String) value}</td><td>A fixed value</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHidden(boolean) hidden}</td><td>if set to <code>true</code>, the value of the parameter will not be shown in the log (replaced by asterisks)</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setMinLength(int) minLength}</td><td>if set (>=0) and the length of the value of the parameter deceeds this minimum length, the value is padded</td><td>-1</td></tr>
 * <tr><td>{@link #setMaxLength(int) maxLength}</td><td>if set (>=0) and the length of the value of the parameter exceeds this maximum length, the length is trimmed to this maximum length</td><td>-1</td></tr>
 * <tr><td>{@link #setMinInclusive(String) minInclusive}</td><td>used in combination with type <code>number</code>; if set and the value of the parameter exceeds this minimum value, this minimum value is taken</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxInclusive(String) maxInclusive}</td><td>used in combination with type <code>number</code>; if set and the value of the parameter exceeds this maximum value, this maximum value is taken</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXslt2(boolean) xslt2}</td><td>(applicable for xpathExpression and styleSheetName) when set <code>true</code> XSLT processor 2.0 (net.sf.saxon) will be used, otherwise XSLT processor 1.0 (org.apache.xalan)</td><td>false</td></tr>
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
	protected Logger log = LogUtil.getLogger(this);
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

	public final static String TYPE_XML="xml";
	public final static String TYPE_NODE="node";
	public final static String TYPE_DOMDOC="domdoc";
	public final static String TYPE_DATE="date";
	public final static String TYPE_TIME="time";
	public final static String TYPE_DATETIME="datetime";
	public final static String TYPE_TIMESTAMP="timestamp";
	public final static String TYPE_XMLDATETIME="xmldatetime";
	public final static String TYPE_NUMBER="number";
	public final static String TYPE_INTEGER="integer";
	public final static String TYPE_INPUTSTREAM="inputstream";
	public final static String TYPE_LIST="list";
	public final static String TYPE_MAP="map";
	
	public final static String TYPE_DATE_PATTERN="yyyy-MM-dd";
	public final static String TYPE_TIME_PATTERN="HH:mm:ss";
	public final static String TYPE_DATETIME_PATTERN="yyyy-MM-dd HH:mm:ss";
	public final static String TYPE_TIMESTAMP_PATTERN=DateUtils.FORMAT_FULL_GENERIC;

	public final static String FIXEDUID ="0a1b234c--56de7fa8_9012345678b_-9cd0";
	public final static String FIXEDHOSTNAME ="MYHOST000012345";
	
	private String name = null;
	private String type = null;
	private String sessionKey = null;
	private String sessionKeyXPath = null;
	private String xpathExpression = null; 
	private String namespaceDefs = null;
	private String styleSheetName = null;
	private String pattern = null;
	private String defaultValue = null;
	private String defaultValueMethods = "defaultValue";
	private String value = null;
	private String formatString = null;
	private String decimalSeparator = null;
	private String groupingSeparator = null;
	private int minLength = -1;
	private int maxLength = -1;
	private String minInclusiveString = null;
	private String maxInclusiveString = null;
	private Number minInclusive;
	private Number maxInclusive;
	private boolean hidden = false;
	private boolean removeNamespaces=false;
	private boolean xslt2=false;

	private DecimalFormatSymbols decimalFormatSymbols = null;
	private TransformerPool transformerPool = null;
	private TransformerPool transformerPoolRemoveNamespaces;
	private TransformerPool transformerPoolSessionKey = null;
	protected ParameterList paramList = null;
	private boolean configured = false;

	@Override
	public void addParameter(Parameter p) { 
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getSessionKey()) && 
			    StringUtils.isNotEmpty(getSessionKeyXPath())) {
			throw new ConfigurationException("Parameter ["+getName()+"] cannot have both sessionKey and sessionKeyXPath specified");
		}
		if (StringUtils.isNotEmpty(getXpathExpression()) || 
		    StringUtils.isNotEmpty(styleSheetName)) {
			if (paramList!=null) {
				paramList.configure();
			}
			String outputType=TYPE_XML.equalsIgnoreCase(getType()) ||
							  TYPE_NODE.equalsIgnoreCase(getType()) || 
							  TYPE_DOMDOC.equalsIgnoreCase(getType())?"xml":"text";
			boolean includeXmlDeclaration=false;
			
			transformerPool=TransformerPool.configureTransformer0("Parameter ["+getName()+"] ",classLoader,getNamespaceDefs(),getXpathExpression(), styleSheetName,outputType,includeXmlDeclaration,paramList,isXslt2());
	    } else {
			if (paramList!=null && StringUtils.isEmpty(getXpathExpression())) {
				throw new ConfigurationException("Parameter ["+getName()+"] can only have parameters itself if a styleSheetName or xpathExpression is specified");
			}
	    }
		if (isRemoveNamespaces()) {
			String removeNamespaces_xslt = XmlUtils.makeRemoveNamespacesXslt(true,false);
			try {
				transformerPoolRemoveNamespaces = TransformerPool.getInstance(removeNamespaces_xslt);
			} catch (TransformerConfigurationException te) {
				throw new ConfigurationException("Got error creating transformer from removeNamespaces", te);
			}
		}
		if (StringUtils.isNotEmpty(getSessionKeyXPath())) {
			transformerPoolSessionKey = TransformerPool.configureTransformer("SessionKey for parameter ["+getName()+"] ", classLoader, getNamespaceDefs(), getSessionKeyXPath(), null,"text",false,null);
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
	 * @param alreadyResolvedParameters
	 * @return the raw value as object
	 * @throws IbisException
	 */
	public Object getValue(ParameterValueList alreadyResolvedParameters, ParameterResolutionContext prc) throws ParameterException {
		Object result = null;
		log.debug("Calculating value for Parameter ["+getName()+"]");
		if (!configured) {
			throw new ParameterException("Parameter ["+getName()+"] not configured");
		}
		
		String retrievedSessionKey;
		if (transformerPoolSessionKey != null) {
			try {
				retrievedSessionKey = transformerPoolSessionKey.transform(prc.getInput(), null);
			} catch (Exception e) {
				throw new ParameterException("SessionKey for parameter ["+getName()+"] exception on transformation to get name", e);
			}
		} else {
			retrievedSessionKey = getSessionKey();
		}
		
		TransformerPool pool = getTransformerPool();
		if (pool != null) {
			try {
				Object transformResult=null;
				Source source=null;
				if (StringUtils.isNotEmpty(getValue())) {
					source = XmlUtils.stringToSourceForSingleUse(getValue(), prc.isNamespaceAware());
				} else if (StringUtils.isNotEmpty(retrievedSessionKey)) {
					String sourceString;
					Object sourceObject = prc.getSession().get(retrievedSessionKey);
					if (TYPE_LIST.equals(getType())
							&& sourceObject instanceof List) {
						List<String> items = (List<String>) sourceObject;
						XmlBuilder itemsXml = new XmlBuilder("items");
						for (Iterator<String> it = items.iterator(); it.hasNext();) {
							String item = it.next();
							XmlBuilder itemXml = new XmlBuilder("item");
							itemXml.setValue(item);
							itemsXml.addSubElement(itemXml);
						}
						sourceString = itemsXml.toXML();
					} else if (TYPE_MAP.equals(getType())
								&& sourceObject instanceof Map) {
						Map<String, String> items = (Map<String, String>) sourceObject;
						XmlBuilder itemsXml = new XmlBuilder("items");
						for (Iterator<String> it = items.keySet().iterator(); it.hasNext();) {
							String item = it.next();
							XmlBuilder itemXml = new XmlBuilder("item");
							itemXml.addAttribute("name", item);
							itemXml.setValue(items.get(item));
							itemsXml.addSubElement(itemXml);
						}
						sourceString = itemsXml.toXML();
					} else {
						sourceString = (String) sourceObject;
					}
					if (StringUtils.isNotEmpty(sourceString)) {
						log.debug("Parameter ["+getName()+"] using sessionvariable ["+retrievedSessionKey+"] as source for transformation");
						source = XmlUtils.stringToSourceForSingleUse(sourceString, prc.isNamespaceAware());
					} else {
						log.debug("Parameter ["+getName()+"] sessionvariable ["+retrievedSessionKey+"] empty, no transformation will be performed");
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
			if (StringUtils.isNotEmpty(retrievedSessionKey)) {
				result=prc.getSession().get(retrievedSessionKey);
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
			StringTokenizer stringTokenizer = new StringTokenizer(getDefaultValueMethods(), ",");
			while (result == null && stringTokenizer.hasMoreElements()) {
				String token = stringTokenizer.nextToken();
				if ("defaultValue".equals(token)) {
					result = getDefaultValue();
				} else if ("sessionKey".equals(token)) {
					result = prc.getSession().get(retrievedSessionKey);
				} else if ("pattern".equals(token)) {
					result = format(alreadyResolvedParameters, prc);
				} else if ("value".equals(token)) {
					result = getValue();
				} else if ("input".equals(token)) {
					result = prc.getInput();
				}
			}
			log.debug("Parameter ["+getName()+"] resolved to defaultvalue ["+(isHidden()?hide(result.toString()):result)+"]");
		}
		if (result !=null && result instanceof String) {
			if (getMinLength()>=0 && !TYPE_NUMBER.equals(getType())) {
				if (result.toString().length()<getMinLength()) {
					log.debug("Padding parameter ["+getName()+"] because length ["+result.toString().length()+"] deceeds minLength ["+getMinLength()+"]" );
					result = StringUtils.rightPad(result.toString(), getMinLength());
				}
			}
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
				if (getMinLength()>=0 && result.toString().length()<getMinLength()) {
					log.debug("Adding leading zeros to parameter ["+getName()+"]" );
					result = StringUtils.leftPad(result.toString(), getMinLength(), '0');
				}
			}
			if (TYPE_INTEGER.equals(getType())) {
				log.debug("Parameter ["+getName()+"] converting result ["+result+"] to integer" );
				try {
					Integer i = Integer.parseInt((String)result);
					result = i;
				} catch (NumberFormatException e) {
					throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+result+"] to integer",e);
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
		String hiddenString;
		if (string == null) {
			hiddenString = null;
		} else {
			hiddenString = "";
			for (int i = 0; i < string.toString().length(); i++) {
				hiddenString = hiddenString + "*";
			}
		}
		return hiddenString;
	}

	private String format(ParameterValueList alreadyResolvedParameters, ParameterResolutionContext prc) throws ParameterException {
		int startNdx = -1;
		int endNdx = 0;

		// replace the named parameter with numbered parameters
		StringBuffer formatPattern = new StringBuffer();
		List<Object> params = new ArrayList<Object>();
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
			} else if ("uuid".equals(name.toLowerCase())) {
				substitutionValue = Misc.createRandomUUID();
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
			} else if ("fixeduid".equals(name.toLowerCase())) {
				if (!ConfigurationUtils.stubConfiguration()) {
					throw new ParameterException("Parameter pattern [" + name + "] only allowed in stub mode");
				}
				substitutionValue = FIXEDUID;
			} else if ("fixedhostname".equals(name.toLowerCase())) {
				if (!ConfigurationUtils.stubConfiguration()) {
					throw new ParameterException("Parameter pattern [" + name + "] only allowed in stub mode");
				}
				substitutionValue = FIXEDHOSTNAME;
			}
		}
		if (substitutionValue == null) {
			throw new ParameterException("Parameter with name [" + name + "] in pattern" + pattern + " can not be resolved");
		}
		return substitutionValue;		
	}

	@Override
	public void setName(String parameterName) {
		name = parameterName;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setDefaultValue(String string) {
		defaultValue = string;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValueMethods(String string) {
		defaultValueMethods = string;
	}

	public String getDefaultValueMethods() {
		return defaultValueMethods;
	}

	TransformerPool getTransformerPool() {
		return transformerPool;
	}

	public void setSessionKey(String string) {
		sessionKey = string;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKeyXPath(String string) {
		sessionKeyXPath = string;
	}

	public String getSessionKeyXPath() {
		return sessionKeyXPath;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "Parameter name=["+name+"] defaultValue=["+defaultValue+"] sessionKey=["+sessionKey+"] sessionKeyXPath=["+sessionKeyXPath+"] xpathExpression=["+xpathExpression+ "] type=["+type+ "] value=["+value+ "]";
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

	public void setMinLength(int i) {
		minLength = i;
	}
	public int getMinLength() {
		return minLength;
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

	public void setXslt2(boolean b) {
		xslt2 = b;
	}
	public boolean isXslt2() {
		return xslt2;
	}
}
