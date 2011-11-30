/*
 * $Log: ItemList.java,v $
 * Revision 1.3  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/12/29 14:25:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
