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

import com.sap.conn.jco.JCoRecord;

/**
 * Generic handler to be extended by other handlers.
 *
 * @author  Jaco de Groot
 * @since   5.0
 */
public class RecordHandler extends Handler {

	private final JCoRecord record;

	public RecordHandler(JCoRecord record) {
		super();
		this.record = record;
	}

	@Override
	protected void startElement(String localName) {
		if (record.getMetaData().hasField(localName)) {
			childHandler = getHandler(record, localName);
			if (childHandler == null) {
				startStringField(localName, record);
			}
		} else {
			log.warn("field '{}' does not exist", localName);
			unknownElementDepth = 1;
		}
	}

	@Override
	protected void endElement(String localName) {
		if (parsedStringField) {
			endStringField(localName, record);
		} else {
			finished(localName);
		}
	}

}
