/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.util.AppConstants;


/** 
 * @author  Gerrit van Brakel
 * @since   4.11
 */
@IbisDescription(
	"General Cache provider." + 
	"N.B. the default values shown can be overridden using properties in appConstants. The property names are found by prefixing the attribute name with <code>cache.default.</code>." + 
	"</p>" 
)
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

	protected Object getElementObject(Object key) {
		Element element = cache.get(key);
		if (element==null) {
			return null;
		}
		return element.getObjectValue();
	}

	protected void putElementObject(Object key, Object value) {
		Element element = new Element(key,value);
		cache.put(element);
	}

	protected boolean removeElement(Object key) {
		return cache.remove(key);
	}

	public int getMaxElementsInMemory() {
		return maxElementsInMemory;
	}

	@IbisDoc({"the maximum number of elements in memory, before they are evicted", "100"})
	public void setMaxElementsInMemory(int maxElementsInMemory) {
		this.maxElementsInMemory = maxElementsInMemory;
	}

	public String getMemoryStoreEvictionPolicy() {
		return memoryStoreEvictionPolicy;
	}

	@IbisDoc({"either <code>lru</code>=leastrecentuse,<code>lfu</code>=leastfrequentuse or <code>fifo</code>=firstinfirstout", "lru"})
	public void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
		this.memoryStoreEvictionPolicy = memoryStoreEvictionPolicy;
	}

	public boolean isEternal() {
		return eternal;
	}

	@IbisDoc({"if <code>true</code>, the elements in the cache are eternal, i.e. never expire", "<code>false</code>"})
	public void setEternal(boolean eternal) {
		this.eternal = eternal;
	}

	public int getTimeToLiveSeconds() {
		return timeToLiveSeconds;
	}

	@IbisDoc({"the amount of time to live for an element from its creation date", "36000 (=10 hours)"})
	public void setTimeToLiveSeconds(int timeToLiveSeconds) {
		this.timeToLiveSeconds = timeToLiveSeconds;
	}

	public int getTimeToIdleSeconds() {
		return timeToIdleSeconds;
	}

	@IbisDoc({"the amount of time to live for an element from its last accessed or modified date", "36000 (=10 hours)"})
	public void setTimeToIdleSeconds(int timeToIdleSeconds) {
		this.timeToIdleSeconds = timeToIdleSeconds;
	}

	public boolean isOverflowToDisk() {
		return overflowToDisk;
	}

	@IbisDoc({"if <code>true</code>, the elements that are evicted from memory are spooled to disk", "<code>false</code>"})
	public void setOverflowToDisk(boolean overflowToDisk) {
		this.overflowToDisk = overflowToDisk;
	}

	public int getMaxElementsOnDisk() {
		return maxElementsOnDisk;
	}

	@IbisDoc({"the maximum number of elements on disk, before they are removed", "10000"})
	public void setMaxElementsOnDisk(int maxElementsOnDisk) {
		this.maxElementsOnDisk = maxElementsOnDisk;
	}

	public boolean isDiskPersistent() {
		return diskPersistent;
	}

	@IbisDoc({"if <code>true</code>, the the cache is reloaded after the jvm restarts", "<code>false</code>"})
	public void setDiskPersistent(boolean diskPersistent) {
		this.diskPersistent = diskPersistent;
	}

	public int getDiskExpiryThreadIntervalSeconds() {
		return diskExpiryThreadIntervalSeconds;
	}

	@IbisDoc({"how often to run the disk store expiry thread", "600"})
	public void setDiskExpiryThreadIntervalSeconds(
			int diskExpiryThreadIntervalSeconds) {
		this.diskExpiryThreadIntervalSeconds = diskExpiryThreadIntervalSeconds;
	}
}
