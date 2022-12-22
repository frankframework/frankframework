/*
Copyright 2017 Integration Partners B.V.

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
package nl.nn.adapterframework.http.rest;

import nl.nn.adapterframework.http.RestListener;

/**
 * Etag (key-value) Cache interface, allows {@link RestListener RestListeners} and {@link ApiListener ApiListeners} to save and retrieve etags.
 *
 * @author	Niels Meijer
 * @since	7.0-B2
 *
 */
public interface IApiCache {

	/**
	 * Retrieve an object from the cache
	 *
	 * @param key		name of the object to fetch
	 * @return			null or value of the stored object
	 */
	public Object get(String key);

	/**
	 * Place an object in the cache
	 *
	 * @param key		name of the object to store
	 * @param value		value of the object
	 */
	public void put(String key, Object value);

	/**
	 * Place an object in the cache
	 *
	 * @param key		name of the object to store
	 * @param value		value of the object
	 * @param ttl		time to live, when the object expires
	 */
	public void put(String key, Object value, int ttl);

	/**
	 * Remove an object from the cache
	 *
	 * @param key		name of the object to remove
	 * @return			returns true when successfully removed the object
	 */
	public boolean remove(String key);

	/**
	 * Checks whether or not an object has previously been stored in the cache
	 *
	 * @param key		name of the object to find
	 * @return			true when found
	 */
	public boolean containsKey(String key);

	/**
	 * Removes all items in the cache.
	 */
	public void clear();

	/**
	 * Closes the cache.
	 */
	public void destroy();
}
