/*
   Copyright 2013, 2016, 2019, 2020 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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

import static nl.nn.adapterframework.functional.FunctionalUtil.logValue;
import static nl.nn.adapterframework.util.StringUtil.hide;

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
import java.util.stream.Collectors;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.doc.DocumentedEnum;
import nl.nn.adapterframework.doc.EnumLabel;
import nl.nn.adapterframework.pipes.PutSystemDateInSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.util.UUIDUtil;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Generic parameter definition.
 *
 * A parameter resembles an attribute. However, while attributes get their value at configuration-time,
 * parameters get their value at the time of processing the message. Value can be retrieved from the message itself,
 * a fixed value, or from the pipelineSession. If this does not result in a value (or if neither of these is specified), a default value
 * can be specified. If an XPathExpression or stylesheet is specified, it will be applied to the message, the value retrieved
 * from the pipelineSession or the fixed value specified. If the transformation produces no output, the default value
 * of the parameter is taken if provided.
 * <br/><br/>
 * Examples:
 * <pre><code>
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
 * </code></pre>
 *
 * N.B. to obtain a fixed value: a non-existing 'dummy' <code>sessionKey</code> in combination with the fixed value in <code>defaultValue</code> is used traditionally.
 * The current version of parameter supports the 'value' attribute, that is sufficient to set a fixed value.
 * @author Gerrit van Brakel
 * @ff.parameters Parameters themselves can have parameters too, for instance if a XSLT transformation is used, that transformation can have parameters.
 */
public class Parameter implements IConfigurable, IWithParameters {
	private static final Logger LOG = LogManager.getLogger(Parameter.class);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	public static final String TYPE_DATE_PATTERN="yyyy-MM-dd";
	public static final String TYPE_TIME_PATTERN="HH:mm:ss";
	public static final String TYPE_DATETIME_PATTERN="yyyy-MM-dd HH:mm:ss";
	public static final String TYPE_TIMESTAMP_PATTERN=DateUtils.FORMAT_FULL_GENERIC;

	public static final String FIXEDUID ="0a1b234c--56de7fa8_9012345678b_-9cd0";
	public static final String FIXEDHOSTNAME ="MYHOST000012345";

	private String name = null;
	private @Getter ParameterType type = ParameterType.STRING;
	private @Getter String sessionKey = null;
	private @Getter String sessionKeyXPath = null;
	private @Getter String contextKey = null;
	private @Getter String xpathExpression = null;
	private @Getter String namespaceDefs = null;
	private @Getter String styleSheetName = null;
	private @Getter String pattern = null;
	private @Getter String authAlias;
	private @Getter String username;
	private @Getter String password;
	private @Getter boolean ignoreUnresolvablePatternElements = false;
	private @Getter String defaultValue = null;
	private @Getter String defaultValueMethods = "defaultValue";
	private @Getter String value = null;
	private @Getter String formatString = null;
	private @Getter String decimalSeparator = null;
	private @Getter String groupingSeparator = null;
	private @Getter int minLength = -1;
	private @Getter int maxLength = -1;
	private @Getter String minInclusiveString = null;
	private @Getter String maxInclusiveString = null;
	private Number minInclusive;
	private Number maxInclusive;
	private @Getter boolean hidden = false;
	private @Getter boolean removeNamespaces=false;
	private @Getter int xsltVersion=0; // set to 0 for auto detect.

	private @Getter DecimalFormatSymbols decimalFormatSymbols = null;
	private TransformerPool transformerPool = null;
	private TransformerPool transformerPoolRemoveNamespaces;
	private TransformerPool tpDynamicSessionKey = null;
	protected ParameterList paramList = null;
	private boolean configured = false;
	private CredentialFactory cf;

	private List<DefaultValueMethods> defaultValueMethodsList;

	@Getter
	private ParameterMode mode = ParameterMode.INPUT;

	public enum ParameterMode {
		INPUT, OUTPUT, INOUT
	}

	public enum ParameterType {
		/** Renders the contents of the first node (in combination with xslt or xpath). Please note that
		 * if there are child nodes, only the contents are returned, use <code>XML</code> if the xml tags are required */
		STRING,

		/** Renders an xml-nodeset as an xml-string (in combination with xslt or xpath). This will include the xml tags */
		XML,

		/** Renders the CONTENTS of the first node as a nodeset
		 * that can be used as such when passed as xslt-parameter (only for XSLT 1.0).
		 * Please note that the nodeset may contain multiple nodes, without a common root node.
		 * N.B. The result is the set of children of what you might expect it to be... */
		NODE(true),

		/** Renders XML as a DOM document; similar to <code>node</code>
			with the distinction that there is always a common root node (required for XSLT 2.0) */
		DOMDOC(true),

		/** Converts the result to a Date, by default using formatString <code>yyyy-MM-dd</code>.
		 * When applied as a JDBC parameter, the method setDate() is used */
		DATE(true),

		/** Converts the result to a Date, by default using formatString <code>HH:mm:ss</code>.
		 * When applied as a JDBC parameter, the method setTime() is used */
		TIME(true),

		/** Converts the result to a Date, by default using formatString <code>yyyy-MM-dd HH:mm:ss</code>.
		 * When applied as a JDBC parameter, the method setTimestamp() is used */
		DATETIME(true),

		/** Similar to <code>DATETIME</code>, except for the formatString that is <code>yyyy-MM-dd HH:mm:ss.SSS</code> by default */
		TIMESTAMP(true),

		/** Converts the result from a XML formatted dateTime to a Date.
		 * When applied as a JDBC parameter, the method setTimestamp() is used */
		XMLDATETIME(true),

		/** Converts the result to a Number, using decimalSeparator and groupingSeparator.
		 * When applied as a JDBC parameter, the method setDouble() is used */
		NUMBER(true),

		/** Converts the result to an Integer */
		INTEGER(true),

		/** Converts the result to a Boolean */
		BOOLEAN(true),

		/** Only applicable as a JDBC parameter, the method setBinaryStream() is used */
		@ConfigurationWarning("use type [BINARY] instead")
		@Deprecated INPUTSTREAM,

		/** Only applicable as a JDBC parameter, the method setBytes() is used */
		@ConfigurationWarning("use type [BINARY] instead")
		@Deprecated BYTES,

		/** Forces the parameter value to be treated as binary data (e.g. when using a SQL BLOB field).
		 * When applied as a JDBC parameter, the method setBinaryStream() or setBytes() is used */
		BINARY,

		/** Forces the parameter value to be treated as character data (e.g. when using a SQL CLOB field).
		 * When applied as a JDBC parameter, the method setCharacterStream() or setString() is used */
		CHARACTER,

		/** (Used in larva only) Converts a List to a xml-string (&lt;items&gt;&lt;item&gt;...&lt;/item&gt;&lt;item&gt;...&lt;/item&gt;&lt;/items&gt;) */
		@Deprecated LIST,

		/** (Used in larva only) Converts a Map&lt;String, String&gt; object to a xml-string (&lt;items&gt;&lt;item name='...'&gt;...&lt;/item&gt;&lt;item name='...'&gt;...&lt;/item&gt;&lt;/items&gt;) */
		@Deprecated MAP;

		public final boolean requiresTypeConversion;

		private ParameterType() {
			this(false);
		}

		private ParameterType(boolean requiresTypeConverion) {
			this.requiresTypeConversion = requiresTypeConverion;
		}

	}

	public enum DefaultValueMethods implements DocumentedEnum {
		@EnumLabel("defaultValue")	DEFAULTVALUE,
		@EnumLabel("sessionKey")	SESSIONKEY,
		@EnumLabel("pattern")		PATTERN,
		@EnumLabel("value")			VALUE,
		@EnumLabel("input") 		INPUT;
	}

	public Parameter() {
		super();
	}

	/** utility constructor, useful for unit testing */
	public Parameter(String name, String value) {
		this();
		this.name = name;
		this.value = value;
	}

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
			OutputType outputType = getType() == ParameterType.XML || getType() == ParameterType.NODE || getType() == ParameterType.DOMDOC ? OutputType.XML : OutputType.TEXT;
			boolean includeXmlDeclaration = false;

			transformerPool = TransformerPool.configureTransformer0(this, getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), outputType, includeXmlDeclaration, paramList, getXsltVersion());
		} else {
			if (paramList != null && StringUtils.isEmpty(getPattern())) {
				throw new ConfigurationException("Parameter [" + getName() + "] can only have parameters itself if a styleSheetName, xpathExpression or pattern is specified");
			}
		}
		if (isRemoveNamespaces()) {
			transformerPoolRemoveNamespaces = XmlUtils.getRemoveNamespacesTransformerPool(true,false);
		}
		if (StringUtils.isNotEmpty(getSessionKeyXPath())) {
			tpDynamicSessionKey = TransformerPool.configureTransformer(this, getNamespaceDefs(), getSessionKeyXPath(), null, OutputType.TEXT,false,null);
		}
		if (getType() == null) {
			LOG.info("parameter [{} has no type. Setting the type to [{}]", this::getType, ()->ParameterType.STRING);
			setType(ParameterType.STRING);
		}
		if(StringUtils.isEmpty(getFormatString())) {
			switch(getType()) {
				case DATE:
					setFormatString(TYPE_DATE_PATTERN);
					break;
				case DATETIME:
					setFormatString(TYPE_DATETIME_PATTERN);
					break;
				case TIMESTAMP:
					setFormatString(TYPE_TIMESTAMP_PATTERN);
					break;
				case TIME:
					setFormatString(TYPE_TIME_PATTERN);
					break;
				default:
					break;
			}
		}
		if (getType()==ParameterType.NUMBER) {
			decimalFormatSymbols = new DecimalFormatSymbols();
			if (StringUtils.isNotEmpty(getDecimalSeparator())) {
				decimalFormatSymbols.setDecimalSeparator(getDecimalSeparator().charAt(0));
			}
			if (StringUtils.isNotEmpty(getGroupingSeparator())) {
				decimalFormatSymbols.setGroupingSeparator(getGroupingSeparator().charAt(0));
			}
		}
		configured = true;

		if (getMinInclusiveString()!=null || getMaxInclusiveString()!=null) {
			if (getType()!=ParameterType.NUMBER) {
				throw new ConfigurationException("minInclusive and maxInclusive only allowed in combination with type ["+ParameterType.NUMBER+"]");
			}
			if (getMinInclusiveString()!=null) {
				DecimalFormat df = new DecimalFormat();
				df.setDecimalFormatSymbols(decimalFormatSymbols);
				try {
					minInclusive = df.parse(getMinInclusiveString());
				} catch (ParseException e) {
					throw new ConfigurationException("Attribute [minInclusive] could not parse result ["+getMinInclusiveString()+"] to number; decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]",e);
				}
			}
			if (getMaxInclusiveString()!=null) {
				DecimalFormat df = new DecimalFormat();
				df.setDecimalFormatSymbols(decimalFormatSymbols);
				try {
					maxInclusive = df.parse(getMaxInclusiveString());
				} catch (ParseException e) {
					throw new ConfigurationException("Attribute [maxInclusive] could not parse result ["+getMaxInclusiveString()+"] to number; decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]",e);
				}
			}
		}
		if (StringUtils.isNotEmpty(getAuthAlias()) || StringUtils.isNotEmpty(getUsername()) || StringUtils.isNotEmpty(getPassword())) {
			cf=new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		}
	}

	private List<DefaultValueMethods> getDefaultValueMethodsList() {
		if (defaultValueMethodsList == null) {
			defaultValueMethodsList = StringUtil.splitToStream(getDefaultValueMethods(), ", ")
					.map(token -> EnumUtils.parse(DefaultValueMethods.class, token))
					.collect(Collectors.toList());
		}
		return defaultValueMethodsList;
	}

	private Document transformToDocument(Source xmlSource, ParameterValueList pvl) throws TransformerException, IOException {
		TransformerPool pool = getTransformerPool();
		DOMResult transformResult = new DOMResult();
		pool.transform(xmlSource,transformResult, pvl);
		return (Document) transformResult.getNode();
	}


	/**
	 * if this returns true, then the input value must be repeatable, as it might be used multiple times.
	 */
	public boolean requiresInputValueForResolution() {
		if (tpDynamicSessionKey != null) { // tpDynamicSessionKey is applied to the input message to retrieve the session key
			return true;
		}
		return StringUtils.isEmpty(getContextKey()) &&
				(StringUtils.isEmpty(getSessionKey()) && StringUtils.isEmpty(getValue()) && StringUtils.isEmpty(getPattern())
						|| getDefaultValueMethodsList().contains(DefaultValueMethods.INPUT)
				);
	}

	public boolean requiresInputValueOrContextForResolution() {
		if (tpDynamicSessionKey != null) { // tpDynamicSessionKey is applied to the input message to retrieve the session key
			return true;
		}
		return StringUtils.isEmpty(getSessionKey()) && StringUtils.isEmpty(getValue()) && StringUtils.isEmpty(getPattern())
					|| getDefaultValueMethodsList().contains(DefaultValueMethods.INPUT);
	}

	public boolean consumesSessionVariable(String sessionKey) {
		return StringUtils.isEmpty(getContextKey()) && (
					sessionKey.equals(getSessionKey())
					|| getPattern()!=null && getPattern().contains("{"+sessionKey+"}")
					|| getParameterList()!=null && getParameterList().consumesSessionVariable(sessionKey)
				);
	}

	/**
	 * determines the raw value
	 */
	public Object getValue(ParameterValueList alreadyResolvedParameters, Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		Object result = null;
		LOG.debug("Calculating value for Parameter [{}]", this::getName);
		if (!configured) {
			throw new ParameterException("Parameter ["+getName()+"] not configured");
		}

		String requestedSessionKey;
		if (tpDynamicSessionKey != null) {
			try {
				requestedSessionKey = tpDynamicSessionKey.transform(message.asSource());
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
				if (getValue()!=null) {
					source = XmlUtils.stringToSourceForSingleUse(getValue(), namespaceAware);
				} else if (StringUtils.isNotEmpty(requestedSessionKey)) {
					Object sourceObject = session.get(requestedSessionKey);
					if (getType()==ParameterType.LIST && sourceObject instanceof List) {
						// larva can produce the sourceObject as list
						List<String> items = (List<String>) sourceObject;
						XmlBuilder itemsXml = new XmlBuilder("items");
						for (String item : items) {
							XmlBuilder itemXml = new XmlBuilder("item");
							itemXml.setValue(item);
							itemsXml.addSubElement(itemXml);
						}
						source = XmlUtils.stringToSourceForSingleUse(itemsXml.toXML(), namespaceAware);
					} else if (getType()==ParameterType.MAP && sourceObject instanceof Map) {
						// larva can produce the sourceObject as map
						Map<String, String> items = (Map<String, String>) sourceObject;
						XmlBuilder itemsXml = new XmlBuilder("items");
						for (String item : items.keySet()) {
							XmlBuilder itemXml = new XmlBuilder("item");
							itemXml.addAttribute("name", item);
							itemXml.setValue(items.get(item));
							itemsXml.addSubElement(itemXml);
						}
						source = XmlUtils.stringToSourceForSingleUse(itemsXml.toXML(), namespaceAware);
					} else {
						Message sourceMsg = Message.asMessage(sourceObject);
						if (StringUtils.isNotEmpty(getContextKey())) {
							sourceMsg = Message.asMessage(sourceMsg.getContext().get(getContextKey()));
						}
						if (!sourceMsg.isEmpty()) {
							LOG.debug("Parameter [{}] using sessionvariable [{}] as source for transformation", this::getName, ()-> requestedSessionKey);
							source = sourceMsg.asSource();
						} else {
							LOG.debug("Parameter [{}] sessionvariable [{}] empty, no transformation will be performed", this::getName, ()-> requestedSessionKey);
						}
					}
				} else if (StringUtils.isNotEmpty(getPattern())) {
					String sourceString = formatPattern(alreadyResolvedParameters, session);
					if (StringUtils.isNotEmpty(sourceString)) {
						LOG.debug("Parameter [{}] using pattern [{}] as source for transformation", this::getName, this::getPattern);
						source = XmlUtils.stringToSourceForSingleUse(sourceString, namespaceAware);
					} else {
						LOG.debug("Parameter [{}] pattern [{}] empty, no transformation will be performed", this::getName, this::getPattern);
					}
				} else {
					if (StringUtils.isNotEmpty(getContextKey())) {
						source = Message.asSource(message.getContext().get(getContextKey()));
					} else {
						source = message.asSource();
					}
				}
				if (source!=null) {
					if (transformerPoolRemoveNamespaces != null) {
						String rnResult = transformerPoolRemoveNamespaces.transform(source);
						source = XmlUtils.stringToSource(rnResult);
					}
					ParameterValueList pvl = paramList==null ? null : paramList.getValues(message, session, namespaceAware);
					switch (getType()) {
					case NODE:
						return transformToDocument(source, pvl).getFirstChild();
					case DOMDOC:
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
				result = session.get(requestedSessionKey);
				if (result instanceof Message && StringUtils.isNotEmpty(getContextKey())) {
					result = ((Message)result).getContext().get(getContextKey());
				}
				if (LOG.isDebugEnabled() && (result == null ||
						((result instanceof String) && ((String) result).isEmpty()) ||
						((result instanceof Message) && ((Message) result).isEmpty()))) {
					LOG.debug("Parameter [{}] session variable [{}] is empty", this::getName, () -> requestedSessionKey);
				}
			} else if (StringUtils.isNotEmpty(getPattern())) {
				result = formatPattern(alreadyResolvedParameters, session);
			} else if (getValue()!=null) {
				result = getValue();
			} else {
				try {
					if (message==null) {
						return null;
					}
					if (StringUtils.isNotEmpty(getContextKey())) {
						result = message.getContext().get(getContextKey());
					} else {
						message.preserve();
						result=message;
					}
				} catch (IOException e) {
					throw new ParameterException(e);
				}
			}
		}

		if (result instanceof Message) { //we just need to check if the message is null or not!
			if (Message.isNull((Message)result)) {
				result = null;
			} else if (((Message)result).asObject() instanceof String) { //Used by getMinLength and getMaxLength
				result = ((Message) result).asObject();
			}
		}
		if (result != null && !"".equals(result)) {
			final Object finalResult = result;
			LOG.debug("Parameter [{}] resolved to [{}]", this::getName, ()-> isHidden() ? hide(finalResult.toString()) : finalResult);
		} else {
			// if result is empty then return specified default value
			Object valueByDefault=null;
			Iterator<DefaultValueMethods> it = getDefaultValueMethodsList().iterator();
			while (valueByDefault == null && it.hasNext()) {
				DefaultValueMethods method = it.next();
				switch(method) {
					case DEFAULTVALUE:
						valueByDefault = getDefaultValue();
						break;
					case SESSIONKEY:
						valueByDefault = session.get(requestedSessionKey);
						break;
					case PATTERN:
						valueByDefault = formatPattern(alreadyResolvedParameters, session);
						break;
					case VALUE:
						valueByDefault = getValue();
						break;
					case INPUT:
						try {
							message.preserve();
							valueByDefault=message.asString();
						} catch (IOException e) {
							throw new ParameterException(e);
						}
						break;
					default:
						throw new IllegalArgumentException("Unknown defaultValues method ["+method+"]");
				}
			}
			if (valueByDefault!=null) {
				result = valueByDefault;
				final Object finalResult = result;
				LOG.debug("Parameter [{}] resolved to default value [{}]", this::getName, ()-> isHidden() ? hide(finalResult.toString()) : finalResult);
			}
		}
		if (result instanceof String) {
			if (getMinLength()>=0 && getType()!=ParameterType.NUMBER) {
				final String stringResult = (String) result;
				if (stringResult.length() < getMinLength()) {
					LOG.debug("Padding parameter [{}] because length [{}] falls short of minLength [{}]", this::getName, stringResult::length, this::getMinLength);
					result = StringUtils.rightPad(stringResult, getMinLength());
				}
			}
			if (getMaxLength()>=0) {
				final String stringResult = (String) result;
				if (stringResult.length() > getMaxLength()) {
					LOG.debug("Trimming parameter [{}] because length [{}] exceeds maxLength [{}]", this::getName, stringResult::length, this::getMaxLength);
					result = stringResult.substring(0, getMaxLength());
				}
			}
		}
		if(result !=null && getType().requiresTypeConversion) {
			result = getValueAsType(result, namespaceAware);
		}
		if (result instanceof Number) {
			if (getMinInclusiveString()!=null && ((Number)result).floatValue() < minInclusive.floatValue()) {
				LOG.debug("Replacing parameter [{}] because value [{}] falls short of minInclusive [{}]", this::getName, logValue(result), this::getMinInclusiveString);
				result = minInclusive;
			}
			if (getMaxInclusiveString()!=null && ((Number)result).floatValue() > maxInclusive.floatValue()) {
				LOG.debug("Replacing parameter [{}] because value [{}] exceeds maxInclusive [{}]", this::getName, logValue(result), this::getMaxInclusiveString);
				result = maxInclusive;
			}
		}
		if (getType()==ParameterType.NUMBER && getMinLength()>=0 && (result+"").length()<getMinLength()) {
			LOG.debug("Adding leading zeros to parameter [{}]", this::getName);
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
				case NODE:
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
						result = XmlUtils.buildDomDocument(requestMessage.asInputSource(), namespaceAware).getDocumentElement();
						final Object finalResult = result;
						LOG.debug("final result [{}][{}]", ()->finalResult.getClass().getName(), ()-> finalResult);
					} catch (DomBuilderException | TransformerException | IOException | SAXException e) {
						throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+requestMessage+"] to XML nodeset",e);
					}
					break;
				case DOMDOC:
					try {
						if (transformerPoolRemoveNamespaces != null) {
							requestMessage = new Message(transformerPoolRemoveNamespaces.transform(requestMessage, null));
						}
						if(requestObject instanceof Document) {
							return requestObject;
						}
						result = XmlUtils.buildDomDocument(requestMessage.asInputSource(), namespaceAware);
						final Object finalResult = result;
						LOG.debug("final result [{}][{}]", ()->finalResult.getClass().getName(), ()-> finalResult);
					} catch (DomBuilderException | TransformerException | IOException | SAXException e) {
						throw new ParameterException("Parameter ["+getName()+"] could not parse result ["+requestMessage+"] to XML document",e);
					}
					break;
				case DATE:
				case DATETIME:
				case TIMESTAMP:
				case TIME: {
					if (requestObject instanceof Date) {
						return requestObject;
					}
					Message finalRequestMessage = requestMessage;
					LOG.debug("Parameter [{}] converting result [{}] to Date using formatString [{}]", this::getName, () -> finalRequestMessage, this::getFormatString);
					DateFormat df = new SimpleDateFormat(getFormatString());
					try {
						result = df.parseObject(requestMessage.asString());
					} catch (ParseException e) {
						throw new ParameterException("Parameter [" + getName() + "] could not parse result [" + requestMessage + "] to Date using formatString [" + getFormatString() + "]", e);
					}
					break;
				}
				case XMLDATETIME: {
					if (requestObject instanceof Date) {
						return requestObject;
					}
					Message finalRequestMessage = requestMessage;
					LOG.debug("Parameter [{}] converting result [{}] from XML dateTime to Date", this::getName, () -> finalRequestMessage);
					result = DateUtils.parseXmlDateTime(requestMessage.asString());
					break;
				}
				case NUMBER: {
					if (requestObject instanceof Number) {
						return requestObject;
					}
					Message finalRequestMessage = requestMessage;
					LOG.debug("Parameter [{}] converting result [{}] to number decimalSeparator [{}] groupingSeparator [{}]", this::getName, ()->finalRequestMessage, decimalFormatSymbols::getDecimalSeparator, decimalFormatSymbols::getGroupingSeparator);
					DecimalFormat decimalFormat = new DecimalFormat();
					decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);
					try {
						result = decimalFormat.parse(requestMessage.asString());
					} catch (ParseException e) {
						throw new ParameterException("Parameter [" + getName() + "] could not parse result [" + requestMessage + "] to number decimalSeparator [" + decimalFormatSymbols.getDecimalSeparator() + "] groupingSeparator [" + decimalFormatSymbols.getGroupingSeparator() + "]", e);
					}
					break;
				}
				case INTEGER: {
					if (requestObject instanceof Integer) {
						return requestObject;
					}
					Message finalRequestMessage = requestMessage;
					LOG.debug("Parameter [{}] converting result [{}] to integer", this::getName, ()->finalRequestMessage);
					try {
						result = Integer.parseInt(requestMessage.asString());
					} catch (NumberFormatException e) {
						throw new ParameterException("Parameter [" + getName() + "] could not parse result [" + requestMessage + "] to integer", e);
					}
					break;
				}
				case BOOLEAN: {
					if (requestObject instanceof Boolean) {
						return requestObject;
					}
					Message finalRequestMessage = requestMessage;
					LOG.debug("Parameter [{}] converting result [{}] to boolean", this::getName, ()->finalRequestMessage);
					result = Boolean.parseBoolean(requestMessage.asString());
					break;
				}
				default:
					break;
			}
		} catch(IOException e) {
			throw new ParameterException("Could not convert parameter ["+getName()+"] to String", e);
		}

		return result;
	}

	private String formatPattern(ParameterValueList alreadyResolvedParameters, PipeLineSession session) throws ParameterException {
		int startNdx;
		int endNdx = 0;

		// replace the named parameter with numbered parameters
		StringBuilder formatPattern = new StringBuilder();
		List<Object> params = new ArrayList<>();
		int paramPosition = 0;
		while(true) {
			// get name of parameter in pattern to be substituted
			startNdx = pattern.indexOf("{", endNdx);
			if (startNdx == -1) {
				formatPattern.append(pattern.substring(endNdx));
				break;
			}
			else {
				formatPattern.append(pattern, endNdx, startNdx);
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
		try {
			return MessageFormat.format(formatPattern.toString(), params.toArray());
		} catch (Exception e) {
			throw new ParameterException("Cannot parse ["+formatPattern.toString()+"]", e);
		}
	}

	private Object preFormatDateType(Object rawValue, String formatType, String patternFormatString) throws ParameterException {
		if (formatType!=null && (formatType.equalsIgnoreCase("date") || formatType.equalsIgnoreCase("time"))) {
			if (rawValue instanceof Date) {
				return rawValue;
			}
			DateFormat df = new SimpleDateFormat(StringUtils.isNotEmpty(patternFormatString) ? patternFormatString : DateUtils.FORMAT_GENERICDATETIME);
			try {
				return df.parse(Message.asString(rawValue));
			} catch (ParseException | IOException e) {
				throw new ParameterException("Cannot parse ["+rawValue+"] as date", e);
			}
		}
		if (rawValue instanceof Date) {
			DateFormat df = new SimpleDateFormat(StringUtils.isNotEmpty(patternFormatString) ? patternFormatString : DateUtils.FORMAT_GENERICDATETIME);
			return df.format(rawValue);
		}
		try {
			return Message.asString(rawValue);
		} catch (IOException e) {
			throw new ParameterException("Cannot read date value ["+rawValue+"]", e);
		}
	}


	private Object getValueForFormatting(ParameterValueList alreadyResolvedParameters, PipeLineSession session, String targetPattern) throws ParameterException {
		String[] patternElements = targetPattern.split(",");
		String name = patternElements[0].trim();
		String formatType = patternElements.length>1 ? patternElements[1].trim() : null;
		String formatString = patternElements.length>2 ? patternElements[2].trim() : null;

		ParameterValue paramValue = alreadyResolvedParameters.get(name);
		Object substitutionValue = paramValue == null ? null : paramValue.getValue();

		if (substitutionValue == null) {
			Message substitutionValueMessage = session.getMessage(name);
			if (!substitutionValueMessage.isEmpty()) {
				if (substitutionValueMessage.asObject() instanceof Date) {
					substitutionValue = preFormatDateType(substitutionValueMessage.asObject(), formatType, formatString);
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
			switch (namelc) {
				case "now":
					substitutionValue = preFormatDateType(new Date(), formatType, formatString);
					break;
				case "uid":
					substitutionValue = UUIDUtil.createSimpleUUID();
					break;
				case "uuid":
					substitutionValue = UUIDUtil.createRandomUUID();
					break;
				case "hostname":
					substitutionValue = Misc.getHostname();
					break;
				case "fixeddate":
					if (!ConfigurationUtils.isConfigurationStubbed(configurationClassLoader)) {
						throw new ParameterException("Parameter pattern [" + name + "] only allowed in stub mode");
					}
					Object fixedDateTime = session.get(PutSystemDateInSession.FIXEDDATE_STUB4TESTTOOL_KEY);
					if (fixedDateTime == null) {
						DateFormat df = new SimpleDateFormat(DateUtils.FORMAT_GENERICDATETIME);
						try {
							fixedDateTime = df.parse(PutSystemDateInSession.FIXEDDATETIME);
						} catch (ParseException e) {
							throw new ParameterException("Could not parse FIXEDDATETIME [" + PutSystemDateInSession.FIXEDDATETIME + "]", e);
						}
					}
					substitutionValue = preFormatDateType(fixedDateTime, formatType, formatString);
					break;
				case "fixeduid":
					if (!ConfigurationUtils.isConfigurationStubbed(configurationClassLoader)) {
						throw new ParameterException("Parameter pattern [" + name + "] only allowed in stub mode");
					}
					substitutionValue = FIXEDUID;
					break;
				case "fixedhostname":
					if (!ConfigurationUtils.isConfigurationStubbed(configurationClassLoader)) {
						throw new ParameterException("Parameter pattern [" + name + "] only allowed in stub mode");
					}
					substitutionValue = FIXEDHOSTNAME;
					break;
				case "username":
					substitutionValue = cf != null ? cf.getUsername() : "";
					break;
				case "password":
					substitutionValue = cf != null ? cf.getPassword() : "";
					break;
			}
		}
		if (substitutionValue == null) {
			if (isIgnoreUnresolvablePatternElements()) {
				substitutionValue="";
			} else {
				throw new ParameterException("Parameter or session variable with name [" + name + "] in pattern [" + getPattern() + "] cannot be resolved");
			}
		}
		return substitutionValue;
	}

	@Override
	public String toString() {
		return "Parameter name=[" + name + "] defaultValue=[" + defaultValue + "] sessionKey=[" + sessionKey + "] sessionKeyXPath=[" + sessionKeyXPath + "] xpathExpression=[" + xpathExpression + "] type=[" + type + "] value=[" + value + "]";
	}

	private TransformerPool getTransformerPool() {
		return transformerPool;
	}

	/** Name of the parameter */
	@Override
	public void setName(String parameterName) {
		name = parameterName;
	}
	@Override
	public String getName() {
		return name;
	}

	/** The target data type of the parameter, related to the database or XSLT stylesheet to which the parameter is applied. */
	public void setType(ParameterType type) {
		this.type = type;
	}

	/** The value of the parameter, or the base for transformation using xpathExpression or stylesheet, or formatting. */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Key of a PipelineSession-variable. <br/>If specified, the value of the PipelineSession variable is used as input for
	 * the xpathExpression or stylesheet, instead of the current input message. <br/>If no xpathExpression or stylesheet are
	 * specified, the value itself is returned. <br/>If the value '*' is specified, all existing sessionkeys are added as
	 * parameter of which the name starts with the name of this parameter. <br/>If also the name of the parameter has the
	 * value '*' then all existing sessionkeys are added as parameter (except tsReceived)
	 */
	public void setSessionKey(String string) {
		sessionKey = string;
	}

	/** key of message context variable to use as source, instead of the message found from input message or sessionKey itself */
	public void setContextKey(String string) {
		contextKey = string;
	}

	/** Instead of a fixed <code>sessionKey</code> it's also possible to use a XPath expression applied to the input message to extract the name of the session-variable. */
	public void setSessionKeyXPath(String string) {
		sessionKeyXPath = string;
	}

	/** URL to a stylesheet that wil be applied to the contents of the message or the value of the session-variable. */
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}

	/** the XPath expression to extract the parameter value from the (xml formatted) input or session-variable. */
	public void setXpathExpression(String xpathExpression) {
		this.xpathExpression = xpathExpression;
	}

	/**
	 * If set to <code>2</code> or <code>3</code> a Saxon (net.sf.saxon) xslt processor 2.0 or 3.0 respectively will be used, otherwise xslt processor 1.0 (org.apache.xalan). <code>0</code> will auto detect	 * @ff.default 0
	 */
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}

	@Deprecated
	@ConfigurationWarning("Its value is now auto detected. If necessary, replace with a setting of xsltVersion")
	public void setXslt2(boolean b) {
		xsltVersion=b?2:1;
	}

	/**
	 * Namespace definitions for xpathExpression. Must be in the form of a comma or space separated list of
	 * <code>prefix=namespaceuri</code>-definitions. One entry can be without a prefix, that will define the default namespace.
	 */
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}

	/**
	 * When set <code>true</code> namespaces (and prefixes) in the input message are removed before the stylesheet/xpathExpression is executed
	 * @ff.default <code>false</code>
	 */
	public void setRemoveNamespaces(boolean b) {
		removeNamespaces = b;
	}

	/** If the result of sessionKey, xpathExpression and/or stylesheet returns null or an empty string, this value is returned */
	public void setDefaultValue(String string) {
		defaultValue = string;
	}

	/**
	 * Comma separated list of methods (<code>defaultValue</code>, <code>sessionKey</code>, <code>pattern</code>, <code>value</code> or <code>input</code>) to use as default value. Used in the order they appear until a non-null value is found.
	 * @ff.default <code>defaultValue</code>
	 */
	public void setDefaultValueMethods(String string) {
		defaultValueMethods = string;
	}

	/**
	 * Value of parameter is determined using substitution and formatting, following MessageFormat syntax with named parameters. The expression can contain references
	 * to session-variables or other parameters using {name-of-parameter} and is formatted using java.text.MessageFormat.
	 * <br/>If for instance <code>fname</code> is a parameter or session variable that resolves to Eric, then the pattern
	 * 'Hi {fname}, how do you do?' resolves to 'Hi Eric, do you do?'.<br/>
	 * The following predefined reference can be used in the expression too:<ul>
	 * <li>{now}: the current system time</li>
	 * <li>{uid}: an unique identifier, based on the IP address and java.rmi.server.UID</li>
	 * <li>{uuid}: an unique identifier, based on the IP address and java.util.UUID</li>
	 * <li>{hostname}: the name of the machine the application runs on</li>
	 * <li>{username}: username from the credentials found using authAlias, or the username attribute</li>
	 * <li>{password}: password from the credentials found using authAlias, or the password attribute</li>
	 * <li>{fixeddate}: fake date, for testing only</li>
	 * <li>{fixeduid}: fake uid, for testing only</li>
	 * <li>{fixedhostname}: fake hostname, for testing only</li>
	 * </ul>
	 * A guid can be generated using {hostname}_{uid}, see also
	 * <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/rmi/server/uid.html">http://java.sun.com/j2se/1.4.2/docs/api/java/rmi/server/uid.html</a> for more information about (g)uid's or
	 * <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/util/uuid.html">http://docs.oracle.com/javase/1.5.0/docs/api/java/util/uuid.html</a> for more information about uuid's.
	 * <br/>
	 * When combining a date or time <code>pattern</code> like {now} or {fixeddate} with a DATE, TIME, DATETIME or TIMESTAMP <code>type</code>, the effective value of the attribute
	 * <code>formatString</code> must match the effective value of the formatString in the <code>pattern</code>.
	 */
	public void setPattern(String string) {
		pattern = string;
	}

	/** Alias used to obtain username and password, used when a <code>pattern</code> containing {username} or {password} is specified */
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	/** Default username that is used when a <code>pattern</code> containing {username} is specified */
	public void setUsername(String string) {
		username = string;
	}

	/** Default password that is used when a <code>pattern</code> containing {password} is specified */
	public void setPassword(String string) {
		password = string;
	}

	/** If set <code>true</code> pattern elements that cannot be resolved to a parameter or sessionKey are silently resolved to an empty string */
	public void setIgnoreUnresolvablePatternElements(boolean b) {
		ignoreUnresolvablePatternElements = b;
	}

	/**
	 * Used in combination with types <code>DATE</code>, <code>TIME</code>, <code>DATETIME</code> and <code>TIMESTAMP</code> to parse the raw parameter string data into an object of the respective type
	 * @ff.default depends on type
	 */
	public void setFormatString(String string) {
		formatString = string;
	}

	/**
	 * Used in combination with type <code>NUMBER</code>
	 * @ff.default system default
	 */
	public void setDecimalSeparator(String string) {
		decimalSeparator = string;
	}

	/**
	 * Used in combination with type <code>NUMBER</code>
	 * @ff.default system default
	 */
	public void setGroupingSeparator(String string) {
		groupingSeparator = string;
	}

	/**
	 * If set (>=0) and the length of the value of the parameter falls short of this minimum length, the value is padded
	 * @ff.default -1
	 */
	public void setMinLength(int i) {
		minLength = i;
	}

	/**
	 * If set (>=0) and the length of the value of the parameter exceeds this maximum length, the length is trimmed to this maximum length
	 * @ff.default -1
	 */
	public void setMaxLength(int i) {
		maxLength = i;
	}

	/** Used in combination with type <code>number</code>; if set and the value of the parameter exceeds this maximum value, this maximum value is taken */
	public void setMaxInclusive(String string) {
		maxInclusiveString = string;
	}

	/** Used in combination with type <code>number</code>; if set and the value of the parameter falls short of this minimum value, this minimum value is taken */
	public void setMinInclusive(String string) {
		minInclusiveString = string;
	}

	/**
	 * If set to <code>true</code>, the value of the parameter will not be shown in the log (replaced by asterisks)
	 * @ff.default <code>false</code>
	 */
	public void setHidden(boolean b) {
		hidden = b;
	}

	/**
	 * Set the mode of the parameter, which determines if the parameter is an INPUT, OUTPUT, or INOUT.
	 * This parameter only has effect for {@link nl.nn.adapterframework.jdbc.StoredProcedureQuerySender}.
	 * An OUTPUT parameter does not need to have a value specified, but does need to have the type specified.
	 * Parameter values will not be updated, but output values will be put into the result of the
	 * {@link nl.nn.adapterframework.jdbc.StoredProcedureQuerySender}.
	 * <b/>
	 * If not specified, the default is INPUT.
	 *
	 * @param mode INPUT, OUTPUT or INOUT.
	 */
	public void setMode(ParameterMode mode) {
		this.mode = mode;
	}
}
