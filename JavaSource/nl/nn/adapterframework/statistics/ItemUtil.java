/*
 * $Log: ItemUtil.java,v $
 * Revision 1.1  2009-12-29 14:25:18  L190409
 * moved statistics to separate package
 *
 */
package nl.nn.adapterframework.statistics;

import java.text.DecimalFormat;

import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Utility functions to process statistics items.
 * @author  Gerrit van Brakel
 * @since   4.9.9
 * @version Id
 */
public class ItemUtil {

	public static void addItem(XmlBuilder xml, String name, String value) {
		XmlBuilder item = new XmlBuilder("item");
	
		item.addAttribute("name", name);
		item.addAttribute("value", value);
		xml.addSubElement(item);
	}
	public static void addItem(XmlBuilder xml, String name, Long value) {
		addItem(xml,name,""+value);
	}
	
	public static XmlBuilder getSummaryContainer(XmlBuilder parent, String name) {
		if (parent==null) {
			throw new NullPointerException("parent XmlBuilder cannot be null");
		}
		if (name!=null) {
			parent.addAttribute("name", name);
		}
		XmlBuilder stats = new XmlBuilder("summary");
		parent.addSubElement(stats);
		return stats;
	}

	public static XmlBuilder toXml(ItemList il, String elementName, String name, DecimalFormat timeFormat, DecimalFormat percentageFormat) {
		XmlBuilder container = new XmlBuilder(elementName);
		XmlBuilder stats = getSummaryContainer(container, name);
	
		for (int i=0; i<il.getItemCount(); i++) {
			Object item = il.getItemValue(i);
			if (item==null) {
				addItem(stats, il.getItemName(i), ItemList.ITEM_VALUE_NAN);
			} else {
				switch (il.getItemType(i)) {
					case ItemList.ITEM_TYPE_INTEGER: 
						addItem(stats, il.getItemName(i), ""+ (Long)item);
						break;
					case ItemList.ITEM_TYPE_TIME: 
						addItem(stats, il.getItemName(i), timeFormat.format(item));
						break;
					case ItemList.ITEM_TYPE_FRACTION:
						addItem(stats, il.getItemName(i), percentageFormat.format(((Double)item).doubleValue()*100)+ "%");
						break;
				}
			}
		}
		return container;
	}

	public static String getItemValueFormated(ItemList il, int index) {
		Object item = il.getItemValue(index);
		if (item==null) {
			return ItemList.ITEM_VALUE_NAN;
		} else {
			switch (il.getItemType(index)) {
				case StatisticsKeeper.ITEM_TYPE_INTEGER: 
					return ""+ (Long)item;
				case StatisticsKeeper.ITEM_TYPE_TIME: 
					DecimalFormat df=new DecimalFormat(ItemList.ITEM_FORMAT_TIME);
					return df.format(item);
				case StatisticsKeeper.ITEM_TYPE_FRACTION:
					DecimalFormat pf=new DecimalFormat(ItemList.ITEM_FORMAT_PERC);
					return ""+pf.format(((Double)item).doubleValue()*100);
				default:
					return item.toString();
			}
		}
	}
    

}
