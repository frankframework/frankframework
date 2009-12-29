/*
 * $Log: ItemList.java,v $
 * Revision 1.1  2009-12-29 14:25:18  L190409
 * moved statistics to separate package
 *
 */
package nl.nn.adapterframework.statistics;

import nl.nn.adapterframework.util.DateUtils;

/**
 * List of statistics items that can be iterated over to show all values.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.9
 * @version Id
 */
public interface ItemList {

	final String ITEM_FORMAT_TIME=DateUtils.FORMAT_MILLISECONDS;
	final String ITEM_FORMAT_PERC="##0.0";

	final int ITEM_TYPE_INTEGER=1;
	final int ITEM_TYPE_TIME=2;
	final int ITEM_TYPE_FRACTION=3;

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
	int getItemType(int index);
	Object getItemValue(int index);

}
