/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2022 WeAreFrank!

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
package org.frankframework.statistics;

/**
 * List of statistics items that can be iterated over to show all values.
 *
 * @author  Gerrit van Brakel
 * @since   4.9.9
 */
public interface ItemList {

	String ITEM_FORMAT_TIME= "######.###";
	String ITEM_FORMAT_PERC="##0.0";

	String PRINT_FORMAT_COUNT="#,##0";
	String PRINT_FORMAT_TIME="#,##0";
	String PRINT_FORMAT_PERC="##0.0";

	public enum Type {
		INTEGER,
		TIME,
		FRACTION
	}

	final String ITEM_NAME_COUNT="count";
	final String ITEM_NAME_MIN="min";
	final String ITEM_NAME_MAX="max";
	final String ITEM_NAME_AVERAGE="avg";
	final String ITEM_NAME_STDDEV="stdDev";
	final String ITEM_NAME_SUM="sum";
	final String ITEM_NAME_SUMSQ="sumsq";

	final String ITEM_VALUE_NAN="-";

	int getItemCount();
	String getItemName(int index);
	Type getItemType(int index);
	Object getItemValue(int index);

}
