package nl.nn.adapterframework.core;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.exception.NestableException;

/**
 * Base Exception with exhaustive toString() and compact getMessage()
 * @author Gerrit van Brakel
 * 
 * <p>$Id: IbisException.java,v 1.3 2004-03-23 15:25:00 NNVZNL01#L180564 Exp $</p>
 *
 */
public class IbisException extends NestableException {
		public static final String version="$Id: IbisException.java,v 1.3 2004-03-23 15:25:00 NNVZNL01#L180564 Exp $";

public IbisException() {
	super();
}
public IbisException(String arg1) {
	super(arg1);
}
public IbisException(String arg1, Throwable arg2) {
	super(arg1, arg2);
}
public IbisException(Throwable arg1) {
	super(arg1);
}
    public String getMessage() {
	    String messages[]=getMessages(); 
	    String last_message=null;
		String result=null;
	    
	    for(int i=0; i<messages.length; i++) {
		    if (messages[i]!=null && messages[i]!=last_message) {
			    last_message = messages[i];
			    if (result==null) {
				    result=last_message;
			    } else {
				    result=result+": "+last_message;
			    }
		    }
	    }
	    if (result==null) {
		    result=toString();
	    }
	   
	    return result;
    }
    
    
/**
 * Retrieves the messages of all exceptions
 */
public String toString() {
		String result="";
		Throwable t;
		String additionalMethodNames[] = { "getLinkedException" };
 
		for (t=this; t!=null; t=ExceptionUtils.getCause(t,additionalMethodNames)) {
//			result += "\nclass: "+t.getClass().getName()+"\nmessage:"+t.getMessage()+"\nfields:\n"+ToStringBuilder.reflectionToString(t,ToStringStyle.MULTI_LINE_STYLE)+"\n";
			result += "\nclass: "+t.getClass().getName()+"\nmessage:"+t.getMessage()+"\n";
		}
		
	return result;
 }
}
