package nl.nn.adapterframework.http;

/**
 * Etag (key-value) Cache interface, allows {@link nl.nn.adapterframework.http.RestListener RestListeners} to save and retrieve etags.
 * 
 * @author	Niels Meijer
 * @since	7.0-B2
 *
 */
public interface IRestEtagCache {

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
	 * Flushes all cache items from memory to the disk store, and from the DiskStore to disk
	 */
	public void flush();


	/**
	 * Removes all items in the cache.
	 */
	public void removeAll();
}
