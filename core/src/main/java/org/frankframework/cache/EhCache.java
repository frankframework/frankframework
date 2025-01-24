/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.cache;

import java.io.IOException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;

/**
 * General Cache provider.
 *
 * N.B. the default values shown can be overridden using properties in appConstants. The property names are found by prefixing the attribute name with <code>cache.default.</code>.
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
@FrankDocGroup(FrankDocGroupValue.OTHER)
public class EhCache<V> extends AbstractCacheAdapter<V> {

	private static final String KEY_PREFIX = "cache.default.";
	private static final String KEY_MAX_ELEMENTS_IN_MEMORY = KEY_PREFIX + "maxElementsInMemory";
	private static final String KEY_MEMORYSTORE_EVICTION_POLICY = KEY_PREFIX + "memoryStoreEvictionPolicy";
	private static final String KEY_ETERNAL = KEY_PREFIX + "eternal";
	private static final String KEY_TIME_TO_LIVE_SECONDS = KEY_PREFIX + "timeToLiveSeconds";
	private static final String KEY_TIME_TO_IDLE_SECONDS = KEY_PREFIX + "timeToIdleSeconds";
	private static final String KEY_OVERFLOW_TO_DISK = KEY_PREFIX + "overflowToDisk";
	private static final String KEY_MAX_ELEMENTS_ON_DISK = KEY_PREFIX + "maxElementsOnDisk";
	private static final String KEY_DISK_PERSISTENT = KEY_PREFIX + "diskPersistent";
	private static final String KEY_DISK_EXPIRY_THREAD_INTERVAL_SECONDS = KEY_PREFIX + "diskExpiryThreadIntervalSeconds";

	private int maxElementsInMemory=100;
	private String memoryStoreEvictionPolicy="LRU";
	private boolean eternal=false;
	private int timeToLiveSeconds=36000;
	private int timeToIdleSeconds=36000;
	private boolean overflowToDisk=false;
	private int maxElementsOnDisk=10000;
	private boolean diskPersistent=false;
	private int diskExpiryThreadIntervalSeconds=600;

	private Ehcache cache = null;
	private IbisCacheManager cacheManager = null;

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

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (isDiskPersistent() && !isOverflowToDisk()) {
			log.info("setting overflowToDisk true, to support diskPersistent=true");
			setOverflowToDisk(true);
		}
		MemoryStoreEvictionPolicy.fromString(getMemoryStoreEvictionPolicy());
	}

	@Override
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

	@Override
	public void close() {
		if (isDiskPersistent()) {
			log.debug("cache [{}] flushing to disk", getName());
			cache.flush();
		} else {
			log.debug("cache [{}] clearing data", getName());
			cache.removeAll();
		}
		if (cacheManager!=null) {
			cacheManager.destroyCache(cache.getName());
			cacheManager=null;
		}
		cache=null;
	}

	@Override
	protected V getElement(String key) {
		Element element = cache.get(key);
		if (element==null) {
			return null;
		}
		return (V)element.getValue();
	}

	@Override
	protected void putElement(String key, V value) {
		Element element=new Element(key,value);
		cache.put(element);
	}

	@Override
	protected boolean removeElement(Object key) {
		return cache.remove(key);
	}

	@Override
	protected V toValue(Message value) {
		try {
			return (V)value.asString();
		} catch (IOException e) {
			log.warn("Could not perform toValue() by asString()", e);
			return null;
		}
	}

	/**
	 * The maximum number of elements in memory, before they are evicted
	 * @ff.default 100
	 */
	public void setMaxElementsInMemory(int maxElementsInMemory) {
		this.maxElementsInMemory = maxElementsInMemory;
	}
	public int getMaxElementsInMemory() {
		return maxElementsInMemory;
	}

	/**
	 * Either <code>LRU</code>=Least Recent Use,<code>LFU</code>=Least Frequent Use or <code>FIFO</code>=First In - First Out
	 * @ff.default LRU
	 */
	public void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
		this.memoryStoreEvictionPolicy = memoryStoreEvictionPolicy;
	}
	public String getMemoryStoreEvictionPolicy() {
		return memoryStoreEvictionPolicy;
	}

	/**
	 * If <code>true</code>, the elements in the cache are eternal, i.e. never expire
	 * @ff.default false
	 */
	public void setEternal(boolean eternal) {
		this.eternal = eternal;
	}
	public boolean isEternal() {
		return eternal;
	}

	/**
	 * The amount of time <i>in seconds</i> to live for an element from its creation date
	 * @ff.default 36000
	 */
	public void setTimeToLiveSeconds(int timeToLiveSeconds) {
		this.timeToLiveSeconds = timeToLiveSeconds;
	}
	public int getTimeToLiveSeconds() {
		return timeToLiveSeconds;
	}

	/**
	 * The amount of time <i>in seconds</i> to live for an element from its last accessed or modified date
	 * @ff.default 36000
	 */
	public void setTimeToIdleSeconds(int timeToIdleSeconds) {
		this.timeToIdleSeconds = timeToIdleSeconds;
	}
	public int getTimeToIdleSeconds() {
		return timeToIdleSeconds;
	}

	/**
	 * If <code>true</code>, the elements that are evicted from memory are spooled to disk
	 * @ff.default false
	 */
	public void setOverflowToDisk(boolean overflowToDisk) {
		this.overflowToDisk = overflowToDisk;
	}
	public boolean isOverflowToDisk() {
		return overflowToDisk;
	}

	/**
	 * The maximum number of elements on disk, before they are removed
	 * @ff.default 10000
	 */
	public void setMaxElementsOnDisk(int maxElementsOnDisk) {
		this.maxElementsOnDisk = maxElementsOnDisk;
	}
	public int getMaxElementsOnDisk() {
		return maxElementsOnDisk;
	}

	/**
	 * If <code>true</code>, the cache is stored on disk and survives configuration reloads & JVM restarts.
	 * @ff.default false
	 */
	public void setDiskPersistent(boolean diskPersistent) {
		this.diskPersistent = diskPersistent;
	}
	public boolean isDiskPersistent() {
		return diskPersistent;
	}

	/**
	 * How often to run the disk store expiry thread
	 * @ff.default 600
	 */
	public void setDiskExpiryThreadIntervalSeconds(int diskExpiryThreadIntervalSeconds) {
		this.diskExpiryThreadIntervalSeconds = diskExpiryThreadIntervalSeconds;
	}
	public int getDiskExpiryThreadIntervalSeconds() {
		return diskExpiryThreadIntervalSeconds;
	}

}
