/*
 * $Log: MettHook.java,v $
 * Revision 1.1  2006-02-09 07:57:22  europe\L190409
 * METT tracing support
 *
 */
package nl.nn.adapterframework.core;

/**
 * Piefje om METT te kunnen gebruiken in IBIS
 * 
 * @author L190409
 * @since  
 * @version Id
 */
public interface MettHook {

	public int getAfterEvent();
	public int getBeforeEvent();
	public int getExceptionEvent();

}
