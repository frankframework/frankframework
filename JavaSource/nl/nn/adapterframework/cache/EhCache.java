/*
 * $Log: EhCache.java,v $
 * Revision 1.1  2010-09-13 13:28:19  L190409
 * added cache facility
 *
 */
package nl.nn.adapterframework.cache;

import java.io.Serializable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * General Cache provider.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Cache, will be set from owner</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyXPath(String) keyXPath}</td><td>xpath expression to extract cache key from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyNamespaceDefs(String) keyNamespaceDefs}</td><td>namespace defintions for keyXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyStyleSheet(String) keyStyleSheet}</td><td>stylesheet to extract cache key from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxElementsInMemory(String) maxElementsInMemory}</td><td>the maximum number of elements in memory, before they are evicted</td><td>10000</td></tr>
 * <tr><td>{@link #setMemoryStoreEvictionPolicy(String) memoryStoreEvictionPolicy}</td><td>either <code>LRU</code>,<code>LFU</code> or <code>FIFO</code></td><td>"LRU"</td></tr>
 * <tr><td>{@link #setEternal(boolean) eternal}</td><td>If <code>true</code>, the elements in the cache are eternal, i.e. never expire</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setTimeToLiveSeconds(int) timeToLiveSeconds}</td><td>the amount of time to live for an element from its creation date</td><td>3600</td></tr>
 * <tr><td>{@link #setTimeToIdleSeconds(int) timeToIdleSeconds}</td><td>the amount of time to live for an element from its last accessed or modified date</td><td>3600</td></tr>
 * <tr><td>{@link #setOverflowToDisk(boolean) overflowToDisk}</td><td>If <code>true</code>, the elements that are evicted from memory are spooled to disk</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setMaxElementsOnDisk(String) maxElementsOnDisk}</td><td>the maximum number of elements on disk, before they are removed</td><td>1000000</td></tr>
 * <tr><td>{@link #setDiskPersistent(boolean) diskPersistent}</td><td>If <code>true</code>, the the cache is reloaded after the JVM restarts</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setDiskExpiryThreadIntervalSeconds(int) diskExpiryThreadIntervalSeconds}</td>  <td>how often to run the disk store expiry thread</td><td>600</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
 */
public class EhCache extends CacheAdapterBase {
	
	private int maxElementsInMemory=10000;
	private String memoryStoreEvictionPolicy="LRU";
	private boolean eternal=false;
	private int timeToLiveSeconds=3600;
	private int timeToIdleSeconds=3600;
	private boolean overflowToDisk=false;
	private int maxElementsOnDisk=1000000;
	private boolean diskPersistent=false;
	private int diskExpiryThreadIntervalSeconds=600;
	
	private Ehcache cache=null;
	private IbisCacheManager cacheManager=null;
	
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
