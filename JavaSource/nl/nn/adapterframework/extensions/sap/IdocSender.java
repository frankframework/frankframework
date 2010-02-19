/*
 * $Log: IdocSender.java,v $
 * Revision 1.3  2010-02-19 13:45:28  m00f069
 * - Added support for (sender) stubbing by debugger
 * - Added reply listener and reply sender to debugger
 * - Use IbisDebuggerDummy by default
 * - Enabling/disabling debugger handled by debugger instead of log level
 * - Renamed messageId to correlationId in debugger interface
 *
 * Revision 1.2  2009/08/26 15:34:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.1  2008/01/30 14:44:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.debug.IbisDebugger;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.senders.SenderBase;
import nl.nn.adapterframework.util.XmlUtils;

import com.sap.mw.idoc.IDoc;
import com.sap.mw.jco.JCO;

/**
 * Implementation of {@link ISender} that sends an IDoc to SAP.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.sap.IdocSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemName(String) sapSystemName}</td><td>name of the {@link SapSystem} used by this object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemNameParam(String) sapSystemNameParam}</td><td>name of the parameter used to indicate the name of the {@link SapSystem} used by this object if the attribute <code>sapSystemName</code> is empty</td><td>sapSystemName</td></tr>
 * </table>
 * </p>
 * N.B. The sending of the iDoc is committed right after the XA transaction is completed.
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>sapSystemName</td><td>String</td><td>points to {@link SapSystem} to use; required when attribute <code>sapSystemName</code> is empty</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class IdocSender extends SapSenderBase {
	private IbisDebugger ibisDebugger;
	
	protected IDoc.Document parseIdoc(SapSystem sapSystem, String message) throws SenderException {
		
		IdocXmlHandler handler = new IdocXmlHandler(sapSystem);
	
		try {
			log.debug(getLogPrefix()+"start parsing Idoc");
			XmlUtils.parseXml(handler, message);	
			log.debug(getLogPrefix()+"finished parsing Idoc");
			return handler.getIdoc();
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}


	
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		message = ibisDebugger.senderInput(this, correlationID, message);
		String result = null;
		try {
			if (!ibisDebugger.stubSender(this, correlationID)) {
				String tid=null;
				try {
					ParameterValueList pvl = null;
					if (prc!=null) {
						pvl=prc.getValues(paramList);
					}
					SapSystem sapSystem = getSystem(pvl);
					
					IDoc.Document idoc = parseIdoc(sapSystem,message);
					
					try {
						log.debug(getLogPrefix()+"checking syntax");
						idoc.checkSyntax();
					}
					catch ( IDoc.Exception e ) {
						throw new SenderException("Syntax error in idoc", e);
					}
		
					if (log.isDebugEnabled()) { log.debug(getLogPrefix()+"parsed idoc ["+idoc.toXML()+"]"); } 
		
					JCO.Client client = getClient(prc.getSession(), sapSystem);
					try {
						tid=getTid(client,sapSystem);
						if (tid==null) {
							throw new SenderException("could not obtain TID to send Idoc");
						}
						client.send(idoc,tid);
					} finally {
						releaseClient(client,sapSystem);
					}
					result = tid;
				} catch (Exception e) {
					throw new SenderException(e);
				}
			}
		} catch(Throwable throwable) {
			throwable = ibisDebugger.senderAbort(this, correlationID, throwable);
			SenderBase.throwSenderOrTimeOutException(this, throwable);
		}
		return ibisDebugger.senderOutput(this, correlationID, result);
	}
	
	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

}
