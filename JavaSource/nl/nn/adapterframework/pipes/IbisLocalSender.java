/*
 * $Log: IbisLocalSender.java,v $
 * Revision 1.20  2008-08-06 16:38:20  europe\L190409
 * moved from pipes to senders package
 *
 * Revision 1.19  2007/12/10 10:10:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * no transactions in IsolatedServiceCaller
 *
 * Revision 1.18  2007/11/22 15:18:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved javadoc
 *
 * Revision 1.17  2007/09/10 11:19:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.16  2007/09/05 13:04:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for returning session keys
 *
 * Revision 1.15  2007/08/30 15:10:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added timeout to dependency
 *
 * Revision 1.14  2007/08/29 15:09:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute checkDependency
 *
 * Revision 1.13  2007/05/29 11:10:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implementation of HasPhysicalDestination
 *
 * Revision 1.12  2007/05/16 11:46:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved javadoc
 *
 * Revision 1.11  2006/08/22 06:51:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 * adapted code calling IsolatedServiceCaller
 *
 * Revision 1.10  2006/07/17 09:03:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * force isolated=true if synchronous=false
 *
 * Revision 1.9  2006/07/14 10:04:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added asynchronous-option, by setting synchronous=false
 *
 * Revision 1.8  2005/12/28 08:37:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.7  2005/10/18 07:02:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * better exception handling
 *
 * Revision 1.6  2005/09/28 14:15:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added super.configure()
 *
 * Revision 1.5  2005/09/26 11:54:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enabeld isolated calls from IbisLocalSender to JavaListener as well as to WebServiceListener
 *
 * Revision 1.4  2005/09/07 15:36:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute "isolated", to enable sub-transactions
 *
 * Revision 1.3  2005/08/30 16:02:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made parameterized version
 *
 * Revision 1.2  2005/08/24 15:53:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved error message for configuration exception
 *
 * Revision 1.1  2004/08/09 13:50:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;


/**
 * Posts a message to another IBIS-adapter in the same IBIS instance.
 * 
 * An IbisLocalSender makes a call to a Receiver with either a {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener}
 * or a {@link nl.nn.adapterframework.receivers.JavaListener JavaListener}. 
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.pipes.IbisLocalSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>Name of the {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener} that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJavaListener(String) javaListener}</td><td>Name of the {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIsolated(boolean) isolated}</td><td>when <code>true</code>, the call is made in a separate thread, possibly using separate transaction</td><td>false</td></tr>
 * <tr><td>{@link #setCheckDependency(boolean) checkDependency}</td><td>when <code>true</code>, the sender waits upon open until the called {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} is opened</td><td>true</td></tr>
 * <tr><td>{@link #setDependencyTimeOut(int) dependencyTimeOut}</td><td>maximum time (in seconds) the sender waits for the listener to start</td><td>60 s</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td> when set <code>false</code>, the call is made asynchronously. This implies <code>isolated=true</code></td><td>true</td></tr>
 * <tr><td>{@link #setReturnedSessionKeys(String) returnedSessionKeys}</td><td>comma separated list of keys of session variables that should be returned to caller, 
 *         for correct results as well as for erronous results. (Only for listeners that support it, like JavaListener)<br/>
 *         N.B. To get this working, the attribute returnedSessionKeys must also be set on the corresponding Receiver</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * Any parameters are copied to the PipeLineSession of the service called.
 * 
 * <h3>Configuration of the Adapter to be called</h3>
 * A call to another Adapter in the same IBIS instance is preferably made using the combination
 * of an IbisLocalSender and a {@link nl.nn.adapterframework.receivers.JavaListener JavaListener}. If, 
 * however, a Receiver with a {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener} is already present, that can be used in some cases, too.
 *  
 * <h4>configuring IbisLocalSender and JavaListener</h4>
 * <ul>
 *   <li>Define a GenericMessageSendingPipe with an IbisLocalSender</li>
 *   <li>Set the attribute <code>javaListener</code> to <i>yourServiceName</i></li>
 *   <li>Do not set the attribute <code>serviceName</code></li>
 * </ul>
 * In the Adapter to be called:
 * <ul>
 *   <li>Define a Receiver with a JavaListener</li>
 *   <li>Set the attribute <code>name</code> to <i>yourServiceName</i></li>
 *   <li>Do not set the attribute <code>serviceName</code>, except if the service is to be called also
 *       from applications other than this IBIS-instance</li>
 * </ul>
 * 
 * <h4>configuring IbisLocalSender and WebServiceListener</h4>
 * 
 * <ul>
 *   <li>Define a GenericMessageSendingPipe with an IbisLocalSender</li>
 *   <li>Set the attribute <code>serviceName</code> to <i>yourIbisWebServiceName</i></li>
 *   <li>Do not set the attribute <code>javaListener</code></li>
 * </ul>
 * In the Adapter to be called:
 * <ul>
 *   <li>Define a Receiver with a WebServiceListener</li>
 *   <li>Set the attribute <code>name</code> to <i>yourIbisWebServiceName</i></li>
 * </ul>
 *
 * @author Gerrit van Brakel
 * @since  4.2
 * @deprecated Please replace with nl.nn.adapterframework.senders.IbisLocalSender
 */
public class IbisLocalSender extends nl.nn.adapterframework.senders.IbisLocalSender {

	public void configure() throws ConfigurationException {
		log.warn(getLogPrefix()+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+super.getClass().getName()+"]");
		super.configure();
	}
}
