/*
 * $Log: IbisException.java,v $
 * Revision 1.24  2008-08-12 15:13:48  europe\L190409
 * removed duplicate parts in getMessage
 *
 * Revision 1.23  2008/03/28 14:50:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed position of XML location info
 *
 * Revision 1.22  2008/03/20 11:57:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * do not skip Exception from classname
 *
 * Revision 1.21  2007/07/17 15:08:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * tune SQL exception
 *
 * Revision 1.20  2007/07/17 10:46:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added SQLException specific fields
 *
 * Revision 1.19  2005/10/17 08:52:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * included location info for TransformerExceptions
 *
 * Revision 1.18  2005/08/08 09:40:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed dedicated toString()
 *
 * Revision 1.17  2005/07/28 07:28:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reduced extensive toString, as it was too much, recursively
 *
 * Revision 1.16  2005/07/19 12:12:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved rendering of SaxParseException
 *
 * Revision 1.15  2005/07/05 11:01:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved rendering of errormessage
 *
 * Revision 1.14  2005/04/26 15:14:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import java.sql.SQLException;

import javax.mail.internet.AddressException;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.exception.NestableException;
import org.xml.sax.SAXParseException;

/**
 * Base Exception with compact but informative getMessage().
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public class IbisException extends NestableException {
//	private Logger log = LogUtil.getLogger(this);

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
	
	public String getExceptionSpecificDetails(Throwable t) {
		String result=null;
		if (t instanceof AddressException) { 
			AddressException ae = (AddressException)t;
			String parsedString=ae.getRef();
			if (StringUtils.isNotEmpty(parsedString)) {
				result = addPart(result, " ", "["+parsedString+"]");
			}
			int column = ae.getPos()+1;
			if (column>0) {
				result = addPart(result, " ", "at column ["+column+"]");
			}
		}
		if (t instanceof SAXParseException) {
			SAXParseException spe = (SAXParseException)t;
			int line = spe.getLineNumber();
			int col = spe.getColumnNumber();
			String sysid = spe.getSystemId();
			
			String locationInfo=null;
			if (StringUtils.isNotEmpty(sysid)) {
				locationInfo =  "SystemId ["+sysid+"]";
			}
			if (line>=0) {
				locationInfo =  addPart(locationInfo, " ", "line ["+line+"]");
			}
			if (col>=0) {
				locationInfo =  addPart(locationInfo, " ", "column ["+col+"]");
			}
			result = addPart(locationInfo, ": ", result);
		} 
		if (t instanceof TransformerException) {
			TransformerException te = (TransformerException)t;
			SourceLocator locator = te.getLocator();
			if (locator!=null) {
				int line = locator.getLineNumber();
				int col = locator.getColumnNumber();
				String sysid = locator.getSystemId();
				
				String locationInfo=null;
				if (StringUtils.isNotEmpty(sysid)) {
					locationInfo =  "SystemId ["+sysid+"]";
				}
				if (line>=0) {
					locationInfo =  addPart(locationInfo, " ", "line ["+line+"]");
				}
				if (col>=0) {
					locationInfo =  addPart(locationInfo, " ", "column ["+col+"]");
				}
				result = addPart(locationInfo, ": ", result);
			}
		} 
		if (t instanceof SQLException) {
			SQLException sqle = (SQLException)t;
			int errorCode = sqle.getErrorCode();
			String sqlState = sqle.getSQLState();
			if (errorCode!=0) {
				result =  addPart("errorCode ["+errorCode+"]", ", ", result);
			}
			if (StringUtils.isNotEmpty(sqlState)) {
				result =  addPart("SQLState ["+sqlState+"]", ", ", result);
			}
		} 
		return result;
	}
	
    public String getMessage() {
	    Throwable throwables[]=getThrowables();
		String result=null;
		String prev_message=null;


		for(int i=getThrowableCount()-1; i>=0; i--) {
			
			String cur_message=getMessage(i);
			
//			if (log.isDebugEnabled()) {
//				log.debug("t["+i+"], ["+ClassUtils.nameOf(throwables[i])+"], cur ["+cur_message+"], prev ["+prev_message+"]");
//			} 			
			
			String newPart=null;
			
			// prefix the result with the message of this exception.
			// if the new message ends with the previous, remove the part that is already known
			if (StringUtils.isNotEmpty(cur_message)) {
				newPart = addPart(cur_message, " ", newPart);
				if (StringUtils.isNotEmpty(newPart) && StringUtils.isNotEmpty(prev_message) && newPart.endsWith(prev_message)) {
					newPart=newPart.substring(0,newPart.length()-prev_message.length());
				}
				if (StringUtils.isNotEmpty(newPart) && newPart.endsWith(": ")) {
					newPart=newPart.substring(0,newPart.length()-2);
				}
				prev_message=cur_message;
			}
			String specificDetails = getExceptionSpecificDetails(throwables[i]);
			if (StringUtils.isNotEmpty(specificDetails) && (result==null || result.indexOf(specificDetails)<0)) {
				newPart= addPart(specificDetails,": ",newPart);
			}
			
			if (!(throwables[i] instanceof IbisException)) { 
				String exceptionType = "("+getExceptionType(throwables[i])+")";
				newPart = addPart(exceptionType, " ", newPart);
			}
			result = addPart(newPart, ": ", result);
		}
		
	    if (result==null) {
//	    	log.debug("no message found, returning fields by inspection");
	    	// do not replace the following with toString(), this causes an endless loop. GvB
		    result="no message, fields of this exception: "+ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
	    }
	    return result;
    }

/*
	public String toString() {
		String result = super.toString();
		Throwable t = getCause();
		if (t != null && !(t instanceof IbisException)) {
			t=ExceptionUtils.getRootCause(this);
			result += "\nroot cause:\n"+ToStringBuilder.reflectionToString(t,ToStringStyle.MULTI_LINE_STYLE)+"\n";
		}
		return result;
	}
	*/
}
