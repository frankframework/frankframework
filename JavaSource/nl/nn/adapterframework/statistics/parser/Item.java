/*
 * $Log: Item.java,v $
 * Revision 1.1  2009-12-29 14:25:18  L190409
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
