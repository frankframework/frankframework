/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.batch;

import java.util.Map;

import lombok.Getter;

import org.frankframework.core.PipeLineSession;

/**
 * Manager that decides the handlers based on the content of a field in the specified
 * position in a record. The fields in the record are of a fixed length.
 * The data beween the start position and end position is taken as key in the flow-table.
 *
 *
 * @author John Dekker
 * @deprecated Warning: non-maintained functionality.
 */
public class FixedPositionRecordHandlerManager extends RecordHandlerManager {

	private @Getter int startPosition;
	private @Getter int endPosition=-1;

	@Override
	public RecordHandlingFlow getRecordHandler(PipeLineSession session, String record) throws Exception {
		String value = null;
		if (startPosition >= record.length()) {
			throw new Exception("Record size is smaller then the specified position of the recordtype within the record");
		}
		if (endPosition < 0) {
			Map<String,RecordHandlingFlow> valueHandlersMap = getFlowMap();
			RecordHandlingFlow rhf = null;
			for(String name: valueHandlersMap.keySet()) {
				if (log.isTraceEnabled())
					log.trace("determining value for record [{}] with key [{}] and startPosition [{}]", record, name, startPosition);
				if (name.length()<=record.length()) {
					value = record.substring(startPosition, name.length());
					if (value.equals(name) && (rhf = valueHandlersMap.get(name))!=null) {
						break;
					}
				}
			}
			if (rhf == null) {
				rhf =getFlowMap().get("*");
				if (rhf == null) {
					throw new Exception("No handlers (flow) found for recordKey [" + value + "]");
				}
			}
			return rhf;
		}
		if (endPosition >= record.length()) {
			value = record.substring(startPosition);
		} else {
			value = record.substring(startPosition, endPosition);
		}

		return super.getRecordHandlerByKey(value);
	}


	/**
	 * Start position of the field in the record that identifies the recordtype (first character is 0)
	 * @ff.default 0
	 */
	public void setStartPosition(int i) {
		startPosition = i;
	}

	/**
	 * If endposition &gt;= 0 then this field contains the endPosition of the recordtype field in the record; All characters beyond this position are ignored. Else, if endPosition &lt; 0 then it depends on the length of the recordkey in the flow
	 * @ff.default -1
	 */
	public void setEndPosition(int i) {
		endPosition = i;
	}
}
