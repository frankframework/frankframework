/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.extensions.sap.jco3;

import com.sap.conn.idoc.IDocDocument;
import com.sap.conn.idoc.IDocException;
import com.sap.conn.idoc.IDocFactory;
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.jco.JCoDestination;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Implementation of {@link nl.nn.adapterframework.core.ISender sender} that sends an IDoc to SAP.
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
 * @author  Jaco de Groot
 * @since   5.0
 */
public class IdocSender extends SapSenderBase {

	protected IDocDocument parseIdoc(SapSystem sapSystem, Message message) throws SenderException {
		
		IdocXmlHandler handler = new IdocXmlHandler(sapSystem);
	
		try {
			log.debug(getLogPrefix()+"start parsing Idoc");
			XmlUtils.parseXml(message.asInputSource(), handler);	
			log.debug(getLogPrefix()+"finished parsing Idoc");
			return handler.getIdoc();
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		String tid=null;
		try {
			ParameterValueList pvl = null;
			if (paramList!=null) {
				pvl = paramList.getValues(message, session);
			}
			SapSystem sapSystem = getSystem(pvl);
			
			IDocDocument idoc = parseIdoc(sapSystem,message);
			
			try {
				log.trace(getLogPrefix()+"checking syntax");
				idoc.checkSyntax();
			}
			catch ( IDocException e ) {
				throw new SenderException("Syntax error in idoc", e);
			}

			if (log.isDebugEnabled()) { log.debug(getLogPrefix()+"parsed idoc ["+JCoIDoc.getIDocFactory().getIDocXMLProcessor().render(idoc)+"]"); }


			JCoDestination destination = getDestination(session, sapSystem);
			tid=getTid(destination,sapSystem);
			if (tid==null) {
				throw new SenderException("could not obtain TID to send Idoc");
			}
			JCoIDoc.send(idoc,IDocFactory.IDOC_VERSION_DEFAULT ,destination,tid);
			return new Message(tid);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	protected String getFunctionName() {
		return null;
	}
}
