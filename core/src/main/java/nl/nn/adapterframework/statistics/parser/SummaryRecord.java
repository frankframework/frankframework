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
package nl.nn.adapterframework.statistics.parser;

import java.text.DecimalFormat;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.statistics.Basics;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Record use to gather statistics data from a file.
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class SummaryRecord extends Basics {
	protected Logger log = LogUtil.getLogger(this);

	private String name;

	public void registerItem(Item item) {
		String name=item.getName();
		String value=item.getValue();
		if (!value.equals(ITEM_VALUE_NAN)) {
			if (ITEM_NAME_COUNT.equals(name)) {
				count=Long.parseLong(value);
			} else
			if (ITEM_NAME_MIN.equals(name)) {
				min=Long.parseLong(value);
			} else
			if (ITEM_NAME_MAX.equals(name)) {
				max=Long.parseLong(value);
			} else
			if (ITEM_NAME_SUM.equals(name)) {
				sum=Long.parseLong(value);
			} else
			if (ITEM_NAME_SUMSQ.equals(name)) {
				sumOfSquares=Long.parseLong(value);
			}
		}
	}

	public XmlBuilder toXml(String name, DecimalFormat timeFormat, DecimalFormat percentageFormat) {
		return super.toXml("stat", name, timeFormat, percentageFormat);
	}

	public XmlBuilder toXml(DecimalFormat timeFormat, DecimalFormat percentageFormat) {
		return super.toXml("stat", getName(), timeFormat, percentageFormat);
	}

	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}
}
