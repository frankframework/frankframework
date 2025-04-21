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
package org.frankframework.extensions.rekenbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

/**
 *  This is the reader of the Calcbox output. This class
 *   uses the XMLReader interface, so it reads the
 *   Calcbox output in the same way it reads XML
 *
 *  Change History
 *  Author                  Date         Version    Details
 *  Colin Wilmans           18-04-2002   1.0        First version
 *  Tim N. van der Leeuw    30-07-2002   1.1        Better handling of
 *                                                  labels with sequence
 *                                                  numbers.
 *                                                  Split-method taken out
 *                                                  and put in Util-object.
 *  Boris O Maltha			23-01-2003	 1.2		Added logging
 *
*/
public class CalcboxOutputReader implements XMLReader {
	ContentHandler handler;

	// We're not doing namespaces.
	String nsu = ""; // NamespaceURI
	String rootElement = "CALCBOXMESSAGE";
	String indent = "\n    "; // for readability!

	//static int to memorize details of last tags
	String[] tagMemory = {};

	/** Parse the input (CalcBox Message Format) */
	@Override
	public void parse(InputSource input) throws IOException, SAXException {
		try {
			// If we have no handler we can stop
			if(handler == null) {
				throw new SAXException("No content handler");
			}

			// Note:
			// We're ignoring setDocumentLocator(), as well
			Attributes atts = new AttributesImpl();
			handler.startDocument();
			handler.startElement(nsu, rootElement, rootElement, atts);

			// Get an efficient reader for the file
			java.io.Reader r = input.getCharacterStream();
			BufferedReader br = new RekenboxLineReader(r);

			// Read the file and output it's contents.
			String line = "";
			while(null != (line = br.readLine())) {

				// get everything before :
				int colon = line.indexOf(":");
				if(colon == -1) {
					continue;
				}
				String calcboxtag = line.substring(0, colon).trim();
				output(calcboxtag, calcboxtag, line);

			}

			// XXX
			handler.ignorableWhitespace("\n".toCharArray(), 0, 1);

			// Place last end tags...
			for(int i = tagMemory.length; i > 0; i--) {
				String strippedTag = striptrailingnumbers(tagMemory[i - 1]);
				handler.endElement(nsu, strippedTag, strippedTag);
			}
			handler.endElement(nsu, rootElement, rootElement);
			handler.endDocument();

		} catch (Exception e) {
			// throw new SAXException(e);
		}
	}

	/** Create output of the input */
	void output(String name, String prefix, String line) throws SAXException {
		String value;

		// Tags with '#SAMENGESTELD' are not interesting for us,
		// because next tag gives this information (redundant protocol error:)
		if(line.indexOf("#SAMENGESTELD") != -1)
			return;

		// place tag in arraylist
		String[] arrayTagString = split(name, ".");

		//Figure out wich elements changed...
		int tagChangeLevel = tagMemory.length + 1;
		for(int i = tagMemory.length; i > 0; i--) {
			// If new tag has more tag parts or a tag part has changed
			if(i > arrayTagString.length || !tagMemory[i - 1].equals(arrayTagString[i - 1])) {
				tagChangeLevel = i;
			}
		}

		//And place end elements for these tags
		for(int i = tagMemory.length; i >= tagChangeLevel; i--) {
			String strippedTag = striptrailingnumbers(tagMemory[i - 1]);
			handler.endElement(nsu, strippedTag, strippedTag);
		}

		//Determine where the ':' is, and take this as the startposition
		int startIndex = line.indexOf(":") + 1; // 1=length of ":" after the name
		handler.ignorableWhitespace(indent.toCharArray(), 0, indent.length());

		// Place start elements
		for(int i = tagChangeLevel; i <= arrayTagString.length; i++) {
			String label = arrayTagString[i - 1];
			int splitPos = trailingNumberSplitPos(label);
			String tag = label.substring(0, splitPos);
			String number = label.substring(splitPos);

			AttributesImpl atts = new AttributesImpl();

			if(!number.isEmpty()) {
				atts.addAttribute(nsu, "volgnummer", "volgnummer", "", number);
			}
			handler.startElement(nsu, tag, tag, atts);
		}

		// don't forget the tagarray for the next time
		tagMemory = arrayTagString;

		// Place the element tag
		value = line.substring(startIndex).trim();
//		BUGFIX: WE MOGEN VERWACHTEN GEEN ; TERUG TE KRIJGEN
//		handler.characters(value.toCharArray(), 0, value.length()-1);
		handler.characters(value.toCharArray(), 0, value.length());

	}

	/** Allow an application to register a content event handler. */
	@Override
	public void setContentHandler(ContentHandler handler) {
		this.handler = handler;
	}

	/** Return the current content handler. */
	@Override
	public ContentHandler getContentHandler() {
		return this.handler;
	}

	// =============================================
	// IMPLEMENT THESE FOR A ROBUST APP
	// =============================================
	/** Allow an application to register an error event handler. */
	@Override
	public void setErrorHandler(ErrorHandler handler) {
	}

	/** Return the current error handler. */
	@Override
	public ErrorHandler getErrorHandler() {
		return null;
	}

	// =============================================
	// IGNORE THESE
	// =============================================
	/** Parse an XML document from a system identifier (URI). */
	@Override
	public void parse(String systemId) throws IOException, SAXException {
	}

	/** Return the current DTD handler. */
	@Override
	public DTDHandler getDTDHandler() {
		return null;
	}

	/** Return the current entity resolver. */
	@Override
	public EntityResolver getEntityResolver() {
		return null;
	}

	/** Allow an application to register an entity resolver. */
	@Override
	public void setEntityResolver(EntityResolver resolver) {
	}

	/** Allow an application to register a DTD event handler. */
	@Override
	public void setDTDHandler(DTDHandler handler) {
	}

	/** Look up the value of a property. */
	@Override
	public Object getProperty(String name) {
		return null;
	}

	/** Set the value of a property. */
	@Override
	public void setProperty(String name, Object value) {
	}

	/** Set the state of a feature. */
	@Override
	public void setFeature(String name, boolean value) {
	}

	/** Look up the value of a feature. */
	@Override
	public boolean getFeature(String name) {
		return false;
	}

	/** strip function to strip trailing numbers */
	private static String striptrailingnumbers(String str) {
		// XXXXX Strip numbers

		boolean containstrailingnumbers = false;
		int j = 0;
		for(j = str.length(); j > 0; j--) {
			if(Character.isDigit(str.charAt(j - 1))) {
				containstrailingnumbers = true;
			} else
			{
				break;
			}
		}
		if(containstrailingnumbers)
		{
			return str.substring(0,j);
		}
		else
		{
			return str;
		}
	}

	private static int trailingNumberSplitPos(String str) {
		for(int j = str.length(); j > 0; --j) {
			if(!Character.isDigit(str.charAt(j - 1))) {
				return j;
			}
		}
		return 0;
	}

	private String[] split(String name, String separators) {
		StringTokenizer st = new StringTokenizer(name, separators);
		List list = new ArrayList();
		while(st.hasMoreTokens()) {
			list.add(st.nextToken());
		}
		String[] array = new String[list.size()];
		for(int i = 0; i < array.length; i++) {
			array[i] = (String) list.get(i);
		}
		return array;
	}
}
