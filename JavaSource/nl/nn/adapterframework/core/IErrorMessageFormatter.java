package nl.nn.adapterframework.core;

/**
 * An <code>errorMessageFormatter</code> is responsible for returning a string
 * describing the error at hand in a format that the receiver expects. 
 * By implementing this interface, it is possible to customize messages.
 * @author Johan Verrips
 */
public interface IErrorMessageFormatter {
		public static final String version="$Id: IErrorMessageFormatter.java,v 1.1 2004-02-04 08:36:10 a1909356#db2admin Exp $";

public String format(
	String errorMessage,
    Throwable t,
    INamedObject location,
    String originalMessage,
    String messageId,
    long receivedTime);
}
