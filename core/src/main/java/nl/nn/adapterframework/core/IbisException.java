/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
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
		return t.getClass().getSimpleName();
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
		if (t.getClass().getSimpleName().equals("OracleXAException")) { // do not use instanceof here, to avoid unnessecary dependency on Oracle class
			oracle.jdbc.xa.OracleXAException oxae = (oracle.jdbc.xa.OracleXAException)t;
			int xaError = oxae.getXAError();
			if (xaError != 0) {
				result =  addPart("xaError ["+xaError +"] xaErrorMessage ["+oracle.jdbc.xa.OracleXAException.getXAErrorMessage(xaError)+"]", ", ", result);
			}
		} 
		return result;
	}

	@Override
	public String getMessage() {
		Throwable throwables[]=getThrowables();
		String result=null;
		String prev_message=null;
		Throwable prevThrowable=null;


		for(int i=getThrowableCount()-1; i>=0; i--) {
			
			String cur_message=getMessage(i);
			
//			if (log.isDebugEnabled()) {
//				log.debug("t["+i+"], ["+ClassUtils.nameOf(throwables[i])+"], cur ["+cur_message+"], prev ["+prev_message+"]");
//			} 			
			if (prevThrowable!=null && cur_message!=null && (cur_message.equals(prevThrowable.getMessage()) || cur_message.equals(prevThrowable.toString()))) {
				cur_message=null;
			}
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
			prevThrowable=throwables[i];
		}
		
		if (result==null) {
//			log.debug("no message found, returning fields by inspection");
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
