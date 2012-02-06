/*
 * $Log: LoggingSOAPFaultListener.java,v $
 * Revision 1.1  2012-02-06 13:18:20  l190409
 * improved SOAP error logging
 *
 */
package nl.nn.adapterframework.soap;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.apache.soap.server.DOMFaultListener;
import org.apache.soap.server.SOAPFaultEvent;

public class LoggingSOAPFaultListener extends DOMFaultListener {
	protected Logger log = LogUtil.getLogger(this);

	public void fault(SOAPFaultEvent _faultEvent) {
		super.fault(_faultEvent);
		log.warn("observed SoapFault ["+_faultEvent.getFault()+"], source ["+_faultEvent.getSource()+"]", _faultEvent.getSOAPException());
	}

}
