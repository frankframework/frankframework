/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlEncodingUtils;

/**
 * Replaces all occurrences of one string with another.
 *
 * @author Gerrit van Brakel
 * @since 4.2
 */
@ElementType(ElementTypes.TRANSLATOR)
public class ReplacerPipe extends FixedForwardPipe {

	private String find;
	private String replace;
	private String lineSeparatorSymbol=null;
	private boolean replaceNonXmlChars=false;
	private String replaceNonXmlChar=null;
	private boolean allowUnicodeSupplementaryCharacters=false;

	{
		setSizeStatistics(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
//		if (StringUtils.isEmpty(getFind())) {
//			throw new ConfigurationException("cannot have empty find-attribute");
//		}
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
			if (getReplaceNonXmlChar()!=null) {
				if (getReplaceNonXmlChar().length()>1) {
					throw new ConfigurationException("replaceNonXmlChar ["+getReplaceNonXmlChar()+"] has to be one character");
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
		return new PipeRunResult(getSuccessForward(),input);
	}

	/**
	 * Sets the string that is searched for.
	 */
	public void setFind(String find) {
		this.find = find;
	}
	public String getFind() {
		return find;
	}

	/**
	 * Sets the string that will replace each of the occurrences of the find-string.
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

	/** sets the string the representation in find and replace of the line separator */
	public void setLineSeparatorSymbol(String string) {
		lineSeparatorSymbol = string;
	}

	/** Replace all non XML chars (not in the <a href="http://www.w3.org/TR/2006/REC-xml-20060816/#NT-Char">character range as specified by the XML specification</a>) with {@link XmlEncodingUtils#replaceNonValidXmlCharacters(String, char, boolean, boolean) replaceNonValidXmlCharacters}
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
	 * @ff.default false
	 */
	public void setAllowUnicodeSupplementaryCharacters(boolean b) {
		allowUnicodeSupplementaryCharacters = b;
	}

	public boolean isAllowUnicodeSupplementaryCharacters() {
		return allowUnicodeSupplementaryCharacters;
	}

}
