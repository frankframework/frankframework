/*
 * $Log: RootHandler.java,v $
 * Revision 1.1  2012-05-15 22:13:44  m00f069
 * Allow nesting of (different) types in SAP XML
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3.handlers;

import java.util.Iterator;
import java.util.List;

import com.sap.conn.jco.JCoParameterList;

/**
 * Handler for xml root element containing INPUT, OUTPUT and TABLES (parameter
 * lists).
 * 
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 */
public class RootHandler extends Handler {

	private List<JCoParameterList> parameterLists;
	private boolean parsedRequestRoot = false;

	public RootHandler(List<JCoParameterList> parameterLists) {
		super();
		this.parameterLists = parameterLists;
	}

	protected void startElement(String localName) {
		if (!parsedRequestRoot) {
			parsedRequestRoot = true;
		} else  {
			Iterator<JCoParameterList> iterator = parameterLists.iterator();
			while (iterator.hasNext()) {
				JCoParameterList jcoParameterList = (JCoParameterList)iterator.next();
				if (jcoParameterList.getMetaData().getName().equals(localName)) {
					childHandler = new ParameterListHandler(jcoParameterList);
				}
			}
			if (childHandler == null) {
				log.warn("parameter list '"+localName+"' does not exist");
				unknownElementDepth = 1;
			}
		}
	}

	protected void endElement(String localName) {
		finished(localName);
	}

}
