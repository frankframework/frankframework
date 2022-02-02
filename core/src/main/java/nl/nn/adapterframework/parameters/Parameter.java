/*
   Copyright 2013, 2016, 2019, 2020 Nationale-Nederlanden, 2021-2022 WeAreFrank!

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
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.PutSystemDateInSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
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
public class Parameter implements IConfigurable, IWithParameters {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private static final String TYPE_STRING = "string";
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
	public final static String TYPE_BOOLEAN="boolean";
	public final static String TYPE_INPUTSTREAM="inputstream";
	public final static String TYPE_LIST="list";
	public final static String TYPE_MAP="map";
	
	public final static String TYPE_DATE_PATTERN="yyyy-MM-dd";
	public final static String TYPE_TIME_PATTERN="HH:mm:ss";
	public final static String TYPE_DATETIME_PATTERN="yyyy-MM-dd HH:mm:ss";
	public final static String TYPE_TIMESTAMP_PATTERN=DateUtils.FORMAT_FULL_GENERIC;

	private static final List<String> TYPES_REQUIRE_CONVERSION = Arrays.asList(new String[] {TYPE_NODE, TYPE_DOMDOC, TYPE_DATE, TYPE_TIME, TYPE_DATETIME, TYPE_TIMESTAMP, TYPE_XMLDATETIME, TYPE_NUMBER, TYPE_INTEGER, TYPE_BOOLEAN});
	
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
	private String authAlias;
	private String userName;
	private String password;
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
	private int xsltVersion=0; // set to 0 for auto detect.

	private DecimalFormatSymbols decimalFormatSymbols = null;
	private TransformerPool transformerPool = null;
	private TransformerPool transformerPoolRemoveNamespaces;
	private TransformerPool transformerPoolSessionKey = null;
	protected ParameterList paramList = null;
	private boolean configured = false;
	private CredentialFactory cf;

	@Override
	public void addParameter(Parameter p) { 
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getSessionKey()) && StringUtils.isNotEmpty(getSessionKeyXPath())) {
			throw new ConfigurationException("Parameter ["+getName()+"] cannot have both sessionKey and sessionKeyXPath specified");
		}
		if (StringUtils.isNotEmpty(getXpathExpression()) || StringUtils.isNotEmpty(styleSheetName)) {
			if (paramList!=null) {
				paramList.configure();
			}
			String outputType=TYPE_XML.equalsIgnoreCase(getType()) || TYPE_NODE.equalsIgnoreCase(getType()) || TYPE_DOMDOC.equalsIgnoreCase(getType())?"xml":"text";
			boolean includeXmlDeclaration=false;
			
			transformerPool=TransformerPool.configureTransformer0("Parameter ["+getName()+"] ", this, getNamespaceDefs(),getXpathExpression(), styleSheetName,outputType,includeXmlDeclaration,paramList,getXsltVersion());
		} else {
			if (paramList!=null && StringUtils.isEmpty(getXpathExpression())) {
				throw new ConfigurationException("Parameter ["+getName()+"] can only have parameters itself if a styleSheetName or xpathExpression is specified");
			}
		}
		if (isRemoveNamespaces()) {
			transformerPoolRemoveNamespaces = XmlUtils.getRemoveNamespacesTransformerPool(true,false);
		}
		if (StringUtils.isNotEmpty(getSessionKeyXPath())) {
			transformerPoolSessionKey = TransformerPool.configureTransformer("SessionKey for parameter ["+getName()+"] ", this, getNamespaceDefs(), getSessionKeyXPath(), null,"text",false,null);
		}
		if(getType()==null) {
			log.info("parameter ["+getName()+" has no type. Setting the type to ["+TYPE_STRING+"]");
			setType(TYPE_STRING);
		}
		if(StringUtils.isEmpty(getFormatString())) {
			switch(getType()) {
				case TYPE_DATE:
					setFormatString(TYPE_DATE_PATTERN);
					break;
				case TYPE_DATETIME:
					setFormatString(TYPE_DATETIME_PATTERN);
					break;
				case TYPE_TIMESTAMP:
					setFormatString(TYPE_TIMESTAMP_PATTERN);
					break;
				case TYPE_TIME:
					setFormatString(TYPE_TIME_PATTERN);
					break;
				default:
					break;
			}
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
				throw new ConfigurationException("minInclusive and maxInclusive only allowed in combination with type ["+TYPE_NUMBER+"]");
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
					throw new ConfigurationException("Attribute [maxInclusive] could not parse result ["+getMaxInclusive()+"] to number decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]",e);
				}
			}
		}
		if (StringUtils.isNotEmpty(getAuthAlias()) || StringUtils.isNotEmpty(getUserName()) || StringUtils.isNotEmpty(getPassword())) {
			cf=new CredentialFactory(getAuthAlias(), getUserName(), getPassword());
		}
	}

	private Document transformToDocument(Source xmlSource, ParameterValueList pvl) throws ParameterException, TransformerException, IOException {
		TransformerPool pool = getTransformerPool();
		DOMResult transformResult = new DOMResult();
		pool.transform(xmlSource,transformResult, pvl);
		return (Document) transformResult.getNode();
	}

	public boolean requiresInputValueForResolution() {
		if (transformerPoolSessionKey != null) { //TODO: Check if this clause needs to go after the next one. Having a transformerpool on itself doesn't make it necessary to have the input.
			return true;
		}
		if ((StringUtils.isNotEmpty(getSessionKey()) || StringUtils.isNotEmpty(getValue()) || StringUtils.isNotEmpty(getPattern()))
				&& (StringUtils.isEmpty(getDefaultValueMethods()) || !getDefaultValueMethods().contains("input"))) {
			return false;
		}
		return true;
	}
 
	/**
	 * determines the raw value 
	 */
	public Object getValue(ParameterValueList alreadyResolvedParameters, Message message, IPipeLineSession session, boolean namespaceAware) throws ParameterException {
		Object result = null;
		log.debug("Calculating value for Parameter ["+getName()+"]");
		if (!configured) {
			throw new ParameterException("Parameter ["+getName()+"] not configured");
		}
		
		String requestedSessionKey;
		if (transformerPoolSessionKey != null) {
			try {
				requestedSessionKey = transformerPoolSessionKey.transform(message.asSource());
			} catch (Exception e) {
				throw new ParameterException("SessionKey for parameter ["+getName()+"] exception on transformation to get name", e);
			}
		} else {
			requestedSessionKey = getSessionKey();
		}
		TransformerPool pool = getTransformerPool();
		if (pool != null) {
			try {
				/*
				 * determine source for XSLT transformation from
				 * 1) value attribute
				 * 2) requestedSessionKey
				 * 3) pattern
				 * 4) input message
				 * 
				 * N.B. this order differs from untransformed parameters
				 */
				Source source=null;
				if (StringUtils.isNotEmpty(getValue())) {
					source = XmlUtils.stringToSourceForSingleUse(getValue(), namespaceAware);
				} else if (StringUtils.isNotEmpty(requestedSessionKey)) {
					Object sourceObject = session.get(requestedSessionKey);
					if (TYPE_LIST.equals(getType())	&& sourceObject instanceof List) {
						List<String> items = (List<String>) sourceObject;
						XmlBuilder itemsXml = new XmlBuilder("items");
						for (Iterator<String> it = items.iterator(); it.hasNext();) {
							String item = it.next();
							XmlBuilder itemXml = new XmlBuilder("item");
							itemXml.setValue(item);
							itemsXml.addSubElement(itemXml);
						}
						source = XmlUtils.stringToSourceForSingleUse(itemsXml.toXML(), namespaceAware);
					} else if (TYPE_MAP.equals(getType()) && sourceObject instanceof Map) {
						Map<String, String> items = (Map<String, String>) sourceObject;
						XmlBuilder itemsXml = new XmlBuilder("items");
						for (Iterator<String> it = items.keySet().iterator(); it.hasNext();) {
							String item = it.next();
							XmlBuilder itemXml = new XmlBuilder("item");
							itemXml.addAttribute("name", item);
							itemXml.setValue(items.get(item));
							itemsXml.addSubElement(itemXml);
						}
						source = XmlUtils.stringToSourceForSingleUse(itemsXml.toXML(), namespaceAware);
					} else {
						Message sourceMsg = Message.asMessage(sourceObject);
						if (!sourceMsg.isEmpty()) {
							log.debug("Parameter ["+getName()+"] using sessionvariable ["+requestedSessionKey+"] as source for transformation");
							source = sourceMsg.asSource();
						} else {
							log.debug("Parameter ["+getName()+"] sessionvariable ["+requestedSessionKey+"] empty, no transformation will be performed");
						}
					}
				} else if (StringUtils.isNotEmpty(getPattern())) {
					String sourceString = format(alreadyResolvedParameters, session);
					if (StringUtils.isNotEmpty(sourceString)) {
						log.debug("Parameter ["+getName()+"] using pattern ["+getPattern()+"] as source for transformation");
						source = XmlUtils.stringToSourceForSingleUse(sourceString, namespaceAware);
					} else {
						log.debug("Parameter ["+getName()+"] pattern ["+getPattern()+"] empty, no transformation will be performed");
					}
				} else {
					source = message.asSource();
				}
				if (source!=null) {
					if (transformerPoolRemoveNamespaces != null) {
						String rnResult = transformerPoolRemoveNamespaces.transform(source);
						source = XmlUtils.stringToSource(rnResult);
					}
					ParameterValueList pvl = paramList==null ? null : paramList.getValues(message, session, namespaceAware);
					switch (getType()) {
						case TYPE_NODE:
							return transformToDocument(source, pvl).getFirstChild();
						case TYPE_DOMDOC:
							return transformToDocument(source, pvl);
						default:
							String transformResult = pool.transform(source, pvl);
							if (StringUtils.isNotEmpty(transformResult)) {
								result = transformResult;
							}
							break;
					}
				}
			} catch (Exception e) {
				throw new ParameterException("Parameter ["+getName()+"] exception on transformation to get parametervalue", e);
			}
		} else {
			/*
			 * No XSLT transformation, determine primary result from
			 * 1) requestedSessionKey
			 * 2) pattern
			 * 3) value attribute
			 * 4) input message
			 * 
			 * N.B. this order differs from transformed parameters. 
			 */
			if (StringUtils.isNotEmpty(requestedSessionKey)) {
				result=session.get(requestedSessionKey);
				if (log.isDebugEnabled() && (result==null || 
					result instanceof String  && ((String)result).isEmpty() ||
					result instanceof Message && ((Message)result).isEmpty())) {
						log.debug("Parameter ["+getName()+"] session variable ["+requestedSessionKey+"] is empty");
				}
			} else if (StringUtils.isNotEmpty(getPattern())) {
				result=format(alreadyResolvedParameters, session);
			} else if (StringUtils.isNotEmpty(getValue())) {
				result = getValue();
			} else {
				try {
					if (message==null) {
						return null;
					}
					message.preserve();
					result=message.asString();
				} catch (IOException e) {
					throw new ParameterException(e);
				}
			}
		}
		if (result != null && result instanceof Message) {
			result = ((Message)result).asObject(); // avoid the IOException thrown by asString()
		}
		if (result != null) {
			if (log.isDebugEnabled()) log.debug("Parameter ["+getName()+"] resolved to ["+(isHidden()?hide(result.toString()):result)+"]");
		} else {
			// if result is null then return specified default value
			StringTokenizer stringTokenizer = new StringTokenizer(getDefaultValueMethods(), ",");
			while (result == null && stringTokenizer.hasMoreElements()) {
				String token = stringTokenizer.nextToken().trim();
				if ("defaultValue".equalsIgnoreCase(token)) {
					result = getDefaultValue();
				} else if ("sessionKey".equalsIgnoreCase(token)) {
					result = session.get(requestedSessionKey);
				} else if ("pattern".equalsIgnoreCase(token)) {
					result = format(alreadyResolvedParameters, session);
				} else if ("value".equalsIgnoreCase(token)) {
					result = getValue();
				} else if ("input".equalsIgnoreCase(token)) {
					try {
						message.preserve();
						result=message.asString();
					} catch (IOException e) {
						throw new ParameterException(e);
					}
				}
			}
			if (result!=null) {
				log.debug("Parameter ["+getName()+"] resolved to defaultvalue ["+(isHidden()?hide(result.toString()):result)+"]");
			}
		}
		if (result !=null && result instanceof String) {
			if (getMinLength()>=0 && !TYPE_NUMBER.equals(getType())) {
				if (((String)result).length()<getMinLength()) {
					log.debug("Padding parameter ["+getName()+"] because length ["+((String)result).length()+"] deceeds minLength ["+getMinLength()+"]" );
					result = StringUtils.rightPad(((String)result), getMinLength());
				}
			}
			if (getMaxLength()>=0) {
				if (((String)result).length()>getMaxLength()) {
					log.debug("Trimming parameter ["+getName()+"] because length ["+((String)result).length()+"] exceeds maxLength ["+getMaxLength()+"]" );
					result = ((String)result).substring(0, getMaxLength());
				}
			}
		}
		if(result != null && TYPES_REQUIRE_CONVERSION.contains(getType())) {
			result = getValueAsType(result, namespaceAware);
		}
		if (result !=null && result instanceof Number) {
			if (getMinInclusive()!=null && ((Number)result).floatValue() < minInclusive.floatValue()) {
				log.debug("Replacing parameter ["+getName()+"] because value ["+result+"] falls short of minInclusive ["+getMinInclusive()+"]" );
				result = minInclusive;
			}
			if (getMaxInclusive()!=null && ((Number)result).floatValue() > maxInclusive.floatValue()) {
				log.debug("Replacing parameter ["+getName()+"] because value ["+result+"] exceeds maxInclusive ["+getMaxInclusive()+"]" );
				result = maxInclusive;
			}
		}
		if (getType()==TYPE_NUMBER && getMinLength()>=0 && (result+"").length()<getMinLength()) {
			log.debug("Adding leading zeros to parameter ["+getName()+"]" );
			result = StringUtils.leftPad(result+"", getMinLength(), '0');
		}

		return result;
	}

	/** Converts raw data to configured parameter type */
	private Object getValueAsType(Object request, boolean namespaceAware) throws ParameterException {
		Message requestMessage = Message.asMessage(request);
		Object result = request;
		try {
			Object requestObject = requestMessage.asObject();
			switch(getType()) {
				case TYPE_NODE:
					try {
						if (transformerPoolRemoveNamespaces != null) {
							requestMessage = new Message(transformerPoolRemoveNamespaces.transform(requestMessage, null));
						}
						if(requestObject instanceof Document) {
							return ((Document)requestObject).getDocumentElement();
						}
						if(requestObject instanceof Node) {
							return requestObject;
						}
						result=XmlUtils.buildDomDocument(requestMessage.asInputSource(), namespaceAware).getDocumentElement();
						if (log.isDebugEnabled()) log.debug("final result ["+result.getClass().getName()+"]["+result+"]");
					} catch (DomBuilderException | TransformerException | IOException | SAXException e) {
						throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+requestMessage+"] to XML nodeset",e);
					}
					break;
				case TYPE_DOMDOC:
					try {
						if (transformerPoolRemoveNamespaces != null) {
							requestMessage = new Message(transformerPoolRemoveNamespaces.transform(requestMessage, null));
						}
						if(requestObject instanceof Document) {
							return requestObject;
						}
						result=XmlUtils.buildDomDocument(requestMessage.asInputSource(), namespaceAware);
						if (log.isDebugEnabled()) log.debug("final result ["+result.getClass().getName()+"]["+result+"]");
					} catch (DomBuilderException | TransformerException | IOException | SAXException e) {
						throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+requestMessage+"] to XML document",e);
					}
					break;
				case TYPE_DATE:
				case TYPE_DATETIME:
				case TYPE_TIMESTAMP:
				case TYPE_TIME:
					if(requestObject instanceof Date) {
						return requestObject;
					}
					log.debug("Parameter ["+getName()+"] converting result ["+requestMessage+"] to date using formatString ["+getFormatString()+"]" );
					DateFormat df = new SimpleDateFormat(getFormatString());
					try {
						result = df.parseObject(requestMessage.asString());
					} catch (ParseException e) {
						throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+requestMessage+"] to Date using formatString ["+getFormatString()+"]",e);
					}
					break;
				case TYPE_XMLDATETIME:
					if(requestObject instanceof Date) {
						return requestObject;
					}
					log.debug("Parameter ["+getName()+"] converting result ["+requestMessage+"] from xml dateTime to date" );
					result = DateUtils.parseXmlDateTime(requestMessage.asString());
					break;
				case TYPE_NUMBER:
					if(requestObject instanceof Number) {
						return requestObject;
					}
					log.debug("Parameter ["+getName()+"] converting result ["+requestMessage+"] to number decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]" );
					DecimalFormat decimalFormat = new DecimalFormat();
					decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);
					try {
						Number n = decimalFormat.parse(requestMessage.asString());
						result = n;
					} catch (ParseException e) {
						throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+requestMessage+"] to number decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]",e);
					}
					break;
				case TYPE_INTEGER:
					if(requestObject instanceof Integer) {
						return requestObject;
					}
					log.debug("Parameter ["+getName()+"] converting result ["+requestMessage+"] to integer" );
					try {
						Integer i = Integer.parseInt(requestMessage.asString());
						result = i;
					} catch (NumberFormatException e) {
						throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+requestMessage+"] to integer",e);
					}
					break;
				case TYPE_BOOLEAN:
					if(requestObject instanceof Boolean) {
						return requestObject;
					}
					log.debug("Parameter ["+getName()+"] converting result ["+requestMessage+"] to boolean" );
					Boolean i = Boolean.parseBoolean(requestMessage.asString());
					result = i;
					break;
				default:
					break;
			}
		} catch(IOException e) {
			throw new ParameterException("Could not convert parameter ["+getName()+"] to String", e);
		}

		return result;
	}

	private String hide(String string) {
		String hiddenString;
		if (string == null) {
			hiddenString = null;
		} else {
			hiddenString = "";
			for (int i = 0; i < string.length(); i++) {
				hiddenString = hiddenString + "*";
			}
		}
		return hiddenString;
	}

	private String format(ParameterValueList alreadyResolvedParameters, IPipeLineSession session) throws ParameterException {
		int startNdx = -1;
		int endNdx = 0;

		// replace the named parameter with numbered parameters
		StringBuilder formatPattern = new StringBuilder();
		List<Object> params = new ArrayList<>();
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
			String substitutionPattern = pattern.substring(startNdx + 1, tmpEndNdx);
			
			// get value
			Object substitutionValue = getValueForFormatting(alreadyResolvedParameters, session, substitutionPattern);
			params.add(substitutionValue);
			formatPattern.append('{').append(paramPosition++);
		}
		
		return MessageFormat.format(formatPattern.toString(), params.toArray());
	}

	private Object preFormatDateType(Object rawValue, String formatType) throws ParameterException {
		if (formatType!=null && (formatType.equalsIgnoreCase("date") || formatType.equalsIgnoreCase("time"))) {
			if (rawValue instanceof Date) {
				return rawValue;
			}
			DateFormat df = new SimpleDateFormat(StringUtils.isNotEmpty(getFormatString()) ? getFormatString() : DateUtils.FORMAT_GENERICDATETIME);
			try {
				return df.parse(Message.asString(rawValue));
			} catch (ParseException | IOException e) {
				throw new ParameterException("Cannot parse ["+rawValue+"] as date", e);
			}
		} 
		if (rawValue instanceof Date) {
			DateFormat df = new SimpleDateFormat(StringUtils.isNotEmpty(getFormatString()) ? getFormatString() : DateUtils.FORMAT_GENERICDATETIME);
			return df.format(rawValue);
		}
		try {
			return Message.asString(rawValue);
		} catch (IOException e) {
			throw new ParameterException("Cannot read date value ["+rawValue+"]", e);
		}
	}

	private Object getValueForFormatting(ParameterValueList alreadyResolvedParameters, IPipeLineSession session, String targetPattern) throws ParameterException {
		String[] patternElements = targetPattern.split(",");
		String name = patternElements[0].trim();
		String formatType = patternElements.length>1 ? patternElements[1].trim() : null;
		
		ParameterValue paramValue = alreadyResolvedParameters.getParameterValue(name);
		Object substitutionValue = paramValue == null ? null : paramValue.getValue();

		if (substitutionValue == null) {
			Message substitutionValueMessage = Message.asMessage(session.get(name));
			if (!substitutionValueMessage.isEmpty()) {
				if (substitutionValueMessage.asObject() instanceof Date) {
					substitutionValue = preFormatDateType(substitutionValueMessage.asObject(), formatType);
				} else {
					try {
						substitutionValue = substitutionValueMessage.asString();
					} catch (IOException e) {
						throw new ParameterException("Cannot get substitution value", e);
					}
				}
			}
		}
		if (substitutionValue == null) {
			String namelc=name.toLowerCase();
			if ("now".equals(name.toLowerCase())) {
				substitutionValue = preFormatDateType(new Date(), formatType);
			} else if ("uid".equals(namelc)) {
				substitutionValue = Misc.createSimpleUUID();
			} else if ("uuid".equals(namelc)) {
				substitutionValue = Misc.createRandomUUID();
			} else if ("hostname".equals(namelc)) {
				substitutionValue = Misc.getHostname();
			} else if ("fixeddate".equals(namelc)) {
				if (!ConfigurationUtils.isConfigurationStubbed(configurationClassLoader)) {
					throw new ParameterException("Parameter pattern [" + name + "] only allowed in stub mode");
				}
				Object fixedDateTime = session.get(PutSystemDateInSession.FIXEDDATE_STUB4TESTTOOL_KEY);
				if (fixedDateTime==null) {
					fixedDateTime = PutSystemDateInSession.FIXEDDATETIME;
				}
				substitutionValue = preFormatDateType(fixedDateTime, formatType);
			} else if ("fixeduid".equals(namelc)) {
				if (!ConfigurationUtils.isConfigurationStubbed(configurationClassLoader)) {
					throw new ParameterException("Parameter pattern [" + name + "] only allowed in stub mode");
				}
				substitutionValue = FIXEDUID;
			} else if ("fixedhostname".equals(namelc)) {
				if (!ConfigurationUtils.isConfigurationStubbed(configurationClassLoader)) {
					throw new ParameterException("Parameter pattern [" + name + "] only allowed in stub mode");
				}
				substitutionValue = FIXEDHOSTNAME;
			} else if ("username".equals(namelc)) {
				substitutionValue=cf!=null?cf.getUsername():"";
			} else if ("password".equals(namelc)) {
				substitutionValue=cf!=null?cf.getPassword():"";
			}
		}
		if (substitutionValue == null) {
			throw new ParameterException("Parameter or session variable with name [" + name + "] in pattern [" + getPattern() + "] cannot be resolved");
		}
		return substitutionValue;
	}

	@Override
	public String toString() {
		return "Parameter name=["+name+"] defaultValue=["+defaultValue+"] sessionKey=["+sessionKey+"] sessionKeyXPath=["+sessionKeyXPath+"] xpathExpression=["+xpathExpression+ "] type=["+type+ "] value=["+value+ "]";
	}

	private TransformerPool getTransformerPool() {
		return transformerPool;
	}

	@IbisDoc({"1", "Name of the parameter", ""})
	@Override
	public void setName(String parameterName) {
		name = parameterName;
	}
	@Override
	public String getName() {
		return name;
	}

	@IbisDoc({"2", "<ul>"+ 
		"<li><code>string</code>: renders the contents of the first node (in combination with xslt or xpath).<br/>"+ 
			"Please note that if there are child nodes, only the contents are returned, use <code>xml</code> if the xml tags "+ 
			"are required</li>"+ 
		"<li><code>xml</code>:  renders an xml-nodeset as an xml-string (in combination with xslt or xpath). "+ 
			"This will include the xml tags</li>"+ 
		"<li><code>node</code>: renders the CONTENTS of the first node as a nodeset "+ 
			"that can be used as such when passed as xslt-parameter (only for XSLT 1.0). <br/>"+
			"Please note that the nodeset may contain multiple nodes, without a common root node. <br/>"+
			"N.B. The result is the set of children of what you might expect it to be...</li>"+ 
		"<li><code>domdoc</code>: renders xml as a DOM document; similar to <code>node</code> "+ 
			"with the distinction that there is always a common root node (required for XSLT 2.0)</li>"+ 
		"<li><code>date</code>: converts the result to a Date, by default using formatString <code>yyyy-MM-dd</code>. "+ 
			"When applied as a JDBC parameter, the method setDate() is used</li>"+ 
		"<li><code>time</code>: converts the result to a Date, by default using formatString <code>HH:mm:ss</code>. "+ 
			"When applied as a JDBC parameter, the method setTime() is used</li>"+ 
		"<li><code>datetime</code>: converts the result to a Date, by default using formatString <code>yyyy-MM-dd HH:mm:ss</code>. "+ 
			"When applied as a JDBC parameter, the method setTimestamp() is used</li>"+ 
		"<li><code>timestamp</code>: similar to datetime, except for the formatString that is <code>yyyy-MM-dd HH:mm:ss.SSS</code> by default</li>"+ 
		"<li><code>xmldatetime</code>: converts the result from a XML dateTime to a Date. "+ 
			"When applied as a JDBC parameter, the method setTimestamp() is used</li>"+ 
		"<li><code>number</code>: converts the result to a Number, using decimalSeparator and groupingSeparator. "+ 
			"When applied as a JDBC parameter, the method setDouble() is used</li>"+ 
		"<li><code>integer</code>: converts the result to an Integer</li>"+ 
		"<li><code>inputstream</code>: only applicable as a JDBC parameter, the method setBinaryStream() is used</li>"+ 
		"<li><code>list</code>: converts a List&lt;String&gt; object to a xml-string (&lt;items&gt;&lt;item&gt;...&lt;/item&gt;&lt;item&gt;...&lt;/item&gt;&lt;/items&gt;)</li>"+ 
		"<li><code>map</code>: converts a Map&lt;String, String&gt; object to a xml-string (&lt;items&gt;&lt;item name='...'&gt;...&lt;/item&gt;&lt;item name='...'&gt;...&lt;/item&gt;&lt;/items&gt;)</li>"+ 
		"</ul>", "string"})
	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}

	@IbisDoc({"3", "The value of the parameter, or the base for transformation using xpathExpression or stylesheet, or formatting.", ""})
	public void setValue(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}

	@IbisDoc({"4", "Key of a pipelinesession-variable. <br/>If specified, the value of the pipelinesession variable is used as input for "+ 
			"the xpathExpression or stylesheet, instead of the current input message. <br/>If no xpathExpression or stylesheet are "+ 
			"specified, the value itself is returned. <br/>If the value '*' is specified, all existing sessionkeys are added as "+ 
			"parameter of which the name starts with the name of this parameter. <br/>If also the name of the parameter has the "+ 
			"value '*' then all existing sessionkeys are added as parameter (except tsreceived)", ""})
	public void setSessionKey(String string) {
		sessionKey = string;
	}
	public String getSessionKey() {
		return sessionKey;
	}

	@IbisDoc({"5", "Instead of a fixed <code>sessionkey</code> it's also possible to use a xpath expression to extract the name of "+ 
		"the <code>sessionkey</code>", ""})
	public void setSessionKeyXPath(String string) {
		sessionKeyXPath = string;
	}
	public String getSessionKeyXPath() {
		return sessionKeyXPath;
	}

	/**
	 * Specify the stylesheet to use
	 */
	@IbisDoc({"6", "url to a stylesheet that wil be applied to the contents of the message or the value of the session-variable.", ""})
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}

	/**
	 * @param xpathExpression to extract the parameter value from the (xml formatted) input 
	 */
	@IbisDoc({"7", "the xpath expression to extract the parameter value from the (xml formatted) input or session-variable.", ""})
	public void setXpathExpression(String xpathExpression) {
		this.xpathExpression = xpathExpression;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}

	@IbisDoc({"8", "when set to <code>2</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan). <code>0</code> will auto detect", "0"})
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}
	public int getXsltVersion() {
		return xsltVersion;
	}

	@IbisDoc({"9", "when set <code>true</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan)", "false"})
	/**
	 * @deprecated Please remove setting of xslt2, it will be auto detected. Or use xsltVersion.
	 */
	@Deprecated
	@ConfigurationWarning("Its value is now auto detected. If necessary, replace with a setting of xsltVersion")
	public void setXslt2(boolean b) {
		xsltVersion=b?2:1;
	}

	@IbisDoc({"10", "Namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of "+ 
			"<code>prefix=namespaceuri</code>-definitions. One entry can be without a prefix, that will define the default namespace.", ""})
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
	public String getNamespaceDefs() {
		return namespaceDefs;
	}

	@IbisDoc({"11", "When set <code>true</code> namespaces (and prefixes) in the input message are removed before the "+ 
		"stylesheet/xpathexpression is executed", "false"})
	public void setRemoveNamespaces(boolean b) {
		removeNamespaces = b;
	}
	public boolean isRemoveNamespaces() {
		return removeNamespaces;
	}

	@IbisDoc({"12", "If the result of sessionKey, xpathExpression and/or stylesheet returns null or an empty string, this value is returned", ""})
	public void setDefaultValue(String string) {
		defaultValue = string;
	}
	public String getDefaultValue() {
		return defaultValue;
	}

	@IbisDoc({"13", "Comma separated list of methods (defaultvalue, sessionKey, pattern, value or input) to use as default value. Used in the order they appear until a non-null value is found.", "defaultvalue"})
	public void setDefaultValueMethods(String string) {
		defaultValueMethods = string;
	}
	public String getDefaultValueMethods() {
		return defaultValueMethods;
	}

	/**
	 * @param string with pattern to be used, follows MessageFormat syntax with named parameters
	 */
	@IbisDoc({"14", "Value of parameter is determined using substitution and formating. The expression can contain references "+ 
		"to session-variables or other parameters using {name-of-parameter} and is formatted using java.text.MessageFormat. "+ 
		"<br/>If for instance <code>fname</code> is a parameter or session variable that resolves to eric, then the pattern "+ 
		"'hi {fname}, hoe gaat het?' resolves to 'hi eric, hoe gaat het?'.<br/>" +
		"The following predefined reference can be used in the expression too:<ul>" +
		"<li>{now}: the current system time</li>" +
		"<li>{uid}: an unique identifier, based on the IP address and java.rmi.server.UID</li>" +
		"<li>{uuid}: an unique identifier, based on the IP address and java.util.UUID</li>" +
		"<li>{hostname}: the name of the machine the application runs on</li>" +
		"<li>{username}: username from the credentials found using authAlias, or the username attribute</li>" +
		"<li>{password}: password from the credentials found using authAlias, or the password attribute</li>" +
		"<li>{fixeddate}: fake date, for testing only</li>" +
		"<li>{fixeduid}: fake uid, for testing only</li>" +
		"<li>{fixedhostname}: fake hostname, for testing only</li>" +
		"</ul>"+ 
		"A guid can be generated using {hostname}_{uid}, see also "+ 
		"<a href=\"http://java.sun.com/j2se/1.4.2/docs/api/java/rmi/server/uid.html\">http://java.sun.com/j2se/1.4.2/docs/api/java/rmi/server/uid.html</a> "+ 
		"for more information about (g)uid's or <a href=\"http://docs.oracle.com/javase/1.5.0/docs/api/java/util/uuid.html\">http://docs.oracle.com/javase/1.5.0/docs/api/java/util/uuid.html</a> "+ 
		"for more information about uuid's.", ""})
	public void setPattern(String string) {
		pattern = string;
	}
	public String getPattern() {
		return pattern;
	}

	@IbisDoc({"15", "Alias used to obtain username and password, used when a <code>pattern</code> containing {username} or {password} is specified", ""})
	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	@IbisDoc({"16", "Default username that is used when a <code>pattern</code> containing {username} is specified", ""})
	public void setUserName(String string) {
		userName = string;
	}
	public String getUserName() {
		return userName;
	}

	@IbisDoc({"17", "Default password that is used when a <code>pattern</code> containing {password} is specified", " "})
	public void setPassword(String string) {
		password = string;
	}
	public String getPassword() {
		return password;
	}

	@IbisDoc({"18", "Used in combination with types <code>date</code>, <code>time</code> and <code>datetime</code>", "depends on type"})
	public void setFormatString(String string) {
		formatString = string;
	}
	public String getFormatString() {
		return formatString;
	}

	@IbisDoc({"19", "Used in combination with type <code>number</code>", "system default"})
	public void setDecimalSeparator(String string) {
		decimalSeparator = string;
	}
	public String getDecimalSeparator() {
		return decimalSeparator;
	}

	@IbisDoc({"20", "Used in combination with type <code>number</code>", "system default"})
	public void setGroupingSeparator(String string) {
		groupingSeparator = string;
	}
	public String getGroupingSeparator() {
		return groupingSeparator;
	}

	@IbisDoc({"21", "If set (>=0) and the length of the value of the parameter deceeds this minimum length, the value is padded", "-1"})
	public void setMinLength(int i) {
		minLength = i;
	}
	public int getMinLength() {
		return minLength;
	}

	@IbisDoc({"22", "If set (>=0) and the length of the value of the parameter exceeds this maximum length, the length is trimmed "+ 
		"to this maximum length", "-1"})
	public void setMaxLength(int i) {
		maxLength = i;
	}
	public int getMaxLength() {
		return maxLength;
	}

	@IbisDoc({"23", "Used in combination with type <code>number</code>; if set and the value of the parameter exceeds this "+ 
		"maximum value, this maximum value is taken", ""})
	public void setMaxInclusive(String string) {
		maxInclusiveString = string;
	}
	public String getMaxInclusive() {
		return maxInclusiveString;
	}

	@IbisDoc({"24", "Used in combination with type <code>number</code>; if set and the value of the parameter exceeds this "+ 
		"minimum value, this minimum value is taken", ""})
	public void setMinInclusive(String string) {
		minInclusiveString = string;
	}
	public String getMinInclusive() {
		return minInclusiveString;
	}

	@IbisDoc({"25", "If set to <code>true</code>, the value of the parameter will not be shown in the log (replaced by asterisks)", "<code>false</code>"})
	public void setHidden(boolean b) {
		hidden = b;
	}
	public boolean isHidden() {
		return hidden;
	}

}
