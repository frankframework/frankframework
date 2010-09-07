/*
 * $Log: InputOutputPipeProcessor.java,v $
 * Revision 1.2  2010-09-07 15:55:13  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class InputOutputPipeProcessor implements PipeProcessor {
	private Logger log = LogUtil.getLogger(this);
	private PipeProcessor pipeProcessor;

	public void setPipeProcessor(PipeProcessor pipeProcessor) {
		this.pipeProcessor = pipeProcessor;
	}

	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe,
			String messageId, Object message, PipeLineSession pipeLineSession
			) throws PipeRunException {
		Object preservedObject = message;
		PipeRunResult pipeRunResult;
		INamedObject owner = pipeLine.getOwner();

		IExtendedPipe pe=null;
			
		if (pipe instanceof IExtendedPipe) {
			pe = (IExtendedPipe)pipe;
		}
		
		if (pe!=null) {
			if (StringUtils.isNotEmpty(pe.getGetInputFromSessionKey())) {
				if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] replacing input for pipe ["+pe.getName()+"] with contents of sessionKey ["+pe.getGetInputFromSessionKey()+"]");
				message=pipeLineSession.get(pe.getGetInputFromSessionKey());
			}
			if (StringUtils.isNotEmpty(pe.getGetInputFromFixedValue())) {
				if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] replacing input for pipe ["+pe.getName()+"] with fixed value ["+pe.getGetInputFromFixedValue()+"]");
				message=pe.getGetInputFromFixedValue();
			}
		}

		pipeRunResult=pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
		if (pipeRunResult==null){
			throw new PipeRunException(pipe, "Pipeline of ["+pipeLine.getOwner().getName()+"] received null result from pipe ["+pipe.getName()+"]d");
		}

		if (pe !=null) {
			if (StringUtils.isNotEmpty(pe.getStoreResultInSessionKey())) {
				if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] storing result for pipe ["+pe.getName()+"] under sessionKey ["+pe.getStoreResultInSessionKey()+"]");
				Object result = pipeRunResult.getResult();
				pipeLineSession.put(pe.getStoreResultInSessionKey(),result);
			}
			if (pe.isPreserveInput()) {
				pipeRunResult.setResult(preservedObject);
			}
		}

		return pipeRunResult;
	}

}
