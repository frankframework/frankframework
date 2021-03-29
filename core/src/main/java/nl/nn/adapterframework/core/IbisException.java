/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
import java.util.LinkedList;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xml.sax.SAXParseException;

import nl.nn.adapterframework.util.Misc;

/**
 * Base Exception with compact but informative getMessage().
 * 
 * @author Gerrit van Brakel
 */
public class IbisException extends Exception {
	
	private String expandedMessage = null;
	
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
		
	public static String getExceptionSpecificDetails(Throwable t) {
		String result=null;
		if (t instanceof AddressException) { 
			AddressException ae = (AddressException)t;
			String parsedString=ae.getRef();
			if (StringUtils.isNotEmpty(parsedString)) {
				result = Misc.concatStrings(result, " ", "["+parsedString+"]");
			}
			int column = ae.getPos()+1;
			if (column>0) {
				result = Misc.concatStrings(result, " ", "at column ["+column+"]");
			}
		}
		if (t instanceof SAXParseException) {
			SAXParseException spe = (SAXParseException)t;
			int line = spe.getLineNumber();
			int col = spe.getColumnNumber();
			String sysid = spe.getSystemId();
			
			String locationInfo=null;
			if (StringUtils.isNotEmpty(sysid)) {
				locationInfo = "SystemId ["+sysid+"]";
			}
			if (line>=0) {
				locationInfo = Misc.concatStrings(locationInfo, " ", "line ["+line+"]");
			}
			if (col>=0) {
				locationInfo = Misc.concatStrings(locationInfo, " ", "column ["+col+"]");
			}
			result = Misc.concatStrings(locationInfo, ": ", result);
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
					locationInfo = "SystemId ["+sysid+"]";
				}
				if (line>=0) {
					locationInfo = Misc.concatStrings(locationInfo, " ", "line ["+line+"]");
				}
				if (col>=0) {
					locationInfo = Misc.concatStrings(locationInfo, " ", "column ["+col+"]");
				}
				result = Misc.concatStrings(locationInfo, ": ", result);
			}
		} 
		if (t instanceof SQLException) {
			SQLException sqle = (SQLException)t;
			int errorCode = sqle.getErrorCode();
			String sqlState = sqle.getSQLState();
			if (errorCode!=0) {
				result = Misc.concatStrings("errorCode ["+errorCode+"]", ", ", result);
			}
			if (StringUtils.isNotEmpty(sqlState)) {
				result = Misc.concatStrings("SQLState ["+sqlState+"]", ", ", result);
			}
		} 
		if (t.getClass().getSimpleName().equals("OracleXAException")) { // do not use instanceof here, to avoid unnessecary dependency on Oracle class
			oracle.jdbc.xa.OracleXAException oxae = (oracle.jdbc.xa.OracleXAException)t;
			int xaError = oxae.getXAError();
			if (xaError != 0) {
				result = Misc.concatStrings("xaError ["+xaError +"] xaErrorMessage ["+oracle.jdbc.xa.OracleXAException.getXAErrorMessage(xaError)+"]", ", ", result);
			}
		} 
		return result;
	}

	@Override
	public String getMessage() {
		if (expandedMessage == null) {
			List<String> msgChain = getMessages(this, super.getMessage());
			Throwable t = this;
			for(String message:msgChain) {
				String exceptionType = t instanceof IbisException ? "" : "("+t.getClass().getSimpleName()+")";
				message = Misc.concatStrings(exceptionType, " ", message);
				expandedMessage = Misc.concatStrings(expandedMessage, ": ", message);
				t = ExceptionUtils.getCause(t);
			}
			if (expandedMessage==null) {
				// do not replace the following with toString(), this causes an endless loop. GvB
				expandedMessage="no message, fields of this exception: "+ToStringBuilder.reflectionToString(this);
			}
		}
		return expandedMessage;
	}



	public static LinkedList<String> getMessages(Throwable t, String message) {
		Throwable cause = ExceptionUtils.getCause(t);
		LinkedList<String> result;
		if (cause !=null) {
			String causeMessage = cause.getMessage();
			String causeToString = cause.toString(); 

			if (cause instanceof IbisException) {
				// in case of an IbisException, the recursion already happened in cause.getMessage(), so do not call getMessages() here.
				result = new LinkedList<>();
				result.add(causeMessage);
			} else {
				result = getMessages(cause, causeMessage);
			}
			if (StringUtils.isNotEmpty(message) && (message.equals(causeMessage) || message.equals(causeToString))) {
				message = null;
			}
			if (StringUtils.isNotEmpty(message) && StringUtils.isNotEmpty(causeToString) && (message.endsWith(causeToString))) {
				message=message.substring(0,message.length()-causeToString.length());
			}
			if (StringUtils.isNotEmpty(message) && StringUtils.isNotEmpty(causeMessage)  && (message.endsWith(causeMessage))) {
				message=message.substring(0,message.length()-causeMessage.length());
			}
		} else {
			result = new LinkedList<>();
		}
		if (StringUtils.isNotEmpty(message) && (message.endsWith(": "))) {
			message=message.substring(0,message.length()-2);
		}
		String specificDetails = getExceptionSpecificDetails(t);
		if (StringUtils.isNotEmpty(specificDetails)) {
			boolean tailContainsDetails=false;
			for(String part:result) {
				if (part!=null && part.indexOf(specificDetails)>=0) {
					tailContainsDetails = true;
					break;
				}
			}
			if (!tailContainsDetails) {
				message= Misc.concatStrings(specificDetails, ": ", message);
			}
		}
		result.addFirst(message);
		return result;
	}
}
