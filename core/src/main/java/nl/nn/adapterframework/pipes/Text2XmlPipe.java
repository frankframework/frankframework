/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2022 WeAreFrank!

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.EncapsulatingReader;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe for converting text to or from xml. 
 *
 * 
 * @author J. Dekker
 */
public class Text2XmlPipe extends FixedForwardPipe {
	private @Getter String xmlTag;
	private @Getter boolean includeXmlDeclaration = true;
	private @Getter boolean splitLines = false;
	private @Getter boolean replaceNonXmlChars = true;
	private @Getter boolean useCdataSection = true;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(getXmlTag())) {
			throw new ConfigurationException("You have not defined xmlTag");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result;
		try {
			if (isSplitLines() && !Message.isEmpty(message)) {
				Reader reader = message.asReader();
				if (replaceNonXmlChars) {
					reader = new EncapsulatingReader(reader, "", "", true);
				}
				try (BufferedReader br = new BufferedReader(reader)) {
					StringBuilder buffer = new StringBuilder();

					String l;
					while ((l = br.readLine()) != null) {
						buffer.append("<line>"+addCdataSection(l)+"</line>");
					}
	
					result = buffer.toString();
				}
			} else if (replaceNonXmlChars && !Message.isEmpty(message)) {
				result = addCdataSection(XmlUtils.encodeCdataString(message.asString()));
			} else {
				result = addCdataSection((Message.isEmpty(message) ? null : message.asString()));
			}
		} catch (IOException e) {
			throw new PipeRunException(this, "Unexpected exception during splitting", e); 
		}

		String resultString = (isIncludeXmlDeclaration()?"<?xml version=\"1.0\" encoding=\"UTF-8\"?>":"") +
		"<" + getXmlTag() + ">"+result+"</" + xmlTag + ">";	
		return new PipeRunResult(getSuccessForward(), resultString);
	}

	private String addCdataSection(String input) {
		if (isUseCdataSection()) {
			return "<![CDATA["+ input +"]]>";
		} else {
			return input;
		}
	}

	/**
	 * The xml tag to encapsulate the text in
	 * @ff.mandatory
	 */
	public void setXmlTag(String xmlTag) {
		this.xmlTag = xmlTag;
	}

	/**
	 * Controls whether a declation is included above the xml text
	 * @ff.default true
	 */
	public void setIncludeXmlDeclaration(boolean b) {
		includeXmlDeclaration = b;
	}

	/**
	 * Controls whether the lines of the input are places in separated &lt;line&gt; tags
	 * @ff.default false
	 */
	public void setSplitLines(boolean b) {
		splitLines = b;
	}

	/**
	 * Replace all non xml chars (not in the <a href=\"http://www.w3.org/tr/2006/rec-xml-20060816/#nt-char\">character range as specified by the xml specification</a>) 
	 * with the inverted question mark (0x00bf)
	 * @ff.default true
	 */
	public void setReplaceNonXmlChars(boolean b) {
		replaceNonXmlChars = b;
	}

	/**
	 * Controls whether the text to encapsulate should be put in a cdata section
	 * @ff.default true
	 */
	public void setUseCdataSection(boolean b) {
		useCdataSection = b;
	}
}