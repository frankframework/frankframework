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

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;

/**
 * General Cache provider.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Cache, will be set from owner</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxElementsInMemory(String) maxElementsInMemory}</td><td>the maximum number of elements in memory, before they are evicted</td><td>100</td></tr>
 * <tr><td>{@link #setMemoryStoreEvictionPolicy(String) memoryStoreEvictionPolicy}</td><td>either <code>LRU</code>=LeastRecentUse,<code>LFU</code>=LeastFrequentUse or <code>FIFO</code>=FirstInFirstOut</td><td>"LRU"</td></tr>
 * <tr><td>{@link #setEternal(boolean) eternal}</td><td>If <code>true</code>, the elements in the cache are eternal, i.e. never expire</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setTimeToLiveSeconds(int) timeToLiveSeconds}</td><td>the amount of time to live for an element from its creation date</td><td>36000 (=10 hours)</td></tr>
 * <tr><td>{@link #setTimeToIdleSeconds(int) timeToIdleSeconds}</td><td>the amount of time to live for an element from its last accessed or modified date</td><td>36000 (=10 hours)</td></tr>
 * <tr><td>{@link #setOverflowToDisk(boolean) overflowToDisk}</td><td>If <code>true</code>, the elements that are evicted from memory are spooled to disk</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setMaxElementsOnDisk(String) maxElementsOnDisk}</td><td>the maximum number of elements on disk, before they are removed</td><td>10000</td></tr>
 * <tr><td>{@link #setDiskPersistent(boolean) diskPersistent}</td><td>If <code>true</code>, the the cache is reloaded after the JVM restarts</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setDiskExpiryThreadIntervalSeconds(int) diskExpiryThreadIntervalSeconds}</td>  <td>how often to run the disk store expiry thread</td><td>600</td></tr>
 * <tr><td>{@link #setKeyXPath(String) keyXPath}</td><td>xpath expression to extract cache key from request message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyNamespaceDefs(String) keyNamespaceDefs}</td><td>namespace defintions for keyXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyStyleSheet(String) keyStyleSheet}</td><td>stylesheet to extract cache key from request message. Use in combination with {@link #setCacheEmptyKeys(String) cacheEmptyKeys} to inhibit caching for certain groups of request messages</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyInputSessionKey(String) keyInputSessionKey}</td><td>session key to use as input for transformation of request message to key by keyXPath or keyStyleSheet</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCacheEmptyKeys(String) cacheEmptyKeys}</td><td>controls whether empty keys are used for caching. When set true, cache entries with empty keys can exist.</td><td>false</td></tr>
 * <tr><td>{@link #setValueXPath(String) valueXPath}</td><td>xpath expression to extract value to be cached key from response message. Use in combination with {@link #setCacheEmptyValues(String) cacheEmptyValues} to inhibit caching for certain groups of response messages</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setValueNamespaceDefs(String) valueNamespaceDefs}</td><td>namespace defintions for valueXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setValueStyleSheet(String) valueStyleSheet}</td><td>stylesheet to extract value to be cached from response message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setValueInputSessionKey(String) valueInputSessionKey}</td><td>session key to use as input for transformation of response message to cached value by valueXPath or valueStyleSheet</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCacheEmptyValues(String) cacheEmptyValues}</td><td>controls whether empty values will be cached. When set true, empty cache entries can exist for any key.</td><td>false</td></tr>
 * </table>
 * </p>
 * N.B. the default values shown can be overridden using properties in appConstants. The property names are found by prefixing the attribute name with <code>cache.default.</code>.
 * </p>
 * 
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version $Id$
 */
public class EhCache extends CacheAdapterBase {
	
	private final String KEY_PREFIX="cache.default."; 
	private final String KEY_MAX_ELEMENTS_IN_MEMORY=KEY_PREFIX+"maxElementsInMemory"; 
	private final String KEY_MEMORYSTORE_EVICTION_POLICY=KEY_PREFIX+"memoryStoreEvictionPolicy"; 
	private final String KEY_ETERNAL=KEY_PREFIX+"eternal"; 
	private final String KEY_TIME_TO_LIVE_SECONDS=KEY_PREFIX+"timeToLiveSeconds"; 
	private final String KEY_TIME_TO_IDLE_SECONDS=KEY_PREFIX+"timeToIdleSeconds"; 
	private final String KEY_OVERFLOW_TO_DISK=KEY_PREFIX+"overflowToDisk"; 
	private final String KEY_MAX_ELEMENTS_ON_DISK=KEY_PREFIX+"maxElementsOnDisk"; 
	private final String KEY_DISK_PERSISTENT=KEY_PREFIX+"diskPersistent"; 
	private final String KEY_DISK_EXPIRY_THREAD_INTERVAL_SECONDS=KEY_PREFIX+"diskExpiryThreadIntervalSeconds"; 
	
	private int maxElementsInMemory=100;
	private String memoryStoreEvictionPolicy="LRU";
	private boolean eternal=false;
	private int timeToLiveSeconds=36000;
	private int timeToIdleSeconds=36000;
	private boolean overflowToDisk=false;
	private int maxElementsOnDisk=10000;
	private boolean diskPersistent=false;
	private int diskExpiryThreadIntervalSeconds=600;
	
	private Ehcache cache=null;
	private IbisCacheManager cacheManager=null;
	
	public EhCache() {
		super();
		AppConstants ac = AppConstants.getInstance();
		maxElementsInMemory=ac.getInt(KEY_MAX_ELEMENTS_IN_MEMORY, maxElementsInMemory);
		memoryStoreEvictionPolicy=ac.getProperty(KEY_MEMORYSTORE_EVICTION_POLICY, memoryStoreEvictionPolicy);
		eternal=ac.getBoolean(KEY_ETERNAL, eternal);
		timeToLiveSeconds=ac.getInt(KEY_TIME_TO_LIVE_SECONDS, timeToLiveSeconds);
		timeToIdleSeconds=ac.getInt(KEY_TIME_TO_IDLE_SECONDS, timeToIdleSeconds);
		overflowToDisk=ac.getBoolean(KEY_OVERFLOW_TO_DISK, overflowToDisk);
		maxElementsOnDisk=ac.getInt(KEY_MAX_ELEMENTS_ON_DISK, maxElementsOnDisk);
		diskPersistent=ac.getBoolean(KEY_DISK_PERSISTENT, diskPersistent);
		diskExpiryThreadIntervalSeconds=ac.getInt(KEY_DISK_EXPIRY_THREAD_INTERVAL_SECONDS, diskExpiryThreadIntervalSeconds);
	}
	
	public void configure(String ownerName) throws ConfigurationException {
		super.configure(ownerName);
		if (isDiskPersistent() && !isOverflowToDisk()) {
			log.info("setting overflowToDisk true, to support diskPersistent=true");
			setOverflowToDisk(true);
		}
		MemoryStoreEvictionPolicy.fromString(getMemoryStoreEvictionPolicy()); 
	}
	
	public void open() {
		Cache configCache = new Cache(
				getName(),
				getMaxElementsInMemory(),
				MemoryStoreEvictionPolicy.fromString(getMemoryStoreEvictionPolicy()),
				isOverflowToDisk(),
				null,
				isEternal(),
				getTimeToLiveSeconds(),
				getTimeToIdleSeconds(),
				isDiskPersistent(),
				getDiskExpiryThreadIntervalSeconds(),
				null,
				null,
				getMaxElementsOnDisk()
				);
		cacheManager=IbisCacheManager.getInstance();
		cache = cacheManager.addCache(configCache);
	}

	public void close() {
		if (isDiskPersistent()) {
			log.debug("cache ["+getName()+"] flushing to disk");
			cache.flush();
		} else {
			log.debug("cache ["+getName()+"] clearing data");
			cache.removeAll();
		}
		if (cacheManager!=null) {
			cacheManager.removeCache(cache.getName());
			cacheManager=null;
		}
		cache=null;
	}


	protected Serializable getElement(String key) {
		Element element = cache.get(key);
		if (element==null) {
			return null;
		}
		return element.getValue();
	}

	protected void putElement(String key, Serializable value) {
		Element element=new Element(key,value);
		cache.put(element);
	}

	
	public int getMaxElementsInMemory() {
		return maxElementsInMemory;
	}
	public void setMaxElementsInMemory(int maxElementsInMemory) {
		this.maxElementsInMemory = maxElementsInMemory;
	}

	public String getMemoryStoreEvictionPolicy() {
		return memoryStoreEvictionPolicy;
	}
	public void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
		this.memoryStoreEvictionPolicy = memoryStoreEvictionPolicy;
	}

	public boolean isEternal() {
		return eternal;
	}
	public void setEternal(boolean eternal) {
		this.eternal = eternal;
	}

	public int getTimeToLiveSeconds() {
		return timeToLiveSeconds;
	}
	public void setTimeToLiveSeconds(int timeToLiveSeconds) {
		this.timeToLiveSeconds = timeToLiveSeconds;
	}

	public int getTimeToIdleSeconds() {
		return timeToIdleSeconds;
	}
	public void setTimeToIdleSeconds(int timeToIdleSeconds) {
		this.timeToIdleSeconds = timeToIdleSeconds;
	}

	public boolean isOverflowToDisk() {
		return overflowToDisk;
	}
	public void setOverflowToDisk(boolean overflowToDisk) {
		this.overflowToDisk = overflowToDisk;
	}

	public int getMaxElementsOnDisk() {
		return maxElementsOnDisk;
	}
	public void setMaxElementsOnDisk(int maxElementsOnDisk) {
		this.maxElementsOnDisk = maxElementsOnDisk;
	}

	public boolean isDiskPersistent() {
		return diskPersistent;
	}
	public void setDiskPersistent(boolean diskPersistent) {
		this.diskPersistent = diskPersistent;
	}

	public int getDiskExpiryThreadIntervalSeconds() {
		return diskExpiryThreadIntervalSeconds;
	}
	public void setDiskExpiryThreadIntervalSeconds(
			int diskExpiryThreadIntervalSeconds) {
		this.diskExpiryThreadIntervalSeconds = diskExpiryThreadIntervalSeconds;
	}
}
