package nl.nn.adapterframework.util;

import org.apache.commons.lang.exception.NestableException;

public class DomBuilderException extends NestableException {
	public static final String version="$Id: DomBuilderException.java,v 1.1 2004-02-04 08:36:09 a1909356#db2admin Exp $";
	
/**
 * DomBuilderException constructor comment.
 */
public DomBuilderException() {
	super();
}
/**
 * DomBuilderException constructor comment.
 * @param msg java.lang.String
 */
public DomBuilderException(String msg) {
	super(msg);
}
/**
 * DomBuilderException constructor comment.
 * @param msg java.lang.String
 * @param nestedException java.lang.Throwable
 */
public DomBuilderException(String msg, Throwable nestedException) {
	super(msg, nestedException);
}
/**
 * DomBuilderException constructor comment.
 * @param nestedException java.lang.Throwable
 */
public DomBuilderException(Throwable nestedException) {
	super(nestedException);
}
}
