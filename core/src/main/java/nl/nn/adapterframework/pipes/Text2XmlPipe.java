/*
   Copyright 2013 Nationale-Nederlanden

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
import java.io.StringReader;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.EncapsulatingReader;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for converting text to or from xml. 
 *
 * 
 * @author J. Dekker
 */
public class Text2XmlPipe extends FixedForwardPipe {
	private String xmlTag;
	private boolean includeXmlDeclaration = true;
	private boolean splitLines = false;
	private boolean replaceNonXmlChars = true;
	private boolean useCdataSection = true;
	

	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(getXmlTag())) {
			throw new ConfigurationException("You have not defined xmlTag");
		}
	}
	
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, IPipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		if (isSplitLines() && input != null) {
			try {
				Reader reader = new StringReader(input.toString());
				if (replaceNonXmlChars) {
					reader = new EncapsulatingReader(reader, "", "", true);
				}
				BufferedReader br = new BufferedReader(reader);

				String l;
				StringBuffer result = new StringBuffer();

				while ((l = br.readLine()) != null) {
					result.append("<line>"+addCdataSection(l)+"</line>");
				}
					
				input = result.toString();
				br.close();
			}
			catch (IOException e) {
				throw new PipeRunException(this, "Unexpected exception during splitting", e); 
			}
			
						
		} else if (replaceNonXmlChars && input != null) {
			input = addCdataSection(XmlUtils.encodeCdataString(input.toString()));
		} else {
			input = addCdataSection((input == null ? null : input.toString()));
		}
			
		String resultString = (isIncludeXmlDeclaration()?"<?xml version=\"1.0\" encoding=\"UTF-8\"?>":"") +
		"<" + getXmlTag() + ">"+input+"</" + xmlTag + ">";	
		return new PipeRunResult(getForward(), resultString);
	}

	private String addCdataSection(String input) {
		if (isUseCdataSection()) {
			return "<![CDATA["+ input +"]]>";
		} else {
			return input;
		}
	}
	
	/**
	 * Returns the xmltag to encapsulate the text in.
	 */
	public String getXmlTag() {
		return xmlTag;
	}

	/**
	 * Sets the xmltag
	 */
	@IbisDoc({"the xml tag to encapsulate the text in", ""})
	public void setXmlTag(String xmlTag) {
		this.xmlTag = xmlTag;
	}

	public boolean isIncludeXmlDeclaration() {
		return includeXmlDeclaration;
	}

	@IbisDoc({"controls whether a declation is included above the xml text", "true"})
	public void setIncludeXmlDeclaration(boolean b) {
		includeXmlDeclaration = b;
	}

	public boolean isSplitLines() {
		return splitLines;
	}

	@IbisDoc({"controls whether the lines of the input are places in separated &lt;line&gt; tags", "false"})
	public void setSplitLines(boolean b) {
		splitLines = b;
	}

	@IbisDoc({"replace all non xml chars (not in the <a href=\"http://www.w3.org/tr/2006/rec-xml-20060816/#nt-char\">character range as specified by the xml specification</a>) with the inverted question mark (0x00bf)", "true"})
	public void setReplaceNonXmlChars(boolean b) {
		replaceNonXmlChars = b;
	}

	public boolean isUseCdataSection() {
		return useCdataSection;
	}

	@IbisDoc({"controls whether the text to encapsulate should be put in a cdata section", "true"})
	public void setUseCdataSection(boolean b) {
		useCdataSection = b;
	}
}