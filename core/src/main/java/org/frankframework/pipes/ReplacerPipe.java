/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringResolver;
import org.frankframework.util.XmlEncodingUtils;

/**
 * This Pipe is used to replace values in a few ways. The following steps are performed:
 * <ol>
 * <li>If <code>find</code> is provided, it will be replaced by <code>replace</code></li>
 * <li>The resulting string is substituted based on the parameters of this pipe. This step depends on attribute <code>replaceFixedParams</code>.
 * Assume that there is a parameter with name <code>xyz</code>. If <code>replaceFixedParams</code> is <code>false</code>, then
 * each occurrence of <code>?{xyz}</code> is replaced by the parameter's value. Otherwise, the text <code>xyz</code>
 * is substituted. See {@link Parameter} to see how parameter values are determined.</li>
 * <li>If attribute <code>substituteVars</code> is <code>true</code>, then expressions <code>${...}</code> are substituted using
 * system properties, pipelinesession variables and application properties. Please note that
 * no <code>${...}</code> patterns are left if the initial string came from attribute <code>returnString</code>, because
 * any <code>${...}</code> pattern in attribute <code>returnString</code> is substituted when the configuration is loaded.</li>
 * </ol>
 *
 * @ff.parameters Used for substitution. For a parameter named <code>xyz</code>, the string <code>?{xyz}</code> or
 *  * <code>xyz</code> (if <code>replaceFixedParams</code> is true) is substituted by the parameter's value.
 *
 * @author Gerrit van Brakel
 * @since 4.2
 */
@ElementType(ElementTypes.TRANSLATOR)
public class ReplacerPipe extends FixedForwardPipe {

	private static final String SUBSTITUTION_START_DELIMITER = "?";
	private boolean allowUnicodeSupplementaryCharacters = false;
	private AppConstants appConstants;
	private String find;
	private String formatString;
	private String lineSeparatorSymbol = null;
	private String replace;
	private String replaceNonXmlChar = null;
	private boolean replaceNonXmlChars = false;
	private @Getter boolean substituteVars;

	{
		setSizeStatistics(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		appConstants = AppConstants.getInstance(getConfigurationClassLoader());

		if (StringUtils.isNotEmpty(getFind())) {
			if (getReplace() == null) {
				throw new ConfigurationException("cannot have a null replace-attribute");
			}
			log.info("finds [{}] replaces with [{}]", getFind(), getReplace());
			if (!StringUtils.isEmpty(getLineSeparatorSymbol())) {
				find = find != null ? find.replace(lineSeparatorSymbol, System.lineSeparator()) : null;
				replace = replace != null ? replace.replace(lineSeparatorSymbol, System.lineSeparator()) : null;
			}
		}

		if (isReplaceNonXmlChars() && getReplaceNonXmlChar() != null && getReplaceNonXmlChar().length() > 1) {
			throw new ConfigurationException("replaceNonXmlChar [" + getReplaceNonXmlChar() + "] has to be one character");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String input;

		try {
			if (StringUtils.isNotEmpty(formatString)) {
				input = replaceSingle(formatString, "message", message.asString());
			} else {
				input = message.asString();
			}
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		if (StringUtils.isEmpty(input)) {
			return new PipeRunResult(getSuccessForward(), input);
		}

		if (StringUtils.isNotEmpty(getFind())) {
			input = input.replace(getFind(), getReplace());
		}
		if (isReplaceNonXmlChars()) {
			if (StringUtils.isEmpty(getReplaceNonXmlChar())) {
				input = XmlEncodingUtils.stripNonValidXmlCharacters(input, isAllowUnicodeSupplementaryCharacters());
			} else {
				input = XmlEncodingUtils.replaceNonValidXmlCharacters(input, getReplaceNonXmlChar().charAt(0), false, isAllowUnicodeSupplementaryCharacters());
			}
		}

		input = substituteVars(input, session);

		input = replaceParameters(input, message, session);

		return new PipeRunResult(getSuccessForward(), input);
	}

	private String substituteVars(String input, PipeLineSession session) {
		if (isSubstituteVars()) {
			return StringResolver.substVars(input, session, appConstants);
		}

		return input;
	}

	private String replaceParameters(String input, Message message, PipeLineSession session) throws PipeRunException {
		if (!getParameterList().isEmpty()) {
			try {
				ParameterValueList pvl = getParameterList().getValues(message, session);
				for (ParameterValue pv : pvl) {
					input = replaceSingle(input, pv.getName(), pv.asStringValue(""));
				}
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception extracting parameters", e);
			}
		}

		return input;
	}

	private String replaceSingle(String value, String replaceFromValue, String replaceTo) {
		final String replaceFrom = SUBSTITUTION_START_DELIMITER + "{" + replaceFromValue + "}";

		return value.replace(replaceFrom, replaceTo);
	}

	public String getFind() {
		return find;
	}

	/**
	 * Sets the string that is searched for. Newlines can be represented
	 * by the {@link #setLineSeparatorSymbol(String)}.
	 */
	public void setFind(String find) {
		this.find = find;
	}

	public String getReplace() {
		return replace;
	}

	/**
	 * Sets the string that will replace each of the occurrences of the find-string. Newlines can be represented
	 * * by the {@link #setLineSeparatorSymbol(String)}.
	 */
	public void setReplace(String replace) {
		this.replace = replace;
	}

	/**
	 * Sets the string the representation in find and replace of the line separator.
	 */
	public String getLineSeparatorSymbol() {
		return lineSeparatorSymbol;
	}

	/** sets the string that will represent the line-separator in the {@link #setFind(String)} and {@link #setReplace(String)} strings. */
	public void setLineSeparatorSymbol(String string) {
		lineSeparatorSymbol = string;
	}

	public boolean isReplaceNonXmlChars() {
		return replaceNonXmlChars;
	}

	/**
	 * Replace all characters that are non-printable according to the XML specification with
	 * the value specified in {@link #setReplaceNonXmlChar(String)}.
	 * <p>
	 * <b>NB:</b> This will only replace or remove characters considered non-printable. This
	 * will not check if a given character is valid in the particular way it is used. Thus it will
	 * not remove or replace, for instance, a single {@code '&'} character.
	 * </p>
	 * <p>
	 * See also:
	 * 	<ul>
	 * 	    <li>XmlEncodingUtils {@link XmlEncodingUtils#replaceNonValidXmlCharacters(String, char, boolean, boolean) replaceNonValidXmlCharacters}</li>
	 * 	    <li><a href="https://www.w3.org/TR/xml/#charsets">Character ranges specified in the XML Specification</a></li>
	 * 	</ul>
	 * </p>
	 *
	 * @ff.default false
	 */
	public void setReplaceNonXmlChars(boolean b) {
		replaceNonXmlChars = b;
	}

	public String getReplaceNonXmlChar() {
		return replaceNonXmlChar;
	}

	/**
	 * character that will replace each non-valid xml character (empty string is also possible) (use &amp;#x00bf; for inverted question mark)
	 *
	 * @ff.default empty string
	 */
	public void setReplaceNonXmlChar(String replaceNonXmlChar) {
		this.replaceNonXmlChar = replaceNonXmlChar;
	}

	public boolean isAllowUnicodeSupplementaryCharacters() {
		return allowUnicodeSupplementaryCharacters;
	}

	/**
	 * Whether to allow Unicode supplementary characters (like a smiley) during {@link XmlEncodingUtils#replaceNonValidXmlCharacters(String, char, boolean, boolean) replaceNonValidXmlCharacters}
	 *
	 * @ff.default false
	 */
	public void setAllowUnicodeSupplementaryCharacters(boolean b) {
		allowUnicodeSupplementaryCharacters = b;
	}

	/**
	 * Should values between ${ and } be resolved. If true, the search order of replacement values is:
	 * system properties (1), PipelineSession variables (2), application properties (3).
	 *
	 * @ff.default false
	 */
	public void setSubstituteVars(boolean substitute) {
		this.substituteVars = substitute;
	}

	/**
	 * If set, this pattern is used as a pattern to be able to refer to the incoming message. For instance, use "output: [?{message}]" to wrap the
	 * incoming message string in the result.
	 * Please note that "message" in the ?{message} syntax is the reserved word here.
	 *
	 * Will only be used if the value is not empty.
	 *
	 * @ff.default empty string
	 */
	public void setFormatString(String formatString) {
		this.formatString = formatString;
	}
}
