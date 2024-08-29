/*
   Copyright 2013 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package org.frankframework.core;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xml.sax.SAXParseException;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.StringUtil;

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

	public static String getExceptionSpecificDetails(@Nonnull Throwable t) {
		final String className = t.getClass().getCanonicalName();
		switch (className) {
			case "jakarta.mail.internet.AddressException": {
				jakarta.mail.internet.AddressException ae = (jakarta.mail.internet.AddressException) t;
				final String parsedString = ae.getRef();
				final String errorMessage = StringUtils.isNotEmpty(parsedString) ? "[" + parsedString + "]" : null;
				final int column = ae.getPos() + 1;
				return column > 0 ? StringUtil.concatStrings(errorMessage, " ", "at column [" + column + "]") : errorMessage;
			}

			case "org.xml.sax.SAXParseException": {
				return getSAXLocatorInformation((SAXParseException) t);
			}
			case "javax.xml.transform.TransformerException": {
				return getTransformerLocatorInformation((TransformerException) t);
			}
			case "java.sql.SQLException": {
				SQLException sqle = (SQLException) t;
				int errorCode = sqle.getErrorCode();
				String sqlState = sqle.getSQLState();
				String result = null;
				if (errorCode != 0) {
					result = StringUtil.concatStrings("errorCode [" + errorCode + "]", ", ", result);
				}
				if (StringUtils.isNotEmpty(sqlState)) {
					result = StringUtil.concatStrings("SQLState [" + sqlState + "]", ", ", result);
				}
				return result;
			}

			case "oracle.jdbc.xa.OracleXAException": {
				oracle.jdbc.xa.OracleXAException oxae = (oracle.jdbc.xa.OracleXAException) t;
				int xaError = oxae.getXAError();
				return xaError != 0 ? "xaError [" + xaError + "] xaErrorMessage [" + oracle.jdbc.xa.OracleXAException.getXAErrorMessage(xaError) + "]" : null;
			}

			default:
				return null;
		}
	}

	private static String getSAXLocatorInformation(SAXParseException spe) {
		return compileLocatorInformation(spe.getSystemId(), spe.getLineNumber(), spe.getColumnNumber());
	}

	private static String getTransformerLocatorInformation(TransformerException te) {
		SourceLocator locator = te.getLocator();
		if (locator == null) {
			return null;
		}

		return compileLocatorInformation(locator.getSystemId(), locator.getLineNumber(), locator.getColumnNumber());
	}

	private static String compileLocatorInformation(String systemId, int line, int column) {
		String locationInfo = null;
		if (StringUtils.isNotEmpty(systemId)) {
			locationInfo = "SystemId [" + systemId + "]";
		}
		if (line >= 0) {
			locationInfo = StringUtil.concatStrings(locationInfo, " ", "line [" + line + "]");
		}
		if (column >= 0) {
			locationInfo = StringUtil.concatStrings(locationInfo, " ", "column [" + column + "]");
		}
		return locationInfo;
	}

	public static String expandMessage(String msg, Throwable e) {
		return expandMessage(msg, e, IbisException.class::isInstance);
	}

	public static String expandMessage(String msg, Throwable e, ExcludeClassInfoExceptionFilter filter) {
		String result = null;
		List<String> msgChain = getMessages(e, msg);
		Throwable t = e;
		for (String message : msgChain) {
			String exceptionType = filter.accept(t) ? "" : "(" + ClassUtils.classNameOf(t) + ")";
			message = StringUtil.concatStrings(exceptionType, " ", message);
			result = StringUtil.concatStrings(result, ": ", message);
			t = getCause(t);
		}
		if (result == null) {
			// do not replace the following with toString(), this causes an endless loop. GvB
//			result="no message, fields of this exception: " + ReflectionToStringBuilder.toStringExclude(e, "java.lang.Exception.serialVersionUID");
			result = "no message in exception: " + e.getClass().getCanonicalName();
		}
		return result;
	}

	/**
	 * <p>Introspects the {@code Throwable} to obtain the cause.</p>
	 *
	 * <p>The method searches for methods with specific names that return a
	 * {@code Throwable} object. This will pick up most wrapping exceptions,
	 * including those from JDK 1.4.
	 *
	 * <p>The default list searched for are:</p>
	 * <ul>
	 *  <li>{@code getCause()}</li>
	 *  <li>{@code getNextException()}</li>
	 *  <li>{@code getTargetException()}</li>
	 *  <li>{@code getException()}</li>
	 *  <li>{@code getSourceException()}</li>
	 *  <li>{@code getRootCause()}</li>
	 *  <li>{@code getCausedByException()}</li>
	 *  <li>{@code getNested()}</li>
	 *  <li>{@code getLinkedException()}</li>
	 *  <li>{@code getNestedException()}</li>
	 *  <li>{@code getLinkedCause()}</li>
	 *  <li>{@code getThrowable()}</li>
	 * </ul>
	 *
	 * <p>If none of the above is found, returns {@code null}.</p>
	 */
	@Nullable
	private static Throwable getCause(Throwable t) {
		Throwable cause = ExceptionUtils.getCause(t);
		if (cause == null && t != null && t.getSuppressed().length > 0) {
			return t.getSuppressed()[0];
		}
		return cause;
	}

	public static LinkedList<String> getMessages(Throwable t, String message) {
		Throwable cause = getCause(t);
		LinkedList<String> result;
		if (cause != null) {
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
				message = message.substring(0, message.length() - causeToString.length());
			}
			if (StringUtils.isNotEmpty(message) && StringUtils.isNotEmpty(causeMessage) && (message.endsWith(causeMessage))) {
				message = message.substring(0, message.length() - causeMessage.length());
			}
		} else {
			result = new LinkedList<>();
		}
		if (StringUtils.isNotEmpty(message) && (message.endsWith(": "))) {
			message = message.substring(0, message.length() - 2);
		}
		String specificDetails = getExceptionSpecificDetails(t);
		if (StringUtils.isNotEmpty(specificDetails)) {
			boolean tailContainsDetails = false;
			for (String part : result) {
				if (part != null && part.contains(specificDetails)) {
					tailContainsDetails = true;
					break;
				}
			}
			if (!tailContainsDetails) {
				message = StringUtil.concatStrings(specificDetails, ": ", message);
			}
		}
		result.addFirst(message);
		return result;
	}

	@Override
	public String getMessage() {
		if (expandedMessage == null) {
			expandedMessage = expandMessage(super.getMessage(), this);
		}
		return expandedMessage;
	}

	@FunctionalInterface
	public interface ExcludeClassInfoExceptionFilter {
		boolean accept(Throwable t);
	}
}
