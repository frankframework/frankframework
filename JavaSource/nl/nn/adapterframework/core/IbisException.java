/*
 * $Log: IbisException.java,v $
 * Revision 1.14  2005-04-26 15:14:51  L190409
 * improved rendering of errormessage
 *
 * Revision 1.13  2005/02/10 08:14:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added exception type in message
 *
 * Revision 1.12  2004/07/20 13:56:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved message parsing
 *
 * Revision 1.11  2004/07/20 13:50:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved message parsing
 *
 * Revision 1.10  2004/07/08 08:55:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes in toString()
 *
 * Revision 1.9  2004/07/07 14:30:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * toString(): classes, messages and root cause
 *
 * Revision 1.8  2004/07/07 13:55:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved toString() method, including fields of causes
 *
 * Revision 1.7  2004/07/06 06:57:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved workings of getMessage()
 *
 * Revision 1.6  2004/03/30 07:29:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.5  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.4  2004/03/23 16:48:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified getMessage() to avoid endless-loop
 *
 */
package nl.nn.adapterframework.core;

import javax.mail.internet.AddressException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.exception.NestableException;

/**
 * Base Exception with exhaustive toString() and compact getMessage()
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public class IbisException extends NestableException {
		public static final String version="$Id: IbisException.java,v 1.14 2005-04-26 15:14:51 L190409 Exp $";

	static {
		// add methodname to find cause of JMS-Exceptions
		ExceptionUtils.addCauseMethodName("getLinkedException");
	}
	
	public IbisException() {
		super();
	}
	public IbisException(String message) {
		super(message);
	}
	public IbisException(String message, Throwable cause) {
		super(message, cause);
	}
	public IbisException(Throwable cause) {
		super(cause);
	}
	
	public String getExceptionType(Throwable t) {
		String name = t.getClass().getName();
		int dotpos=name.lastIndexOf(".");
		name=name.substring(dotpos+1);
		if ( name.endsWith("Exception") ) {
			name=name.substring(0,name.length()-9);
		}
		return name;
	}
	
	public String addPart(String part1, String separator, String part2) {
		if (StringUtils.isEmpty(part1)) {
			return part2;
		}
		if (StringUtils.isEmpty(part2)) {
			return part1;
		}
		return part1+separator+part2;
	}
	
	public String addExceptionSpecificDetails(Throwable t, String currentResult) {
		if (t instanceof AddressException) { 
			AddressException ae = (AddressException)t;
			String parsedString=ae.getRef();
			if (StringUtils.isNotEmpty(parsedString)) {
				currentResult = addPart(currentResult, " ", "["+parsedString+"]");
			}
			int column = ae.getPos()+1;
			if (column>0) {
				currentResult = addPart(currentResult, " ", "at column ["+column+"]");
			}
		}
		return currentResult;
	}
	
    public String getMessage() {
	    String messages[]=getMessages(); 
	    Throwable throwables[]=getThrowables();
		String result=null;
		String prev_message=null;

		for(int i=messages.length-1; i>=0; i--) {
			String newPart=null;
			
			addExceptionSpecificDetails(throwables[i], newPart);
			
			// prefix the result with the message of this exception.
			// if the new message ends with the previous, remove the part that is already known
			if (messages[i]!=null) {
				newPart = addPart(messages[i], " ", newPart);
				if (prev_message!=null && newPart.endsWith(prev_message)) {
					newPart=newPart.substring(0,newPart.length()-prev_message.length());
				}
				if (newPart.endsWith(": ")) {
					newPart=newPart.substring(0,newPart.length()-2);
				}
				prev_message=messages[i];
			}
			
			if (!(throwables[i] instanceof IbisException)) { 
				String exceptionType = "("+getExceptionType(throwables[i])+")";
				newPart = addPart(exceptionType, " ", newPart);
			}
			result = addPart(newPart, ": ", result);
		}
		
	    if (result==null) {
	    	// do not replace the following with toString(), this causes an endless loop. GvB
		    result="no message, fields of this exception: "+ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
	    }
	   
	    return result;
    }

	public String toString() {
		String result="";
		Throwable t;

		result = "message: "+getMessage()+"\ncause-trace:\n"; 
		for (t=this; t!=null; t=ExceptionUtils.getCause(t)) {
			result += "\nclass: "+t.getClass().getName()+"\nmessage:"+t.getMessage()+"\n";
		}
		t=ExceptionUtils.getRootCause(this);
		if (t!=null) {
			result += "\nroot cause:\n"+ToStringBuilder.reflectionToString(t,ToStringStyle.MULTI_LINE_STYLE)+"\n";
		}
		return result;
	}
}
