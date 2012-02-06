/*
 * $Log: TableDigester.java,v $
 * Revision 1.1  2012-02-06 14:33:05  m00f069
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 * Revision 1.5  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2010/09/10 11:30:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added xml structure to javadoc
 *
 * Revision 1.2  2010/05/06 17:39:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * alternative way, as digester seemed to be not working
 *
 * Revision 1.1  2010/05/06 12:49:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * alternative way to set tables from XML
 *
 */
package nl.nn.adapterframework.extensions.sap.jco2;

import java.io.IOException;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.log4j.Logger;
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
 * @version Id
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
		
		public void startDocument() {
			
		}
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
		XmlUtils.parseXml(ch,xml);
		
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
