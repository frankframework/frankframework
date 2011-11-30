/*
 * $Log: ICacheEnabled.java,v $
 * Revision 1.3  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/09/13 13:28:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
