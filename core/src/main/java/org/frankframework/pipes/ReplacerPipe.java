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

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlEncodingUtils;

/**
 * Replaces all occurrences of one string with another.
 * Optionally strips or replaces XML non-printable characters.
 *
 * @author Gerrit van Brakel
 * @since 4.2
 */
@ElementType(ElementTypes.TRANSLATOR)
public class ReplacerPipe extends FixedForwardPipe {

	private String find;
	private String replace;
	private String lineSeparatorSymbol = null;
	private boolean replaceNonXmlChars = false;
	private String replaceNonXmlChar = null;
	private boolean allowUnicodeSupplementaryCharacters = false;

	{
		setSizeStatistics(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getFind())) {
			if (getReplace() == null) {
				throw new ConfigurationException("cannot have a null replace-attribute");
			}
			log.info("finds [{}] replaces with [{}]", getFind(), getReplace());
			if (!StringUtils.isEmpty(getLineSeparatorSymbol())) {
				find = find != null ? find.replace(lineSeparatorSymbol, System.getProperty("line.separator")) : null;
				replace = replace != null ? replace.replace(lineSeparatorSymbol, System.getProperty("line.separator")) : null;
			}
		}
		if (isReplaceNonXmlChars()) {
			if (getReplaceNonXmlChar() != null) {
				if (getReplaceNonXmlChar().length() > 1) {
					throw new ConfigurationException("replaceNonXmlChar [" + getReplaceNonXmlChar() + "] has to be one character");
				}
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String input;
		try {
			input = message.asString();
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
		return new PipeRunResult(getSuccessForward(), input);
	}

	/**
	 * Sets the string that is searched for. Newlines can be represented
	 * by the {@link #setLineSeparatorSymbol(String)}.
	 */
	public void setFind(String find) {
		this.find = find;
	}

	public String getFind() {
		return find;
	}

	/**
	 * Sets the string that will replace each of the occurrences of the find-string. Newlines can be represented
	 * 	 * by the {@link #setLineSeparatorSymbol(String)}.
	 */
	public void setReplace(String replace) {
		this.replace = replace;
	}

	public String getReplace() {
		return replace;
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

	/**
	 * Replace all characters that are non-printable according to the XML specification with
	 * the value specified in {@link #setReplaceNonXmlChar(String)}.
	 * <p>
	 *     <b>NB:</b> This will only replace or remove characters considered non-printable. This
	 *     will not check if a given character is valid in the particular way it is used. Thus it will
	 *     not remove or replace, for instance, a single {@code '&'} character.
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

	public boolean isReplaceNonXmlChars() {
		return replaceNonXmlChars;
	}

	/**
	 * character that will replace each non valid xml character (empty string is also possible) (use &amp;#x00bf; for inverted question mark)
	 *
	 * @ff.default empty string
	 */
	public void setReplaceNonXmlChar(String replaceNonXmlChar) {
		this.replaceNonXmlChar = replaceNonXmlChar;
	}

	public String getReplaceNonXmlChar() {
		return replaceNonXmlChar;
	}

	/**
	 * Whether to allow Unicode supplementary characters (like a smiley) during {@link XmlEncodingUtils#replaceNonValidXmlCharacters(String, char, boolean, boolean) replaceNonValidXmlCharacters}
	 *
	 * @ff.default false
	 */
	public void setAllowUnicodeSupplementaryCharacters(boolean b) {
		allowUnicodeSupplementaryCharacters = b;
	}

	public boolean isAllowUnicodeSupplementaryCharacters() {
		return allowUnicodeSupplementaryCharacters;
	}

}
