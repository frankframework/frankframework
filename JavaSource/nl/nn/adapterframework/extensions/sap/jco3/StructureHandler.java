package nl.nn.adapterframework.extensions.sap.jco3;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sap.conn.jco.JCoStructure;

public class StructureHandler extends DefaultHandler {
	private Logger log = LogUtil.getLogger(this);

	private JCoStructure structure=null;
	private StringBuffer value=new StringBuffer();

	private boolean parsedStructure=false;
	private boolean parsedField=false;

	public StructureHandler(JCoStructure structure) {
		super();
		this.structure=structure;
	}

	public void startDocument() {
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if (!parsedStructure) {
			log.debug("parsing structure ["+localName+"]");
			parsedStructure=true;
		} else {
			log.debug("parsing field ["+localName+"]");
			parsedField=true;
			value.setLength(0);
		}
	}

	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (parsedField) {
			log.debug("setting field ["+localName+"] to value ["+value+"]");
			if (structure.getMetaData().hasField(localName)) {
				structure.setValue(localName,value.toString());
			} else {
				log.warn("Unknown element ["+localName+"] with value ["+value+"]");
			}
			parsedField=false;
		} else if (parsedStructure) {
			log.debug("finished parsing structure");
		} else {
			log.warn("unexpected state");
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		if (parsedField) {
			value.append(ch,start,length);
		}
	}
}
