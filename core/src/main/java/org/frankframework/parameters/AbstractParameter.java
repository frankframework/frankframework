/*
   Copyright 2013, 2016, 2019, 2020 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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
package org.frankframework.parameters;

import static org.frankframework.util.StringUtil.hide;

import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import com.jayway.jsonpath.JsonPath;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationUtils;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.DocumentedEnum;
import org.frankframework.doc.EnumLabel;
import org.frankframework.jdbc.StoredProcedureQuerySender;
import org.frankframework.json.JsonException;
import org.frankframework.json.JsonUtil;
import org.frankframework.pipes.PutSystemDateInSession;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.UUIDUtil;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Generic parameter definition.
 *
 * <p>
 * A parameter resembles an attribute. However, while attributes get their value at configuration-time,
 * parameters get their value at the time of processing the message. Value can be retrieved from the message itself,
 * a fixed value, or from the pipelineSession. If this does not result in a value (or if neither of these is specified), a default value
 * can be specified. If an XPathExpression, XSLT stylesheet or JSONPathExpression is specified, it will be applied to the message, the value retrieved
 * from the pipelineSession or the fixed value specified. If the transformation produces no output, the default value
 * of the parameter is taken if provided.
 * </p>
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
 * The current version of parameter supports the {@code value} attribute, that is sufficient to set a fixed value.
 * @author Gerrit van Brakel
 * @ff.parameters Parameters themselves can have parameters too, for instance if a XSLT transformation is used, that transformation can have parameters.
 */
@SuppressWarnings("removal")
public abstract class AbstractParameter implements IConfigurable, IWithParameters, IParameter {
	private static final Logger LOG = LogManager.getLogger(Parameter.class);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	public static final String FIXEDUID ="0a1b234c--56de7fa8_9012345678b_-9cd0";
	public static final String FIXEDHOSTNAME ="MYHOST000012345";

	private String name = null;
	private @Getter ParameterType type = ParameterType.STRING;
	private @Getter String sessionKey = null;
	private @Getter String sessionKeyXPath = null;
	private @Getter String sessionKeyJPath = null;
	private JsonPath sessionKeyJsonPath = null;
	private @Getter String contextKey = null;
	private @Getter String xpathExpression = null;
	private @Getter String jsonPathExpression = null;
	private JsonPath jsonPath = null;
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
	private @Getter int minLength = -1;
	private @Getter int maxLength = -1;
	private @Getter boolean hidden = false;
	private @Getter boolean removeNamespaces=false;
	private @Getter int xsltVersion = 0; // set to 0 for auto-detect.

	private TransformerPool transformerPool = null;
	private TransformerPool tpDynamicSessionKey = null;
	protected @Nonnull ParameterList paramList = new ParameterList();
	private boolean configured = false;
	private CredentialFactory cf;

	private List<DefaultValueMethods> defaultValueMethodsList;

	@Getter
	private ParameterMode mode = ParameterMode.INPUT;

	public enum ParameterMode {
		INPUT, OUTPUT, INOUT
	}

	public enum DefaultValueMethods implements DocumentedEnum {
		@EnumLabel("defaultValue")	DEFAULTVALUE,
		@EnumLabel("sessionKey")	SESSIONKEY,
		@EnumLabel("pattern")		PATTERN,
		@EnumLabel("value")			VALUE,
		@EnumLabel("input") 		INPUT;
	}

	@Override
	public void addParameter(IParameter p) {
		paramList.add(p);
	}

	@Override
	public @Nonnull ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getSessionKey()) && StringUtils.isNotEmpty(getSessionKeyXPath())) {
			throw new ConfigurationException("Parameter ["+getName()+"] cannot have both sessionKey and sessionKeyXPath specified");
		}
		if (StringUtils.isNotEmpty(getXpathExpression()) && StringUtils.isNotEmpty(getJsonPathExpression())) {
			throw new ConfigurationException("Parameter [" +getName()+"] cannot have both xpathExpression and jpathExpression");
		}
		paramList.configure();
		if (StringUtils.isNotEmpty(getXpathExpression()) || StringUtils.isNotEmpty(styleSheetName)) {
			OutputType outputType = getType() == ParameterType.XML
					|| getType() == ParameterType.NODE
					|| getType() == ParameterType.DOMDOC ? OutputType.XML : OutputType.TEXT;

			boolean includeXmlDeclaration = false;

			transformerPool = TransformerPool.configureTransformer0(this, getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), outputType, includeXmlDeclaration, paramList, getXsltVersion());
		} else {
			if (StringUtils.isEmpty(getPattern()) && !paramList.isEmpty()) {
				throw new ConfigurationException("Parameter [" + getName() + "] can only have parameters itself if a styleSheetName, xpathExpression or pattern is specified");
			}
		}
		if (StringUtils.isNotEmpty(getSessionKeyXPath())) {
			tpDynamicSessionKey = TransformerPool.configureTransformer(this, getNamespaceDefs(), getSessionKeyXPath(), null, OutputType.TEXT,false,null);
		}
		if (getType() == null) {
			LOG.info("parameter [{} has no type. Setting the type to [{}]", this::getType, ()->ParameterType.STRING);
			setType(ParameterType.STRING);
		}
		jsonPath = JsonUtil.compileJsonPath(jsonPathExpression);
		sessionKeyJsonPath = JsonUtil.compileJsonPath(sessionKeyJPath);

		configured = true;

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
		pool.transform(xmlSource, (Result) transformResult, pvl ==null? null : pvl.getValueMap());
		return (Document) transformResult.getNode();
	}

	/**
	 * if this returns true, then the input value must be repeatable, as it might be used multiple times.
	 */
	@Override
	public boolean requiresInputValueForResolution() {
		if (tpDynamicSessionKey != null || sessionKeyJPath != null) { // tpDynamicSessionKey or sessionKeyJPath are applied to the input message to retrieve the session key
			return true;
		}
		return StringUtils.isEmpty(getContextKey()) &&
				(StringUtils.isEmpty(getSessionKey()) && StringUtils.isEmpty(getValue()) && StringUtils.isEmpty(getPattern())
						|| getDefaultValueMethodsList().contains(DefaultValueMethods.INPUT)
				);
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return StringUtils.isEmpty(getContextKey()) && (
				sessionKey.equals(getSessionKey()) || getPattern() != null && getPattern().contains("{" + sessionKey + "}") || getParameterList().consumesSessionVariable(sessionKey)
		);
	}

	/**
	 * determines the raw value
	 */
	@Override
	public Object getValue(ParameterValueList alreadyResolvedParameters, Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		Object result = null;
		LOG.debug("Calculating value for Parameter [{}]", this::getName);
		if (!configured) {
			throw new ParameterException(getName(), "Parameter ["+getName()+"] not configured");
		}

		String requestedSessionKey;
		if (tpDynamicSessionKey != null) {
			try {
				requestedSessionKey = tpDynamicSessionKey.transformToString(message);
			} catch (Exception e) {
				throw new ParameterException(getName(), "SessionKey for parameter [" + getName() + "] exception on XML transformation to get name", e);
			}
		} else if (sessionKeyJsonPath != null) {
			try {
				requestedSessionKey = JsonUtil.evaluateJsonPathToSingleValue(sessionKeyJsonPath, message);
			} catch (JsonException e) {
				throw new ParameterException(getName(), "SessionKey for parameter [" + getName() + "] exception on JSON Path Evaluation to get name", e);
			}
		} else {
			requestedSessionKey = getSessionKey();
		}

		if (transformerPool != null) {
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
				Source source;
				if (getValue() != null) {
					source = XmlUtils.stringToSourceForSingleUse(getValue(), namespaceAware);
				} else if (StringUtils.isNotEmpty(requestedSessionKey)) {
					Object sourceObject = session.get(requestedSessionKey);
					if (getType() == ParameterType.LIST && sourceObject instanceof List) {
						// larva can produce the sourceObject as list
						//noinspection unchecked
						List<String> items = (List<String>) sourceObject;
						XmlBuilder itemsXml = new XmlBuilder("items");
						for (String item : items) {
							XmlBuilder itemXml = new XmlBuilder("item");
							itemXml.setValue(item);
							itemsXml.addSubElement(itemXml);
						}
						source = XmlUtils.stringToSourceForSingleUse(itemsXml.asXmlString(), namespaceAware);
					} else if (getType() == ParameterType.MAP && sourceObject instanceof Map) {
						// larva can produce the sourceObject as map
						Map<String, String> items = (Map<String, String>) sourceObject;
						XmlBuilder itemsXml = new XmlBuilder("items");
						for (String item : items.keySet()) {
							XmlBuilder itemXml = new XmlBuilder("item");
							itemXml.addAttribute("name", item);
							itemXml.setValue(items.get(item));
							itemsXml.addSubElement(itemXml);
						}
						source = XmlUtils.stringToSourceForSingleUse(itemsXml.asXmlString(), namespaceAware);
					} else {
						Message sourceMsg = Message.asMessage(sourceObject);
						if (StringUtils.isNotEmpty(getContextKey())) {
							sourceMsg = Message.asMessage(sourceMsg.getContext().get(getContextKey()));
						}
						if (!sourceMsg.isEmpty()) {
							LOG.debug("Parameter [{}] using sessionvariable [{}] as source for transformation", this::getName, () -> requestedSessionKey);
							source = sourceMsg.asSource();
						} else {
							LOG.debug("Parameter [{}] sessionvariable [{}] empty, no transformation will be performed", this::getName, () -> requestedSessionKey);
							source = null;
						}
					}
				} else if (StringUtils.isNotEmpty(getPattern())) {
					String sourceString = formatPattern(alreadyResolvedParameters, session);
					if (StringUtils.isNotEmpty(sourceString)) {
						LOG.debug("Parameter [{}] using pattern [{}] as source for transformation", this::getName, this::getPattern);
						source = XmlUtils.stringToSourceForSingleUse(sourceString, namespaceAware);
					} else {
						LOG.debug("Parameter [{}] pattern [{}] empty, no transformation will be performed", this::getName, this::getPattern);
						source = null;
					}
				} else if (message != null) {
					if (StringUtils.isNotEmpty(getContextKey())) {
						source = Message.asMessage(message.getContext().get(getContextKey())).asSource();
					} else {
						source = message.asSource();
					}
				} else {
					source = null;
				}
				if (source != null) {
					if (isRemoveNamespaces()) {
						// TODO: There should be a more efficient way to do this
						String rnResult = XmlUtils.removeNamespaces(XmlUtils.source2String(source));
						source = XmlUtils.stringToSource(rnResult);
					}
					ParameterValueList pvl = paramList.getValues(message, session, namespaceAware);
					switch (getType()) {
						case NODE:
							return transformToDocument(source, pvl).getFirstChild();
						case DOMDOC:
							return transformToDocument(source, pvl);
						default:
							String transformResult = transformerPool.transformToString(source, pvl.getValueMap());
							if (StringUtils.isNotEmpty(transformResult)) {
								result = transformResult;
							}
					}
				}
			} catch (Exception e) {
				throw new ParameterException(getName(), "Parameter ["+getName()+"] exception on transformation to get parametervalue", e);
			}
		} else if (jsonPath != null) {
			/*
			 * determine source for JPath evaluation same as for XSLT / XPath, from
			 * 1) value attribute
			 * 2) requestedSessionKey
			 * 3) pattern
			 * 4) input message
			 *
			 * N.B. this order differs from untransformed parameters
			 */
			Object input;
			if (getValue() != null) {
				input = getValue();
			} else if (StringUtils.isNotEmpty(requestedSessionKey)) {
				input = getParameterValueFromSessionKey(session, requestedSessionKey);
			} else if (StringUtils.isNotEmpty(getPattern())) {
				input = formatPattern(alreadyResolvedParameters, session);
			} else if (message != null) {
				input = getParameterValueFromInputMessage(message);
			} else {
				input = null;
			}
			if (input != null) {
				try {
					result = JsonUtil.evaluateJsonPath(jsonPath, input);
				} catch (JsonException e) {
					throw new ParameterException(getName(), e);
				}
			}
		} else {
			/*
			 * No XSLT transformation or JPath evaluation, determine primary result from
			 * 1) requestedSessionKey
			 * 2) pattern
			 * 3) value attribute
			 * 4) input message
			 *
			 * N.B. this order differs from transformed parameters.
			 */
			if (StringUtils.isNotEmpty(requestedSessionKey)) {
				result = getParameterValueFromSessionKey(session, requestedSessionKey);
			} else if (StringUtils.isNotEmpty(getPattern())) {
				result = formatPattern(alreadyResolvedParameters, session);
			} else if (getValue()!=null) {
				result = getValue();
			} else if (message != null) {
				result = getParameterValueFromInputMessage(message);
			} else {
				return null;
			}
		}

		if (result instanceof Message msg && msg.isNull()) {
			result = null; // Cannot remove this, ParameterValues may return null, and should fallback to `other` defaults.
		}

		if (result != null && !"".equals(result)) {
			// If result is not empty log it
			final Object finalResult = result;
			LOG.debug("Parameter [{}] resolved to [{}]", this::getName, ()-> isHidden() ? hide(finalResult.toString()) : finalResult);
		} else {
			// If result is empty then return specified default value
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
							valueByDefault=message.asString();
						} catch (IOException e) {
							throw new ParameterException(getName(), e);
						}
						break;
					default:
						throw new IllegalArgumentException("Unknown defaultValues method ["+method+"]");
				}
			}
			if (valueByDefault != null) {
				result = valueByDefault;
				final Object finalResult = result;
				LOG.debug("Parameter [{}] resolved to default value [{}]", this::getName, ()-> isHidden() ? hide(finalResult.toString()) : finalResult);
			}
		}

		if (getMinLength() >= 0 || getMaxLength() >= 0) {
			result = applyMinAndMaxLengths(result);
		}

		if(result != null && getType().requiresTypeConversion) {
			try {
				if (result instanceof Message message1) {
					return getValueAsType(message1, namespaceAware);
				} else {
					try (Message message1 = Message.asMessage(result)) {
						return getValueAsType(message1, namespaceAware);
					}
				}
			} catch(IOException e) {
				throw new ParameterException(getName(), "Could not convert parameter ["+getName()+"] to String", e);
			}
		}

		return result;
	}

	private Object getParameterValueFromInputMessage(Message message) throws ParameterException {
		Object input;
		if (StringUtils.isNotEmpty(getContextKey())) {
			input = message.getContext().get(getContextKey());
		} else {
			input = message;
		}
		return input;
	}

	@Nullable
	private Object getParameterValueFromSessionKey(PipeLineSession session, String requestedSessionKey) {
		Object input = session.get(requestedSessionKey);
		if (input instanceof Message message1 && StringUtils.isNotEmpty(getContextKey())) {
			input = message1.getContext().get(getContextKey());
		}
		if (LOG.isDebugEnabled() && (input == null ||
				((input instanceof String string) && string.isEmpty()) ||
				((input instanceof Message message1) && message1.isEmpty()))) {
			LOG.debug("Parameter [{}] session variable [{}] is empty", this::getName, () -> requestedSessionKey);
		}
		return input;
	}

	private Object applyMinAndMaxLengths(final Object request) {
		if (request instanceof Message msg && msg.isRequestOfType(String.class)) { // Used by getMinLength and getMaxLength
			try {
				return applyMinAndMaxLengths(msg.asString()); // WARNING this removes the MessageContext
			} catch (IOException e) {
				// Already checked for String, so this should never happen
			}
		}

		if (request instanceof String str) {
			return applyMinAndMaxLengths(str);
		}

		// All other types
		return request;
	}

	private String applyMinAndMaxLengths(String stringResult) {
		if (getMinLength()>=0 && !(this instanceof NumberParameter)) { // Numbers are formatted left-pad with leading 0's opposed to right-pad spaces.
			if (stringResult.length() < getMinLength()) {
				LOG.debug("Padding parameter [{}] because length [{}] falls short of minLength [{}]", this::getName, stringResult::length, this::getMinLength);
				return StringUtils.rightPad(stringResult, getMinLength());
			}
		}
		if (getMaxLength()>=0) {
			if (stringResult.length() > getMaxLength()) { // Still trims length regardless of type
				LOG.debug("Trimming parameter [{}] because length [{}] exceeds maxLength [{}]", this::getName, stringResult::length, this::getMaxLength);
				return stringResult.substring(0, getMaxLength());
			}
		}
		return stringResult;
	}

	/** Converts raw data to configured parameter type */
	protected Object getValueAsType(@Nonnull Message request, boolean namespaceAware) throws ParameterException, IOException {
		Object result = request.asObject();

		LOG.debug("final result [{}][{}]", ()->result != null ? result.getClass().getName() : null, ()-> result);

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
				throw new ParameterException(getName(), new ParseException("Bracket is not closed", startNdx));
			}
			String substitutionPattern = pattern.substring(startNdx + 1, tmpEndNdx);

			// get value
			Object substitutionValue = getValueForFormatting(alreadyResolvedParameters, session, substitutionPattern);
			if (substitutionValue instanceof Message substitutionMessage) {
				try {
					params.add(substitutionMessage.asString());
				} catch (IOException e) {
					throw new ParameterException(getName(), "Cannot convert substitution pattern ["+ substitutionPattern +"] from message to string", e);
				}
			} else {
				params.add(substitutionValue);
			}
			formatPattern.append('{').append(paramPosition++);
		}
		try {
			return MessageFormat.format(formatPattern.toString(), params.toArray());
		} catch (Exception e) {
			throw new ParameterException(getName(), "Cannot parse ["+ formatPattern +"]", e);
		}
	}

	private Object preFormatDateType(String rawValue, String formatType, String patternFormatString) throws ParameterException {
		if ("date".equalsIgnoreCase(formatType) || "time".equalsIgnoreCase(formatType)) {
			DateFormat df = new SimpleDateFormat(StringUtils.isNotEmpty(patternFormatString) ? patternFormatString : DateFormatUtils.FORMAT_DATETIME_GENERIC);
			try {
				return df.parse(rawValue);
			} catch (ParseException e) {
				throw new ParameterException(getName(), "Cannot parse [" + rawValue + "] as date", e);
			}
		}

		return rawValue;
	}

	private Object getValueForFormatting(ParameterValueList alreadyResolvedParameters, PipeLineSession session, String targetPattern) throws ParameterException {
		String[] patternElements = targetPattern.split(",");
		String name = patternElements[0].trim();
		String formatType = patternElements.length>1 ? patternElements[1].trim() : null;
		String formatString = patternElements.length>2 ? patternElements[2].trim() : null;

		ParameterValue paramValue = alreadyResolvedParameters.get(name);
		Object substitutionValue = paramValue == null ? null : paramValue.getValue();

		if (substitutionValue == null) {
			Object substitutionValueMessage = session.get(name);
			if (substitutionValueMessage != null) {
				if (substitutionValueMessage instanceof Date substitutionValueDate) {
					substitutionValue = getSubstitutionValueForDate(substitutionValueDate, formatType);
				} else if (substitutionValueMessage instanceof String stringValue) {
					substitutionValue = preFormatDateType(stringValue, formatType, formatString);
				} else {
					substitutionValue = session.getString(name);
					if (substitutionValue == null) throw new ParameterException(getName(), "Cannot get substitution value from session key: " + name);
				}
			}
		}
		if (substitutionValue == null) {
			String namelc=name.toLowerCase();
			switch (namelc) {
				case "now":
					if ("date".equalsIgnoreCase(formatType) || "time".equalsIgnoreCase(formatType)) {
						substitutionValue = new Date();
					} else{
						substitutionValue = formatDateToString(new Date(), formatString);
					}

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
						throw new ParameterException(getName(), "Parameter pattern [" + name + "] only allowed in stub mode");
					}

					// Parameter can be provided as a Date or a String. If using session.getString on a Date parameter, it will be formatted incorrectly
					Object fixedDateTime = session.get(PutSystemDateInSession.FIXEDDATE_STUB4TESTTOOL_KEY);

					if (fixedDateTime != null) {
						if (fixedDateTime instanceof Date date) {
							substitutionValue = getSubstitutionValueForDate(date, formatType);
						} else if (fixedDateTime instanceof String string) {
							substitutionValue = preFormatDateType(string, formatType, formatString);
						}
					} else {
						// Get the default value
						substitutionValue = preFormatDateType(PutSystemDateInSession.FIXEDDATETIME, formatType, DateFormatUtils.FORMAT_DATETIME_GENERIC);
					}

					break;
				case "fixeduid":
					if (!ConfigurationUtils.isConfigurationStubbed(configurationClassLoader)) {
						throw new ParameterException(getName(), "Parameter pattern [" + name + "] only allowed in stub mode");
					}
					substitutionValue = FIXEDUID;
					break;
				case "fixedhostname":
					if (!ConfigurationUtils.isConfigurationStubbed(configurationClassLoader)) {
						throw new ParameterException(getName(), "Parameter pattern [" + name + "] only allowed in stub mode");
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
				throw new ParameterException(getName(), "Parameter or session variable with name [" + name + "] in pattern [" + getPattern() + "] cannot be resolved");
			}
		}
		return substitutionValue;
	}

	/**
	 * @return the date when the format type is set, so the formatter knows how to format the Date object or return the date formatted
	 * as a String with the default format.
	 */
	private Object getSubstitutionValueForDate(Date date, String formatType) {
		return (formatType != null) ? date : formatDateToString(date, null);
	}

	/**
	 * @return the given date formatted by the given patternFormatString or the DateFormatUtils.FORMAT_DATETIME_GENERIC pattern if empty
	 */
	private String formatDateToString(Date date, String patternFormatString) {
		DateFormat df = new SimpleDateFormat(StringUtils.isNotEmpty(patternFormatString) ? patternFormatString : DateFormatUtils.FORMAT_DATETIME_GENERIC);
		return df.format(date);
	}

	@Override
	public String toString() {
		String expression = jsonPathExpression != null ? jsonPathExpression : xpathExpression;
		String expressionType = jsonPathExpression != null ? "jpathExpression" : "xpathExpression";
		String sessionKeyExpression = sessionKeyJPath != null ? sessionKeyJPath : sessionKeyXPath;
		String sessionKeyExpressionType = sessionKeyJPath != null ? "sessionKeyJPath" : "sessionKeyXPath";

		return "Parameter name=[" + name + "] defaultValue=[" + defaultValue + "] sessionKey=[" + sessionKey + "] "+ sessionKeyExpressionType+" =[" + sessionKeyExpression + "] "+expressionType+"=[" + expression + "] type=[" + type + "] value=[" + value + "]";
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

	protected void setType(ParameterType type) {
		this.type = type;
	}

	/** The value of the parameter, or the base for transformation using xpathExpression, jpathExpression or stylesheet, or formatting. */
	@Override
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Key of a PipelineSession-variable. <br/>If specified, the value of the PipelineSession variable is used as input for
	 * the xpathExpression, jpathExpression or stylesheet, instead of the current input message. <br/>If no xpathExpression, jpathExpression or stylesheet are
	 * specified, the value itself is returned. <br/>If the value '*' is specified, all existing sessionkeys are added as
	 * parameter of which the name starts with the name of this parameter. <br/>If also the name of the parameter has the
	 * value '*' then all existing sessionkeys are added as parameter (except tsReceived)
	 */
	@Override
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

	/** Instead of a fixed <code>sessionKey</code> it's also possible to use a JPath expression applied to the input message to extract the name of the session-variable. */
	public void setSessionKeyJPath(String string) {
		sessionKeyJPath = string;
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
	 * The JPath expression to extract the parameter value from the input or session-variable.
	 * The input should be JSON or XML formatted, if it is XML formatter a simple XML-to-JSON conversion is done.
	 * When {@code jsonPathExpression} is set, then the value of the parameter will be derived using the same order
	 * of precedence as with {@code xpathExpression}.
	 */
	public void setJsonPathExpression(String jsonPathExpression) {
		this.jsonPathExpression = jsonPathExpression;
	}

	/**
	 * If set to <code>2</code> or <code>3</code> a Saxon (net.sf.saxon) xslt processor 2.0 or 3.0 respectively will be used, otherwise xslt processor 1.0 (org.apache.xalan). <code>0</code> will auto-detect
	 * @ff.default 0
	 */
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}

	/**
	 * Namespace definitions for xpathExpression. Must be in the form of a comma or space separated list of
	 * <code>prefix=namespaceuri</code> definitions. One entry can be without a prefix, that will define the default namespace.
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
	 * to <code>session-variables</code> or other <code>parameters</code> using the {name-of-parameter} and is formatted using java.text.MessageFormat.
	 * <br/><b>NB: When referencing other <code>parameters</code> these MUST be defined before the parameter using pattern substitution.</b>
	 * <br/>
	 * <br/>
	 * If for instance <code>fname</code> is a parameter or session-variable that resolves to Eric, then the pattern
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

	/**
	 * If set to <code>true</code>, the value of the parameter will not be shown in the log (replaced by asterisks)
	 * @ff.default <code>false</code>
	 */
	public void setHidden(boolean b) {
		hidden = b;
	}

	/**
	 * Set the mode of the parameter, which determines if the parameter is an INPUT, OUTPUT, or INOUT.
	 * This parameter only has effect for {@link StoredProcedureQuerySender}.
	 * An OUTPUT parameter does not need to have a value specified, but does need to have the type specified.
	 * Parameter values will not be updated, but output values will be put into the result of the
	 * {@link StoredProcedureQuerySender}.
	 * <b/>
	 * If not specified, the default is INPUT.
	 *
	 * @param mode INPUT, OUTPUT or INOUT.
	 */
	public void setMode(ParameterMode mode) {
		this.mode = mode;
	}
}
