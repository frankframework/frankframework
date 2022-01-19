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
import java.io.Writer;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe for converting text to or from xml. 
 *
 * 
 * @author J. Dekker
 */
public class Text2XmlPipe extends StreamingPipe {
	private @Getter String xmlTag;
	private @Getter boolean includeXmlDeclaration = true;
	private @Getter boolean splitLines = false;
	private @Getter boolean replaceNonXmlChars = true;
	private @Getter boolean useCdataSection = true;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(getXmlTag())) {
			throw new ConfigurationException("Attribute [xmlTag] must be specified");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result;
		try (MessageOutputStream target = getTargetStream(session)) {
			try (Writer writer = target.asWriter()) {
				if (isSplitLines() && !Message.isEmpty(message)) {
					try (BufferedReader reader = new BufferedReader(message.asReader())) {
						String line;
						while ((line = reader.readLine()) != null) {
							writer.append("<line>"+addCdataSection(line)+"</line>");
						}

						result = writer.toString();
					}
				} else if (isReplaceNonXmlChars() && !Message.isEmpty(message)) {
					result = addCdataSection(XmlUtils.encodeCdataString(message.asString()));
				} else {
					result = addCdataSection(message.asString());
				}
			}
		} catch(Exception e) {
			throw new PipeRunException(this, "Unexpected exception during splitting", e); 
		}

		return new PipeRunResult(getSuccessForward(), prepareResult(result));
	}

	private String prepareResult(String result) {
		StringBuilder builder = new StringBuilder(isIncludeXmlDeclaration() ? "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" : "");
		builder.append("<" + getXmlTag());
		if(result == null) {
			builder.append(" nil=\"true\" />");
		} else {
			builder.append(">"+result+"</" + getXmlTag() + ">");
		}
		return builder.toString();
	}

	private String addCdataSection(String input) {
		if (isUseCdataSection()) {
			return "<![CDATA["+ input +"]]>";
		} else {
			return input;
		}
	}

	@Override
	protected boolean canProvideOutputStream() {
		return false;
	}

	/**
	 * The xml tag to encapsulate the text in
	 * @ff.mandatory
	 */
	public void setXmlTag(String xmlTag) {
		this.xmlTag = xmlTag;
	}

	/**
	 * Controls whether a declaration is included above the xml text
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