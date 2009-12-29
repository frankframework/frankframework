/*
 * $Log: SummaryRecord.java,v $
 * Revision 1.1  2009-12-29 14:25:18  L190409
 * moved statistics to separate package
 *
 */
package nl.nn.adapterframework.statistics.parser;

import java.text.DecimalFormat;

import nl.nn.adapterframework.statistics.Basics;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.log4j.Logger;

/**
 * Record use to gather statistics data from a file.
 * @author  Gerrit van Brakel
 * @since   4.9.9
 * @version Id
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
				total=Long.parseLong(value);
			} else
			if (ITEM_NAME_SUMSQ.equals(name)) {
				totalSquare=Long.parseLong(value);
			} 
		}
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
