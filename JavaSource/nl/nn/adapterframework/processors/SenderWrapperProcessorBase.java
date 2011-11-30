/*
 * $Log: SenderWrapperProcessorBase.java,v $
 * Revision 1.3  2011-11-30 13:51:54  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/09/13 13:45:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced Processor base classes
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Baseclass for SenderWrapperProcessor.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
 */
public abstract class SenderWrapperProcessorBase implements SenderWrapperProcessor {
	protected Logger log = LogUtil.getLogger(this);

	protected SenderWrapperProcessor senderWrapperProcessor;

	public void setSenderWrapperProcessor(SenderWrapperProcessor senderWrapperProcessor) {
		this.senderWrapperProcessor = senderWrapperProcessor;
	}

}
