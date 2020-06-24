/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.extensions.sap.jco2;

import java.io.IOException;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sap.mw.jco.JCO;

/**
 * Class to parse table xml and store it into SAP table.
 * XML structure is like:
 * <TABLES>
 *		<INSOBJECTPARTNER> <!-- tabel -->
 *			<item> <!-- rij -->
 *				<BANK_ID_OUT>123</BANK_ID_OUT> <!-- kolom -->
 *			</item>
 *		</INSOBJECTPARTNER>
 * </TABLES>
 * 
 * 
 * @author  Gerrit van Brakel
 * @since   4.11  
 */
public class TableDigester {
	protected Logger log = LogUtil.getLogger(this);

	private class TableHandler extends DefaultHandler {
		
		private JCO.ParameterList tableParams;
		private JCO.Table table=null;
		private StringBuffer columnValue=new StringBuffer();
		
		private boolean parsedTables=false;
		private boolean parsedItem=false;
		private boolean parsedColumn=false;
	
		public TableHandler(JCO.ParameterList tableParams) {
			super();
			this.tableParams=tableParams;
		}
		
		@Override
		public void startDocument() {
			// nothing special here
		}
		
		@Override
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
			if (!parsedTables) {
				if (localName.equals("TABLES")) {
					parsedTables=true;
				}
			} else if (table==null) {
				log.debug("parsing table ["+localName+"]");
				table = tableParams.getTable(localName);
			} else if (!parsedItem) {
				if (localName.equals("item")) {
					log.debug("appending row");
					table.appendRow();
					parsedItem=true;
				}
			} else {
				log.debug("parsing column ["+localName+"]");
				parsedColumn=true;
				columnValue.setLength(0);
			} 
		}
		
		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
			if (parsedColumn) {
				log.debug("setting column ["+localName+"] to value ["+columnValue+"]");
				table.setValue(columnValue.toString(),localName);
				parsedColumn=false;
			} else if (parsedItem){
				parsedItem=false;
			} else if (table!=null) {
				table=null;
			} else if (parsedTables) {
				log.debug("finished parsing tables");
			} else {
				log.warn("unexpected state");
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (parsedColumn) {
				columnValue.append(ch,start,length);
			}
		}
	}



//
//	private class OpenTable extends Rule {
//		public void begin(String namespace, String name, Attributes attributes) throws Exception {
//			log.debug("parsing table ["+name+"]");
//			Digester digester = getDigester();
//			JCO.ParameterList tableParams = (JCO.ParameterList)digester.peek();
//			JCO.Table table = tableParams.getTable(name);
//			digester.push(table);
////			super.begin(namespace,name,attributes);
//		}
//		public void body(String namespace, String name, String text) throws Exception {
//			log.debug("OpenTable.body table ["+name+"], text ["+text+"]");
//		}
//
//		public void end(String namespace, String name) throws Exception {
//			log.debug("OpenTable.end parsing table ["+name+"]");
//			digester.pop();
//		}
//		public void finish() throws Exception {
//			log.debug("OpenTable.finish");
//		}
//
//	}
//
//	private class SetColumn extends Rule {
//		public void begin(String namespace, String name, Attributes attributes) throws Exception {
//			Digester digester = getDigester();
//			digester.push(new StringBuffer());
//		}
//		public void body(String namespace, String name, String text) throws Exception {
//			Digester digester = getDigester();
//			StringBuffer sb = (StringBuffer)digester.peek();
//			sb.append(text);
//		}
//		public void end(String namespace, String name) throws Exception {
//			Digester digester = getDigester();
//			StringBuffer sb = (StringBuffer)digester.pop();
//			JCO.Table table = (JCO.Table)digester.peek();
//			String value = sb.toString();
//			log.debug("setting column ["+name+"] to value ["+value+"]");
//			table.setValue(name,value);
//		}
//	}
//
	
	public void digestTableXml(JCO.ParameterList tableParams, String xml) throws IOException, SAXException  {
		
		ContentHandler ch = new TableHandler(tableParams);
		XmlUtils.parseXml(xml,ch);
		
//		Digester digester = new Digester();
//		digester.setUseContextClassLoader(false);
////		digester.addRule		("TABLES/*",new OpenTable());
////		digester.addCallMethod	("TABLES/*/item","appendRow");
////		digester.addRule		("TABLES/*/item/*",new SetColumn());
//		//digester.addRule		("TABLES",new ParseTables());
//		digester.addRule		("TABLES/RETURN",new OpenTable());
//		digester.addCallMethod	("TABLES/RETURN/item","appendRow");
//		digester.addRule		("TABLES/RETURN/item/MESSAGE",new SetColumn());
//		digester.push(tableParams);
//		log.debug("parsing table XML ["+xml+"]");
//		try {
//			digester.parse(new StringReader(xml));
//		} catch (Throwable t) {
//			log.error("could not parse tablexml",t);
//		}
//		log.debug("finished parsing table XML");
	}
}
