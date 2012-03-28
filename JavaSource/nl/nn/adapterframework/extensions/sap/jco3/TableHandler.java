package nl.nn.adapterframework.extensions.sap.jco3;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoTable;

public class TableHandler extends DefaultHandler {
	private Logger log = LogUtil.getLogger(this);

	private JCoParameterList tableParams;
	private JCoTable table=null;
	private StringBuffer columnValue=new StringBuffer();
	
	private boolean parsedTables=false;
	private boolean parsedTable=false;
	private boolean parsedItem=false;
	private boolean parsedColumn=false;

	public TableHandler(JCoTable table) {
		super();
		this.table=table;
	}
	
	public void startDocument() {
	}
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
//			if (!parsedTables) {
//				if (localName.equals("TABLES")) {
//					parsedTables=true;
//				}
//			} else if (table==null) {
		if (!parsedTable) {
			log.debug("parsing table ["+localName+"]");//parsing table [IT_QUESTIONS]
//				table = tableParams.getTable(localName);
			parsedTable=true;
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
			log.debug("setting column ["+localName+"] to value ["+columnValue+"]");//setting column [LABEL] to value [CALCULATIONDATE]
			if (table.getMetaData().hasField(localName)) {
				table.setValue(localName,columnValue.toString());
			} else {
				log.warn("Unknown element ["+localName+"] with value ["+columnValue+"]");
			}
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
