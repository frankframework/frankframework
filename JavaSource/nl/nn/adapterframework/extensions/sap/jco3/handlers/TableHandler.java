/*
 * $Log: TableHandler.java,v $
 * Revision 1.2  2012-05-23 16:10:20  m00f069
 * Fixed javadoc
 *
 * Revision 1.1  2012/05/15 22:13:44  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Allow nesting of (different) types in SAP XML
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3.handlers;

import com.sap.conn.jco.JCoTable;

/**
 * Handler for table xml elements like:
 * <pre>
 * &lt;INSOBJECTPARTNER&gt; (table)
 *   &lt;item&gt; (row)
 *     &lt;BANK_ID_OUT&gt;123&lt;/BANK_ID_OUT&gt; (column)
 *     &lt;ZZSEQUENCE&gt;1&lt;/ZZSEQUENCE&gt;
 *     STRUCTURE|TABLE
 *   &lt;/item&gt;
 * &lt;/INSOBJECTPARTNER&gt;
 * </pre>
 * 
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 */
public class TableHandler extends Handler {

	private JCoTable table = null;
	private boolean parsedItem = false;

	public TableHandler(JCoTable table) {
		super();
		this.table=table;
	}

	protected void startElement(String localName) {
		if (!parsedItem) {
			if (localName.equals("item")) {
				log.debug("appending row");
				table.appendRow();
				parsedItem=true;
			} else {
				log.warn("element '" + localName + "' invalid");
				unknownElementDepth = 1;
			}
		} else {
			if (table.getMetaData().hasField(localName)) {
				childHandler = getHandler(table, localName);
				if (childHandler == null) {
					startStringField(localName, table);
				}
			} else {
				log.warn("field '" + localName + "' does not exist");
				unknownElementDepth = 1;
			}
		}
	}

	protected void endElement(String localName) {
		if (parsedStringField) {
			endStringField(localName, table);
		} else if (parsedItem){
			parsedItem=false;
		} else {
			finished(localName);
		}
	}
}
