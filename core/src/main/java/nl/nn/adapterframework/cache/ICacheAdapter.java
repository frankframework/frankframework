/*
   Copyright 2013,2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.stream.Message;

/**
 * Interface to be implemented by cache-providers. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public interface ICacheAdapter<K,V> {

	void configure(String ownerName) throws ConfigurationException; 
	void open();
	void close();

	/**
	 * Transform the the current request message to a key in the cache-map.
	 * Allows for instance XPath translations.
	 */
	K transformKey(String input, IPipeLineSession session);
	
	/**
	 * Transform the the current response message to a value in the cache-map.
	 * Allows for instance XPath translations.
	 */
	V transformValue(Message input, IPipeLineSession session);
	

	/**
	 * Obtain a potentially cached value, set by put().
	 */
	V get(K key);
	/**
	 * store a value in the cache, that can be retrieved later using get().
	 */
	void put(K key, V value);
	
}
