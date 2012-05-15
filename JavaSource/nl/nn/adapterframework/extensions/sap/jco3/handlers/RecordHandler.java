/*
 * $Log: RecordHandler.java,v $
 * Revision 1.1  2012-05-15 22:13:44  m00f069
 * Allow nesting of (different) types in SAP XML
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3.handlers;

import com.sap.conn.jco.JCoRecord;

/**
 * Generic handler to be extended by other handlers.
 * 
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 */
public class RecordHandler extends Handler {

	private JCoRecord record;

	public RecordHandler(JCoRecord record) {
		super();
		this.record = record;
	}

	protected void startElement(String localName) {
		if (record.getMetaData().hasField(localName)) {
			childHandler = getHandler(record, localName);
			if (childHandler == null) {
				startStringField(localName, record);
			}
		} else {
			log.warn("field '" + localName + "' does not exist");
			unknownElementDepth = 1;
		}
	}

	protected void endElement(String localName) {
		if (parsedStringField) {
			endStringField(localName, record);
		} else {
			finished(localName);
		}
	}

}
