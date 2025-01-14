/*
   Copyright 2013 Nationale-Nederlanden, 2024-2025 WeAreFrank!

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

import java.util.List;

import org.frankframework.core.FrankElement;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.NameAware;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;

/**
 * Interface for transforming a record (= structured ASCII line).
 *
 * @author John Dekker
 */
@FrankDocGroup(FrankDocGroupValue.BATCH)
public interface IRecordHandler extends IConfigurable, FrankElement, NameAware {

	public void open() throws SenderException;
	public void close() throws SenderException;

	/**
	 * Parse the line into an array of fields.
	 *
	 * @return List with String values for each inputfield
	 */
	List<String> parse(PipeLineSession session, String record) throws Exception;

	/**
	 * Perform an action on the array of fields.
	 *
	 * @return transformed result
	 */
	String handleRecord(PipeLineSession session, List<String> parsedRecord) throws Exception;

	boolean isNewRecordType(PipeLineSession session, boolean equalRecordTypes, List<String> prevRecord, List<String> curRecord) throws Exception;

	public String getRecordType(List<String> record);

}
