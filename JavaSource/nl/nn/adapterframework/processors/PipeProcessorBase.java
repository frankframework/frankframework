/*
 * $Log: PipeProcessorBase.java,v $
 * Revision 1.1  2010-09-13 13:45:09  L190409
 * introduced Processor base classes
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Baseclass for PipeProcessors.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
 */
public abstract class PipeProcessorBase implements PipeProcessor {
	protected Logger log = LogUtil.getLogger(this);

	protected PipeProcessor pipeProcessor;

	public void setPipeProcessor(PipeProcessor pipeProcessor) {
		this.pipeProcessor = pipeProcessor;
	}

}
