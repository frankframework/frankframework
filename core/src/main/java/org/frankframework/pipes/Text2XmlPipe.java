/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2022-2024 WeAreFrank!

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.doc.EnterpriseIntegrationPattern;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import lombok.Getter;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.EncapsulatingReader;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

/**
 * Pipe for converting TEXT to XML.
 *
 * @author J. Dekker
 */
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class Text2XmlPipe extends FixedForwardPipe {
	private @Getter String xmlTag;
	private @Getter boolean splitLines = false;
	private @Getter boolean replaceNonXmlChars = true;
	private @Getter boolean useCdataSection = true;

	protected static final String SPLITTED_LINE_TAG = "line";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(getXmlTag())) {
			throw new ConfigurationException("Attribute [xmlTag] must be specified");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (Message.isNull(message)) {
			return new PipeRunResult(getSuccessForward(), new Message("<" + getXmlTag() + " nil=\"true\" />"));
		} else if (message.isEmpty() && isUseCdataSection()) {
			return new PipeRunResult(getSuccessForward(), new Message("<" + getXmlTag() + "><![CDATA[]]></" + getXmlTag() + ">"));
		}

		try {
			MessageBuilder messageBuilder = new MessageBuilder();
			ContentHandler handler = messageBuilder.asXmlWriter();
			if (!isSplitLines()) {
				processNonSplittingLines(message, handler);
			} else {
				processSplittingLines(message, handler);
			}
			return new PipeRunResult(getSuccessForward(), messageBuilder.build());
		} catch (Exception e) {
			throw new PipeRunException(this, "Unexpected exception during splitting", e);
		}
	}

	private void processSplittingLines(final Message message, final ContentHandler handler) throws SAXException, IOException {
		try {
			handler.startDocument();
			handler.startElement("", getXmlTag(), getXmlTag(), new AttributesImpl());
			try (BufferedReader reader = new BufferedReader(message.asReader())) {
				String line;
				boolean lineWritten = false;
				while ((line = reader.readLine()) != null) {
					if (lineWritten) {
						handler.characters("\n".toCharArray(), 0, "\n".length());
					}
					if (isSplitLines()) {
						handler.startElement("", SPLITTED_LINE_TAG, SPLITTED_LINE_TAG, new AttributesImpl());
					}
					if (isUseCdataSection()) {
						((LexicalHandler) handler).startCDATA();
					}
					char[] characters = line.toCharArray();
					if (isReplaceNonXmlChars()) {
						XmlEncodingUtils.replaceNonPrintableCharacters(characters, 0, characters.length);
					}
					handler.characters(characters, 0, characters.length);
					lineWritten = true;
					if (isUseCdataSection()) {
						((LexicalHandler) handler).endCDATA();
					}
					if (isSplitLines()) {
						handler.endElement("", SPLITTED_LINE_TAG, SPLITTED_LINE_TAG);
					}
				}
			}
			handler.endElement("", getXmlTag(), getXmlTag());
		} finally {
			handler.endDocument();
		}
	}

	private void processNonSplittingLines(final Message message, final ContentHandler handler) throws IOException, SAXException {
		String prefix = "<" + getXmlTag() + ">";
		String suffix = "</" + getXmlTag() + ">";
		if (isUseCdataSection()) {
			prefix += "<![CDATA[";
			suffix = "]]>" + suffix;
		}
		Reader encapsulatingReader = isReplaceNonXmlChars() ? new EncapsulatingReader(message.asReader(), prefix, suffix) {
			@Override
			public int read(char[] cbuf, int off, int len) throws IOException {
				int lenRead = super.read(cbuf, off, len);
				return XmlEncodingUtils.replaceNonPrintableCharacters(cbuf, off, lenRead);
			}

		} : new EncapsulatingReader(message.asReader(), prefix, suffix);
		try (Message encapsulatedMessage = new Message(encapsulatingReader)) {
			XmlUtils.parseXml(encapsulatedMessage.asInputSource(), handler);
		}
	}

	/**
	 * The xml tag to encapsulate the text in
	 *
	 * @ff.mandatory
	 */
	public void setXmlTag(String xmlTag) {
		this.xmlTag = xmlTag;
	}

	/**
	 * Controls whether the lines of the input are places in separated &lt;line&gt; tags
	 *
	 * @ff.default false
	 */
	public void setSplitLines(boolean b) {
		splitLines = b;
	}

	/**
	 * Replace all non xml chars (not in the <a href="http://www.w3.org/tr/2006/rec-xml-20060816/#nt-char">character range as specified by the xml specification</a>)
	 * with the inverted question mark (0x00bf)
	 *
	 * @ff.default true
	 */
	public void setReplaceNonXmlChars(boolean b) {
		replaceNonXmlChars = b;
	}

	/**
	 * Controls whether the text to encapsulate should be put in a cdata section
	 *
	 * @ff.default true
	 */
	public void setUseCdataSection(boolean b) {
		useCdataSection = b;
	}
}
