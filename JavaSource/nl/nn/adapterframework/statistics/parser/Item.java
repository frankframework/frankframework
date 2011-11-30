/*
 * $Log: Item.java,v $
 * Revision 1.3  2011-11-30 13:52:02  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/12/29 14:25:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved statistics to separate package
 *
 */
package nl.nn.adapterframework.statistics.parser;

/**
 * Helperclass used to parse Statistics-files.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.9
 * @version Id
 */
public class Item {
	
	private String name;
	private String value;


	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}

	public void setValue(String string) {
		value = string;
	}
	public String getValue() {
		return value;
	}

}
