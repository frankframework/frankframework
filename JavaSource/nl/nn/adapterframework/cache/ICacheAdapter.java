/*
 * $Log: ICacheAdapter.java,v $
 * Revision 1.2  2011-05-31 15:29:44  L190409
 * added transformValue()
 *
 * Revision 1.1  2010/09/13 13:28:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added cache facility
 *
 */
package nl.nn.adapterframework.cache;

import java.io.Serializable;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * Interface to be implemented by cache-providers. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
 */
public interface ICacheAdapter {

	void configure(String ownerName) throws ConfigurationException; 
	void open();
	void close();

	/**
	 * Transform the the current request message to a key in the cache-map.
	 * Allows for instance XPath translations.
	 */
	String transformKey(String input, Map sessionContext);
	
	/**
	 * Transform the the current response message to a value in the cache-map.
	 * Allows for instance XPath translations.
	 */
	String transformValue(String input, Map sessionContext);
	

	/**
	 * Obtain a potentially cached value, set by putString().
	 */
	String getString(String key);
	/**
	 * store a value in the cache, that can be retrieved later using getString().
	 */
	void putString(String key, String value);

	/**
	 * Obtain a potentially cached value, set by put().
	 */
	Serializable get(String key);
	/**
	 * store a value in the cache, that can be retrieved later using get().
	 */
	void put(String key, Serializable value);
	
}
