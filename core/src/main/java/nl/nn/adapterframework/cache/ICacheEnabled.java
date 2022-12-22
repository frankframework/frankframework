/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

/**
 * Interface to be implemented by classes that could use a cache.
 * Implementers will be notified of a cache that is configured via setCache().
 * They must call cache.configure() once in their own configure() method
 * They must call cache.open() and cache.close() from their own open() resp. close().
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public interface ICacheEnabled<K,V> {

	/** optional {@link EhCache cache} definition */
	void setCache(ICache<K,V> cache);
	ICache<K,V> getCache();
}
