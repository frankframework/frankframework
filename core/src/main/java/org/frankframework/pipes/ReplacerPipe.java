/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.stream.ReplaceNonXmlCharsInputStream;
import org.frankframework.util.stream.ReplacingInputStream;
import org.frankframework.util.stream.ReplacingPropertyVariablesInputStream;
import org.frankframework.util.stream.ReplacingParameterVariablesInputStream;

/**
 * This Pipe is used to replace values in a few ways. The following steps are performed:
 * <ol>
 * <li>If the attribute <code>find</code> is provided, the pipe will attempt to replace the provided value with the content of the attribute <code>replace</code>.</li>
 * <li>The resulting string is substituted based on the parameters of this pipe. It will replace values in the input enclosed
 * with <code>?{...}</code>, for instance text like: <code>?{parameterOne}</code> in combination with a parameter <code>parameterOne</code> will use the value of this {@link Parameter}.
 * If a parameter for the given value is not found, it will not be replaced and the <code>?{parameterOne}</code> value will remain in the output.</li>
 * <p>
 * <p>
 * <li>If attribute <code>substituteVars</code> is {@code true}, then expressions <code>${...}</code> are substituted using
 * system properties, session variables and application properties. Please note that no <code>${...}</code> patterns are left in the input. </li>
 * </ol>
 * This pipe may convert non-xml characters, but it's a text based replace. Find and replace characters should therefore match the input message directly.
 *
 * @ff.tip See {@link Parameter} to see how parameter values are determined.
 * @ff.info Special characters such as {@literal \r} are interpreted by the XML Parser and do not propagate to the FrankElement. In order to use such special characters you could create a property and refer it in the attribute. (e.g. {@code find="${special-property-name}"} ).
 *
 * @author Gerrit van Brakel
 * @ff.parameters Used for substitution. For a parameter named <code>xyz</code>, the string <code>?{xyz}</code> is substituted by the parameter's value.
 * @since 4.2
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class ReplacerPipe extends FixedForwardPipe {

	private boolean allowUnicodeSupplementaryCharacters = false;
	private String find;
	private String lineSeparatorSymbol = null;
	private String replace;
	private String nonXmlReplacementCharacter = null;
	private boolean replaceNonXmlChars = false;
	private @Getter boolean substituteVars;
	private AppConstants appConstants;

	{
		setSizeStatistics(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		appConstants = AppConstants.getInstance(getConfigurationClassLoader());

		if (StringUtils.isNotEmpty(getFind())) {
			log.debug("finds [{}] replaces with [{}]", getFind(), getReplace());

			if (StringUtils.isNotEmpty(getLineSeparatorSymbol())) {
				find = find != null ? find.replace(lineSeparatorSymbol, System.lineSeparator()) : null;
				replace = replace != null ? replace.replace(lineSeparatorSymbol, System.lineSeparator()) : null;
			}
		}

		if (isReplaceNonXmlChars() && getNonXmlReplacementCharacter() != null && getNonXmlReplacementCharacter().length() > 1) {
			throw new ConfigurationException("replaceNonXmlChar [" + getNonXmlReplacementCharacter() + "] has to be one character");
		}
	}

	@NonNull
	@Override
	public PipeRunResult doPipe(@NonNull Message message, @NonNull PipeLineSession session) throws PipeRunException {
		try {
			// Create a ReplacingInputStream for find/replace
			final InputStream replacingInputStream = StringUtils.isNotEmpty(getFind()) ? new ReplacingInputStream(message.asInputStream(), find, replace) : message.asInputStream();

			// Replace NonValidXmlCharacters
			final InputStream replaceNonXmlInputStream = isReplaceNonXmlChars() ? new ReplaceNonXmlCharsInputStream(replacingInputStream, nonXmlReplacementCharacter, allowUnicodeSupplementaryCharacters) : replacingInputStream;

			// Replace Parameters
			InputStream replaceParametersStream = replaceParameters(replaceNonXmlInputStream, message, session);

			// Wrap for 'substitute vars' if necessary
			InputStream inputStream = wrapWithSubstituteVarsInputStreamIfNeeded(replaceParametersStream);

			Message result = new Message(inputStream);
			return new PipeRunResult(getSuccessForward(), result);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
	}

	private InputStream replaceParameters(InputStream replaceNonXmlInputStream, Message message, PipeLineSession session) throws PipeRunException {
		if (getParameterList().size() == 0) {
			return replaceNonXmlInputStream;
		}

		try {
			ParameterValueList pvl = getParameterList().getValues(message, session);
			return new ReplacingParameterVariablesInputStream(replaceNonXmlInputStream, pvl);
		} catch (ParameterException e) {
			throw new PipeRunException(this, "exception extracting parameters", e);
		}
	}

	/**
	 * If {@code subsituteVars} is true, we need to wrap the inputStream again to substitute ${} syntax variables with
	 * system properties, session variables and application properties.
	 */
	private InputStream wrapWithSubstituteVarsInputStreamIfNeeded(InputStream replaceParametersStream) {
		if (substituteVars) {
			return new ReplacingPropertyVariablesInputStream(replaceParametersStream, "$", appConstants);
		}

		return replaceParametersStream;
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
	 * by the {@link #setLineSeparatorSymbol(String)}.
	 * If left empty, the 'find' values will be omitted, and nothing will be replaced.
	 */
	public void setReplace(String replace) {
		this.replace = StringUtils.defaultIfBlank(replace, null);
	}

	/**
	 * Sets the string the representation in find and replace of the line separator.
	 */
	public String getLineSeparatorSymbol() {
		return lineSeparatorSymbol;
	}

	/**
	 * Replaces occurences of the given lineSeperatorSymbol with the host system's lineSeparator in the find and replace values.
	 * On UNIX systems, it returns {@literal \n}; on Microsoft Windows systems it returns {@literal \r\n}.
	 * <br/>
	 * This may be needed when your {@link #setFind(String)} and {@link #setReplace(String)} strings must match the content exactly
	 * and the input message's line-separator can differ. This attribute is not used for the input message.
	 */
	public void setLineSeparatorSymbol(String string) {
		lineSeparatorSymbol = string;
	}

	public boolean isReplaceNonXmlChars() {
		return replaceNonXmlChars;
	}

	/**
	 * Replace all characters that are non-printable according to the XML specification with
	 * the value specified in {@link #setNonXmlReplacementCharacter(String)}.
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

	public String getNonXmlReplacementCharacter() {
		return nonXmlReplacementCharacter;
	}

	/**
	 * Character that will replace each non-valid xml character (empty string is also possible) (use &amp;#x00bf; for inverted question mark).
	 * Note that the find/replace actions are done based on TEXT and not XML content.
	 *
	 * @ff.default empty string
	 */
	public void setNonXmlReplacementCharacter(String nonXmlReplacementCharacter) {
		this.nonXmlReplacementCharacter = nonXmlReplacementCharacter;
	}

	@Deprecated(since = "8.2", forRemoval = true)
	@ConfigurationWarning("The attribute 'replaceNonXmlChar' has been renamed to 'nonXmlReplacementCharacter' for readability")
	public void setReplaceNonXmlChar(String replaceNonXmlChar) {
		setNonXmlReplacementCharacter(replaceNonXmlChar);
	}

	public boolean isAllowUnicodeSupplementaryCharacters() {
		return allowUnicodeSupplementaryCharacters;
	}

	/**
	 * Whether to allow Unicode supplementary characters (like a smiley) during {@link XmlEncodingUtils#replaceNonValidXmlCharacters(String,
	 * char, boolean, boolean) replaceNonValidXmlCharacters}
	 *
	 * @ff.default false
	 */
	public void setAllowUnicodeSupplementaryCharacters(boolean allowUnicodeSupplementaryCharacters) {
		this.allowUnicodeSupplementaryCharacters = allowUnicodeSupplementaryCharacters;
	}

	/**
	 * Should properties (values between <code>${</code> and <code>}</code>) be resolved.
	 *
	 * @ff.default false
	 */
	public void setSubstituteVars(boolean substitute) {
		this.substituteVars = substitute;
	}
}
