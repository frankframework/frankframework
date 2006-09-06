/*
 * $Log: TracingUtil.java,v $
 * Revision 1.2  2006-09-06 16:03:03  europe\L190409
 * added startTracing() and stopTracing()
 *
 * Revision 1.1  2006/02/20 15:42:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved METT-support to single entry point for tracing
 *
 */
package nl.nn.adapterframework.util;

import org.apache.log4j.Logger;

import com.ing.coins.mett.application.MonitorAccessor;
import com.ing.coins.mett.application.exceptions.MonitorStartFailedException;


/**
 * Single point of entry for METT tracing utility
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.5
 * @version Id
 */
public class TracingUtil {
	private static Logger log = Logger.getLogger(TracingUtil.class);
	
	public static void startTracing(String serverConfigFile) throws TracingException {
		try {
			MonitorAccessor.start(serverConfigFile);
		} catch (MonitorStartFailedException e) {
			throw new TracingException("Could not start tracing from config file ["+serverConfigFile+"]", e);
		}
	}

	public static void stopTracing() {
		MonitorAccessor.stop();
	}
	
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
