/*
 * $Log: CalendarParserException.java,v $
 * Revision 1.1.2.1  2008-06-04 16:27:23  europe\L190409
 * sync from HEAD
 *
 * Revision 1.1  2008/06/03 15:53:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of CalendarParser
 *
 */
package nl.nn.adapterframework.util;

import nl.nn.adapterframework.core.IbisException;

/**
 * Exception for CalendarParser.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class CalendarParserException extends IbisException {

	public CalendarParserException(String msg) {
		super(msg);
	}

	public CalendarParserException(String msg, Throwable t) {
		super(msg,t);
	}
}
