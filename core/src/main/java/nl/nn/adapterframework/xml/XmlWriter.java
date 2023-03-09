/*
   Copyright 2019-2022 WeAreFrank!

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
package nl.nn.adapterframework.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.io.output.XmlStreamWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlEncodingUtils;

public class XmlWriter extends DefaultHandler implements LexicalHandler {
	protected Logger log = LogUtil.getLogger(this);

	public static final String DISABLE_OUTPUT_ESCAPING="javax.xml.transform.disable-output-escaping";
	public static final String ENABLE_OUTPUT_ESCAPING="javax.xml.transform.enable-output-escaping";

	private @Getter Writer writer;
	private @Setter boolean includeXmlDeclaration=false;
	private @Setter boolean newlineAfterXmlDeclaration=false;
	private @Setter boolean includeComments=true;
	private @Setter boolean textMode=false;
	private @Setter boolean closeWriterOnEndDocument=false;

	private boolean outputEscaping=true;
	private int elementLevel=0;
	private boolean elementJustStarted;
	private boolean inCdata;
	private List<PrefixMapping> newNamespaceDefinitions=new ArrayList<>();
	private Map<String,Stack<String>> activeNamespaceDefinitions=new HashMap<>();

	private class PrefixMapping {

		public String prefix;
		public String uri;

		PrefixMapping(String prefix, String uri) {
			this.prefix=prefix;
			this.uri=uri;
		}
	}

	public XmlWriter() {
		this(new StringWriter());
	}

	public XmlWriter(Writer writer) {
		this(writer, false);
	}

	public XmlWriter(Writer writer, boolean closeWriterOnEndDocument) {
		this.writer=writer;
		this.closeWriterOnEndDocument = closeWriterOnEndDocument;
	}

	public XmlWriter(OutputStream stream) {
		this(stream, false);
	}

	public XmlWriter(OutputStream stream, boolean closeStreamOnEndDocument) {
		this(new XmlStreamWriter(stream), closeStreamOnEndDocument);
	}

	@Override
	public void startDocument() throws SAXException {
		try {
			if (includeXmlDeclaration) {
				writer.append("<?xml version=\"1.0\" encoding=\""+StreamUtil.DEFAULT_INPUT_STREAM_ENCODING+"\"?>");
				if (newlineAfterXmlDeclaration) {
					writer.append("\n");
				}
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}


	@Override
	public void endDocument() throws SAXException {
		try {
			if (closeWriterOnEndDocument) {
				writer.close();
			} else {
				writer.flush();
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	private void writePrefixMapping(PrefixMapping prefixMapping) throws IOException {
		if (elementLevel==0 && StringUtils.isEmpty(prefixMapping.uri)) {
			return;
		}
		writer.append(" xmlns");
		if (StringUtils.isNotEmpty(prefixMapping.prefix) ) {
			writer.append(":").append(prefixMapping.prefix);
		}
		writer.append("=\"").append(XmlEncodingUtils.encodeChars(prefixMapping.uri)).append("\"");
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		PrefixMapping mapping = new PrefixMapping(prefix,uri);
		Stack<String> prefixMappingStack = activeNamespaceDefinitions.get(prefix);
		if (prefixMappingStack==null) {
			prefixMappingStack = new Stack<>();
			activeNamespaceDefinitions.put(prefix, prefixMappingStack);
		}
		if (prefixMappingStack.isEmpty() || !prefixMappingStack.peek().equals(uri)) {
			newNamespaceDefinitions.add(mapping);
		}
		prefixMappingStack.push(uri);
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		activeNamespaceDefinitions.get(prefix).pop();
	}


	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		try {
			if (elementJustStarted && !textMode) {
				writer.append(">");
			}
			if (!textMode) {
				writer.append("<"+qName);
				for (int i=0; i<attributes.getLength(); i++) {
					String attrValue = attributes.getValue(i);
					if (attrValue!=null) {
						writer.append(" "+attributes.getQName(i)+"=\""+ XmlEncodingUtils.encodeChars(attrValue, true).replace("&#39;", "'")+"\"");
					}
				}
				for (int i=0; i<newNamespaceDefinitions.size(); i++) {
					writePrefixMapping(newNamespaceDefinitions.get(i));
				}
			}
			newNamespaceDefinitions.clear();
			elementJustStarted=true;
			elementLevel++;
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			elementLevel--;
			if (!textMode) {
				if (elementJustStarted) {
					elementJustStarted=false;
					writer.append("/>");
				} else {
					writer.append("</"+qName+">");
				}
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		try {
			if (elementJustStarted) {
				elementJustStarted=false;
				if (!textMode) {
					writer.append(">");
				}
			}
			if (textMode) {
				writer.write(ch, start, length);
			} else {
				if (inCdata || !outputEscaping) {
					writer.append(new String(ch, start, length));
				} else {
					writer.append(XmlEncodingUtils.encodeCharsAndReplaceNonValidXmlCharacters(new String(ch, start, length)).replace("&quot;", "\"").replace("&#39;", "'"));
				}
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		try {
			if (elementJustStarted && !textMode) {
				writer.append(">");
				elementJustStarted=false;
			}
			if (includeComments) {
				writer.append("<!--").append(new String(ch, start, length)).append("-->");
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		try {
			if (target.equals(DISABLE_OUTPUT_ESCAPING)) {
				outputEscaping=false;
				return;
			}
			if (target.equals(ENABLE_OUTPUT_ESCAPING)) {
				outputEscaping=true;
				return;
			}
			if (!textMode) {
				writer.append("<?").append(target).append(" ").append(data).append("?>\n");
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void startDTD(String arg0, String arg1, String arg2) throws SAXException {
//		System.out.println("startDTD");
	}

	@Override
	public void endDTD() throws SAXException {
//		System.out.println("endDTD");
	}

	@Override
	public void startCDATA() throws SAXException {
		try {
			if (elementJustStarted) {
				elementJustStarted=false;
				if (!textMode) {
					writer.append(">");
				}
			}
			if (!textMode) {
				writer.append("<![CDATA[");
			}
			inCdata=true;
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void endCDATA() throws SAXException {
		try {
			if (!textMode) {
				writer.append("]]>");
			}
			inCdata=false;
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void startEntity(String arg0) throws SAXException {
//		System.out.println("startEntity ["+arg0+"]");
	}
	@Override
	public void endEntity(String arg0) throws SAXException {
//		System.out.println("endEntity ["+arg0+"]");
	}

	@Override
	public String toString() {
		return writer.toString();
	}

}
