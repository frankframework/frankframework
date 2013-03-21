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
package nl.nn.adapterframework.cache;

import java.io.Serializable;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * Interface to be implemented by cache-providers. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version $Id$
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
