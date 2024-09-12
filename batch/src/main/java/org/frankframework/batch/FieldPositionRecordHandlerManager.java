/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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

import lombok.Getter;

import org.frankframework.core.PipeLineSession;

/**
 * Manager that decides the handlers based on the content of a field in the specified
 * position in a record. The fields in the record are separated by a separator.
 * The value of the specified field is taken as key in the flow-table.
 *
 * @author John Dekker
 * @deprecated Warning: non-maintained functionality.
 */
public class FieldPositionRecordHandlerManager extends RecordHandlerManager {

	private @Getter int fieldNr;
	private @Getter String separator;

	@Override
	public RecordHandlingFlow getRecordHandler(PipeLineSession session, String record) throws Exception {
		int startNdx = -1, endNdx = -1;
		int curField = 0;
		while (curField++ != fieldNr) {
			if (startNdx != -1 && endNdx == -1) {
				throw new Exception("Record contains less fields then the specified fieldnr indicating its type");
			}
			startNdx = endNdx + 1;
			endNdx = record.indexOf(separator, startNdx);
		}
		if (endNdx == -1) {
			return getRecordHandlerByKey(record.substring(startNdx));
		}
		return getRecordHandlerByKey(record.substring(startNdx, endNdx));
	}



	/** position of field that identifies the recordtype (position of first field is 1) */
	public void setFieldNr(int i) {
		fieldNr = i;
	}

	/** separator that separates the fields in the record */
	public void setSeparator(String string) {
		separator = string;
	}

}
