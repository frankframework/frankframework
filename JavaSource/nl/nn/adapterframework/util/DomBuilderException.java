package nl.nn.adapterframework.util;

import org.apache.commons.lang.exception.NestableException;

public class DomBuilderException extends NestableException {
	
/**
 * DomBuilderException constructor comment.
 * <p>$Id: DomBuilderException.java,v 1.4 2011-11-30 13:51:49 europe\m168309 Exp $</p>
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
