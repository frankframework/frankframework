/*
 * $Log: ICacheAdapter.java,v $
 * Revision 1.1  2010-09-13 13:28:19  L190409
 * added cache facility
 *
 */
package nl.nn.adapterframework.cache;

import java.io.Serializable;

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
	 * Transform the input of the current element to a key in the cache-map.
	 * Allows for instance XPath translations.
	 */
	String transformKey(String input);
	

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
