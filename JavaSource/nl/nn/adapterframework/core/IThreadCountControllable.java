/*
 * $Log: IThreadCountControllable.java,v $
 * Revision 1.3  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2008/01/29 12:11:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.core;

/**
 * Interface to be implemented by classes of which the number of threads can be controlled at runtime.
 * 
 * Implementing this class results in receivers that have a number of threads that can be controlled
 * from the ibisconsole.
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public interface IThreadCountControllable {

	public boolean isThreadCountReadable();
	public boolean isThreadCountControllable();
	
	public int getCurrentThreadCount();
	public int getMaxThreadCount();
	
	public void increaseThreadCount();
	public void decreaseThreadCount();

}
