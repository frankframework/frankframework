/*
 * $Log: TracingUtil.java,v $
 * Revision 1.1  2006-02-20 15:42:40  europe\L190409
 * moved METT-support to single entry point for tracing
 *
 */
package nl.nn.adapterframework.util;

import org.apache.log4j.Logger;

import com.ing.coins.mett.application.MonitorAccessor;


/**
 * Single point of entry for METT tracing utility
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.5
 * @version Id
 */
public class TracingUtil {
	private static Logger log = Logger.getLogger(TracingUtil.class);
	
	public static void beforeEvent(Object o) {
		if (o instanceof TracingEventNumbers) {
			eventOccurred(((TracingEventNumbers)o).getBeforeEvent());
		}
	}

	public static void afterEvent(Object o) {
		if (o instanceof TracingEventNumbers) {
			eventOccurred(((TracingEventNumbers)o).getAfterEvent());
		}
	}

	public static void exceptionEvent(Object o) {
		if (o instanceof TracingEventNumbers) {
			eventOccurred(((TracingEventNumbers)o).getExceptionEvent());
		}
	}

	
	protected static void eventOccurred(int eventNr) {
		if (eventNr>=0) {
			try {
				MonitorAccessor.eventOccurred(eventNr);
			} catch (Throwable t) {
				log.warn("Exception occured posting METT event",t);
			}
		}
	}

}
