/*
 * $Log: TracingEventNumbers.java,v $
 * Revision 1.1  2006-02-20 15:42:40  europe\L190409
 * moved METT-support to single entry point for tracing
 *
 * Revision 1.1  2006/02/09 07:57:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * METT tracing support
 *
 */
package nl.nn.adapterframework.util;

/**
 * Piefje om METT te kunnen gebruiken in IBIS
 * 
 * @author L190409
 * @since  
 * @version Id
 */
public interface TracingEventNumbers {

	public int getAfterEvent();
	public int getBeforeEvent();
	public int getExceptionEvent();

}
