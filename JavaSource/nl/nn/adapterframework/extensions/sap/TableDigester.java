/*
 * $Log: TableDigester.java,v $
 * Revision 1.1  2010-05-06 12:49:27  L190409
 * alternative way to set tables from XML
 *
 */
package nl.nn.adapterframework.extensions.sap;

import java.io.IOException;
import java.io.StringReader;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.sap.mw.jco.JCO;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class TableDigester {
	protected Logger log = LogUtil.getLogger(this);

	
	private class OpenTable extends Rule {
		public void begin(String namespace, String name, Attributes attributes) throws Exception {
			log.debug("parsing table ["+name+"]");
			Digester digester = getDigester();
			JCO.ParameterList tableParams = (JCO.ParameterList)digester.peek();
			JCO.Table table = tableParams.getTable(name);
			digester.push(table);
		}

		public void end(String namespace, String name) throws Exception {
			digester.pop();
		}
	}
	
	private class PushElementName extends Rule {
		public void begin(String namespace, String name, Attributes attributes) throws Exception {
			log.debug("pushing element name ["+name+"]");
			Digester digester = getDigester();
			digester.push(name);
		}
		public void end(String namespace, String name) throws Exception {
			digester.pop();
		}
	}

	private class SetColumn extends Rule {
		public void begin(String namespace, String name, Attributes attributes) throws Exception {
			Digester digester = getDigester();
			digester.push(new StringBuffer());
		}
		public void body(String namespace, String name, String text) throws Exception {
			Digester digester = getDigester();
			StringBuffer sb = (StringBuffer)digester.peek();
			sb.append(text);
		}
		public void end(String namespace, String name) throws Exception {
			Digester digester = getDigester();
			StringBuffer sb = (StringBuffer)digester.pop();
			JCO.Table table = (JCO.Table)digester.peek();
			String value = sb.toString();
			log.debug("setting column ["+name+"] to value ["+value+"]");
			table.setValue(name,value);
		}
	}

	
	public void digestTableXml(JCO.ParameterList tableParams, String xml) throws IOException, SAXException {
		Digester digester = new Digester();
		digester.addRule		("*/TABLES/*",new OpenTable());
		digester.addCallMethod	("*/TABLES/*/item","appendRow");
		digester.addRule		("*/TABLES/*/item/*",new SetColumn());
		digester.push(tableParams);
		digester.parse(new StringReader(xml));
	}
}
