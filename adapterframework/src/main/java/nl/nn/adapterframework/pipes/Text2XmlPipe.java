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
import nl.nn.adapterframework.util.EncapsulatingReader;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for converting text to or from xml. 
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setXmlTag(String) xmlTag}</td><td>the xml tag to encapsulate the text in</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIncludeXmlDeclaration(boolean) includeXmlDeclaration}</td><td>controls whether a declation is included above the Xml text</td><td>true</td></tr>
 * <tr><td>{@link #setSplitLines(boolean) splitLines}</td><td>controls whether the lines of the input are places in separated &lt;line&gt; tags</td><td>false</td></tr>
 * <tr><td>{@link #setReplaceNonXmlChars(boolean) replaceNonXmlChars}</td><td>Replace all non XML chars (not in the <a href="http://www.w3.org/TR/2006/REC-xml-20060816/#NT-Char">character range as specified by the XML specification</a>) with the inverted question mark (0x00BF)</td><td>true</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * @author J. Dekker
 * @version $Id$
 */
public class Text2XmlPipe extends FixedForwardPipe {
	private String xmlTag;
	private boolean includeXmlDeclaration = true;
	private boolean splitLines = false;
	private boolean replaceNonXmlChars = true;
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#configure()
	 */
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(getXmlTag())) {
			throw new ConfigurationException("You have not defined xmlTag");
		}
	}
	
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
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
					result.append("<line><![CDATA["+l+"]]></line>");
				}
					
				input = result.toString();
				br.close();
			}
			catch (IOException e) {
				throw new PipeRunException(this, "Unexpected exception during splitting", e); 
			}
			
						
		} else if (replaceNonXmlChars && input != null) {
			input = "<![CDATA["+ XmlUtils.encodeCdataString(input.toString()) +"]]>";
		} else {
			input = "<![CDATA["+ input +"]]>";
		}
			
		String resultString = (isIncludeXmlDeclaration()?"<?xml version=\"1.0\" encoding=\"UTF-8\"?>":"") +
		"<" + getXmlTag() + ">"+input+"</" + xmlTag + ">";	
		return new PipeRunResult(getForward(), resultString);
	}

	/**
	 * @return the xml tag to encapsulate the text in
	 */
	public String getXmlTag() {
		return xmlTag;
	}

	/**
	 * @param xmlTag
	 */
	public void setXmlTag(String xmlTag) {
		this.xmlTag = xmlTag;
	}

	public boolean isIncludeXmlDeclaration() {
		return includeXmlDeclaration;
	}

	public void setIncludeXmlDeclaration(boolean b) {
		includeXmlDeclaration = b;
	}

	public boolean isSplitLines() {
		return splitLines;
	}

	public void setSplitLines(boolean b) {
		splitLines = b;
	}

	public void setReplaceNonXmlChars(boolean b) {
		replaceNonXmlChars = b;
	}

}

