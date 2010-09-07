/*
 * $Log: SenderWrapperBaseProcessor.java,v $
 * Revision 1.2  2010-09-07 15:55:13  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.SenderWrapperBase;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class SenderWrapperBaseProcessor {
	private Logger log = LogUtil.getLogger(this);

	public String sendMessage(SenderWrapperBase senderWrapperBase, String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		String senderInput=(String)message;
		if (StringUtils.isNotEmpty(senderWrapperBase.getGetInputFromSessionKey())) {
			senderInput=(String)prc.getSession().get(senderWrapperBase.getGetInputFromSessionKey());
			if (log.isDebugEnabled()) log.debug(senderWrapperBase.getLogPrefix()+"set contents of session variable ["+senderWrapperBase.getGetInputFromSessionKey()+"] as input ["+senderInput+"]");
		} else {
			if (StringUtils.isNotEmpty(senderWrapperBase.getGetInputFromFixedValue())) {
				senderInput=senderWrapperBase.getGetInputFromFixedValue();
				if (log.isDebugEnabled()) log.debug(senderWrapperBase.getLogPrefix()+"set input to fixed value ["+senderInput+"]");
			}
		}
		String result = senderWrapperBase.doSendMessage(correlationID, senderInput, prc);
		if (StringUtils.isNotEmpty(senderWrapperBase.getStoreResultInSessionKey())) {
			if (log.isDebugEnabled()) log.debug(senderWrapperBase.getLogPrefix()+"storing results in session variable ["+senderWrapperBase.getStoreResultInSessionKey()+"]");
			prc.getSession().put(senderWrapperBase.getStoreResultInSessionKey(),result);
		}
		return senderWrapperBase.isPreserveInput()?(String)message:result;
	}


}
