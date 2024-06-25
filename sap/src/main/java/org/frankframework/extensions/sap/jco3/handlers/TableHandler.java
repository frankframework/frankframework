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
package org.frankframework.extensions.sap.jco3.handlers;

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
 */
public class TableHandler extends Handler {

	private JCoTable table = null;
	private boolean parsedItem = false;

	public TableHandler(JCoTable table) {
		super();
		this.table=table;
	}

	@Override
	protected void startElement(String localName) {
		if (!parsedItem) {
			if ("item".equals(localName)) {
				if(log.isTraceEnabled()) log.trace("appending row");
				table.appendRow();
				parsedItem=true;
			} else {
				log.warn("element '{}' invalid", localName);
				unknownElementDepth = 1;
			}
		} else {
			if (table.getMetaData().hasField(localName)) {
				childHandler = getHandler(table, localName);
				if (childHandler == null) {
					startStringField(localName, table);
				}
			} else {
				log.warn("field '{}' does not exist", localName);
				unknownElementDepth = 1;
			}
		}
	}

	@Override
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
