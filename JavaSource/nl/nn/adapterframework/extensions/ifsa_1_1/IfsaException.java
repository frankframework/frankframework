package nl.nn.adapterframework.extensions.ifsa_1_1;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.exception.NestableException;

/**
 * Base Exception with exhaustive toString()
 *
 * @see nl.nn.adapterframework.core.IbisException
 *
 * @author Gerrit van Brakel
 * @version Id
 */
public class IfsaException extends NestableException {
public IfsaException() {
	super();
}
public IfsaException(String arg1) {
	super(arg1);
}
public IfsaException(String arg1, Throwable arg2) {
	super(arg1, arg2);
}
public IfsaException(Throwable arg1) {
	super(arg1);
}
	public String toString() {
		String result="";
		Throwable t;
		String additionalMethodNames[] = { "getLinkedException" };

		for (t=this; t!=null; t=ExceptionUtils.getCause(t,additionalMethodNames)) {
			result += "\nclass: "+t.getClass().getName()+"\nmessage:"+t.getMessage()+"\nfields:\n"+ToStringBuilder.reflectionToString(t,ToStringStyle.MULTI_LINE_STYLE)+"\n";
		}
	return result;
 }
}
