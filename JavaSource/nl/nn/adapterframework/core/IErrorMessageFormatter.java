/*
 * $Log: IErrorMessageFormatter.java,v $
 * Revision 1.4  2004-03-30 07:29:54  L190409
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:45  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * An <code>errorMessageFormatter</code> is responsible for returning a string
 * describing the error at hand in a format that the receiver expects. 
 * By implementing this interface, it is possible to customize messages.
 * 
 * @version Id
 * @author Johan Verrips
 */
public interface IErrorMessageFormatter {
		public static final String version="$Id: IErrorMessageFormatter.java,v 1.4 2004-03-30 07:29:54 L190409 Exp $";

public String format(
	String errorMessage,
    Throwable t,
    INamedObject location,
    String originalMessage,
    String messageId,
    long receivedTime);
}
