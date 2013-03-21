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
/*
 * $Log: CompareIntegerPipe.java,v $
 * Revision 1.5  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.4  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2009/03/16 16:14:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected documentation
 *
 * Revision 1.1  2007/06/21 07:06:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added CompareIntegerPipe and IncreaseIntegerPipe
 *
 */

package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe that compares the integer values of two session variables.
 * Used to in combination with {@link IncreaseIntegerPipe} to contstruct loops.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.CompareIntegerPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setActive(boolean) active}</td><td>controls whether Pipe is included in configuration. When set <code>false</code> or set to something else as "true", (even set to the empty string), the Pipe is not included in the configuration</td><td>true</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified, then the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of possible XML parsing in descender-classes</td><td>application default</td></tr>
 * <tr><td>{@link #setTransactionAttribute(String) transactionAttribute}</td><td>Defines transaction and isolation behaviour. Equal to <A href="http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494">EJB transaction attribute</a>. Possible values are: 
 *   <table border="1">
 *   <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipe excecuted in Transaction</th></tr>
 *   <tr><td colspan="1" rowspan="2">Required</td>    <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">RequiresNew</td> <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T2</td></tr>
 *   <tr><td colspan="1" rowspan="2">Mandatory</td>   <td>none</td><td>error</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">NotSupported</td><td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>none</td></tr>
 *   <tr><td colspan="1" rowspan="2">Supports</td>    <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">Never</td>       <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>error</td></tr>
 *  </table></td><td>Supports</td></tr>
 * <tr><td>{@link #setBeforeEvent(int) beforeEvent}</td>      <td>METT eventnumber, fired just before a message is processed by this Pipe</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setAfterEvent(int) afterEvent}</td>        <td>METT eventnumber, fired just after message processing by this Pipe is finished</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setExceptionEvent(int) exceptionEvent}</td><td>METT eventnumber, fired when message processing by this Pipe resulted in an exception</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setSessionKey1(String) sessionKey1}</td><td>reference to one of the session variables to be compared</td><td></td></tr>
 * <tr><td>{@link #setSessionKey2(String) sessionKey2}</td><td>reference to the other session variables to be compared</td><td></td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>lessthan</td><td>when v1 &lt; v2</td></tr>
 * <tr><td>greaterthan</td><td>when v1 &gt; v2</td></tr>
 * <tr><td>equals</td><td>when v1 = v1</td></tr>
 * </table>
 * </p>
 * @version $Id$
 * @author     Richard Punt / Gerrit van Brakel
 */
public class CompareIntegerPipe extends AbstractPipe {

	private final static String LESSTHANFORWARD = "lessthan";
	private final static String GREATERTHANFORWARD = "greaterthan";
	private final static String EQUALSFORWARD = "equals";

	private String sessionKey1 = null;
	private String sessionKey2 = null;

	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(sessionKey1))
			throw new ConfigurationException(getLogPrefix(null) + "sessionKey1 must be filled");

		if (StringUtils.isEmpty(sessionKey2))
			throw new ConfigurationException(getLogPrefix(null) + "sessionKey2 must be filled");

		if (null == findForward(LESSTHANFORWARD))
			throw new ConfigurationException(getLogPrefix(null)	+ "forward ["+ LESSTHANFORWARD+ "] is not defined");

		if (null == findForward(GREATERTHANFORWARD))
			throw new ConfigurationException(getLogPrefix(null)	+ "forward ["+ GREATERTHANFORWARD+ "] is not defined");

		if (null == findForward(EQUALSFORWARD))
			throw new ConfigurationException(getLogPrefix(null)	+ "forward ["+ EQUALSFORWARD+ "] is not defined");
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
		throws PipeRunException {

		String sessionKey1StringValue = (String) session.get(sessionKey1);
		String sessionKey2StringValue = (String) session.get(sessionKey2);

		if (log.isDebugEnabled()) {
			log.debug("sessionKey1StringValue [" + sessionKey1StringValue + "]");
			log.debug("sessionKey2StringValue [" + sessionKey2StringValue + "]");
		}

		Integer sessionKey1IntegerValue;
		Integer sessionKey2IntegerValue;
		try {
			sessionKey1IntegerValue = new Integer(sessionKey1StringValue);
			sessionKey2IntegerValue = new Integer(sessionKey2StringValue);
		} catch (Exception e) {
			PipeRunException prei =
				new PipeRunException(
					this, "Exception while comparing integers", e);
			throw prei;
		}

		int comparison=sessionKey1IntegerValue.compareTo(sessionKey2IntegerValue);
		if (comparison == 0)
			return new PipeRunResult(findForward(EQUALSFORWARD), input);
		else if (comparison < 0)
			return new PipeRunResult(findForward(LESSTHANFORWARD), input);
		else
			return new PipeRunResult(findForward(GREATERTHANFORWARD), input);

	}

	public void setSessionKey1(String string) {
		sessionKey1 = string;
	}
	public String getSessionKey1() {
		return sessionKey1;
	}

	public void setSessionKey2(String string) {
		sessionKey2 = string;
	}
	public String getSessionKey2() {
		return sessionKey2;
	}

}
