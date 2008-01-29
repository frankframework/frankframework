/*
 * $Log: IThreadCountControllable.java,v $
 * Revision 1.1  2008-01-29 12:11:20  europe\L190409
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
