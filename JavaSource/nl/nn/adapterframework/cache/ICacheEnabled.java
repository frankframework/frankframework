/*
 * $Log: ICacheEnabled.java,v $
 * Revision 1.1  2010-09-13 13:28:19  L190409
 * added cache facility
 *
 */
package nl.nn.adapterframework.cache;


/**
 * Interface to be implemented by classes that could use a cache. 
 * Implementers will be notified of a cache that is configured via registerCache().
 * They must call cache.configure() once in their own configure() method
 * They must call cache.open() and cache.close() from their own open() resp. close().
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
 */
public interface ICacheEnabled {

	void registerCache(ICacheAdapter cache);
	ICacheAdapter getCache();
}
