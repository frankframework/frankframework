/*
 * $Log: CorePipeLineProcessor.java,v $
 * Revision 1.9  2012-05-04 09:42:35  m00f069
 * Use PipeProcessors (to e.g. handle statistics) for Validators and Wrappers
 *
 * Revision 1.8  2012/03/05 14:46:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * changed order of validate and wrap
 *
 * Revision 1.7  2012/01/25 15:52:14  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * skip output wrap when output validation fails
 *
 * Revision 1.6  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/11/25 11:28:17  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added inputWrapper and outputWrapper pipes to PipeLine and MessageSendingPipes
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2011/08/22 14:28:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for size statistics
 *
 * Revision 1.3  2011/08/18 14:39:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added validator statistics
 *
 * Revision 1.2  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;
import java.io.IOException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineExitHandler;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class CorePipeLineProcessor implements PipeLineProcessor {
	private Logger log = LogUtil.getLogger(this);
	private PipeProcessor pipeProcessor;

	public void setPipeProcessor(PipeProcessor pipeProcessor) {
		this.pipeProcessor = pipeProcessor;
	}
	
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, String message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		// Object is the object that is passed to and returned from Pipes
		Object object = (Object) message;
		PipeRunResult pipeRunResult;
		// the PipeLineResult 
		PipeLineResult pipeLineResult=new PipeLineResult();   
		
		// ready indicates wether the pipeline processing is complete
		boolean ready=false;

		// get the first pipe to run
		IPipe pipeToRun = pipeLine.getPipe(pipeLine.getFirstPipe());

		boolean inputValidateError = false;
		IPipe inputValidator = pipeLine.getInputValidator();
		if (inputValidator!=null) {
			log.debug("validating input");
			PipeRunResult validationResult = pipeProcessor.processPipe(pipeLine, inputValidator, messageId, message, pipeLineSession);
			if (validationResult!=null && !validationResult.getPipeForward().getName().equals("success")) {
				PipeForward validationForward=validationResult.getPipeForward();
				if (validationForward.getPath()==null) {
					throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"] of inputValidator has emtpy forward path");
				}	
				log.warn("setting first pipe to ["+validationForward.getPath()+"] due to validation fault");
				inputValidateError = true;
				pipeToRun = pipeLine.getPipe(validationForward.getPath());
				if (pipeToRun==null) {
					throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"], path ["+validationForward.getPath()+"] does not correspond to a pipe");
				}
			}
		}

		if (!inputValidateError) {
			IPipe inputWrapper = pipeLine.getInputWrapper();
			if (inputWrapper!=null) {
				log.debug("wrapping input");
				PipeRunResult wrapResult = pipeProcessor.processPipe(pipeLine, inputWrapper, messageId, message, pipeLineSession);
				if (wrapResult!=null && !wrapResult.getPipeForward().getName().equals("success")) {
					PipeForward wrapForward=wrapResult.getPipeForward();
					if (wrapForward.getPath()==null) {
						throw new PipeRunException(pipeToRun,"forward ["+wrapForward.getName()+"] of inputWrapper has emtpy forward path");
					}	
					log.warn("setting first pipe to ["+wrapForward.getPath()+"] due to wrap fault");
					pipeToRun = pipeLine.getPipe(wrapForward.getPath());
					if (pipeToRun==null) {
						throw new PipeRunException(pipeToRun,"forward ["+wrapForward.getName()+"], path ["+wrapForward.getPath()+"] does not correspond to a pipe");
					}
				} else {
					message = wrapResult.getResult().toString();
				}
				log.debug("input after wrapping [" + message + "]");
				object = (Object) message;
			}
		}

		pipeLine.getRequestSizeStats().addValue(message.length());
		
		if (pipeLine.isStoreOriginalMessageWithoutNamespaces()) {
			if (XmlUtils.isWellFormed(message)) {
				String removeNamespaces_xslt = XmlUtils.makeRemoveNamespacesXslt(true,true);
				try{
					String xsltResult = null;
					Transformer transformer = XmlUtils.createTransformer(removeNamespaces_xslt);
					xsltResult = XmlUtils.transformXml(transformer, message);
					pipeLineSession.put("originalMessageWithoutNamespaces", xsltResult);
				} catch (IOException e) {
					throw new PipeRunException(pipeToRun,"cannot retrieve removeNamespaces", e);
				} catch (TransformerConfigurationException te) {
					throw new PipeRunException(pipeToRun,"got error creating transformer from removeNamespaces", te);
				} catch (TransformerException te) {
					throw new PipeRunException(pipeToRun,"got error transforming removeNamespaces", te);
				} catch (DomBuilderException te) {
					throw new PipeRunException(pipeToRun,"caught DomBuilderException", te);
				}
			} else {
				log.warn("original message is not well-formed");
				pipeLineSession.put("originalMessageWithoutNamespaces", message);
			}
		}
	
		boolean outputValidated=false;
		try {    
			while (!ready){
				
				pipeRunResult = pipeProcessor.processPipe(pipeLine, pipeToRun, messageId, object, pipeLineSession);
				object=pipeRunResult.getResult();
	
				if (object!=null && object instanceof String) {
					StatisticsKeeper sizeStat = pipeLine.getPipeSizeStatistics(pipeToRun);
					if (sizeStat!=null) {
						sizeStat.addValue(((String)object).length());
					}
				}
				
				PipeForward pipeForward=pipeRunResult.getPipeForward();
	
	                
				if (pipeForward==null){
					throw new PipeRunException(pipeToRun, "Pipeline of ["+pipeLine.getOwner().getName()+"] received result from pipe ["+pipeToRun.getName()+"] without a pipeForward");
				}
				// get the next pipe to run
				String nextPath=pipeForward.getPath();
				if ((null==nextPath) || (nextPath.length()==0)){
					throw new PipeRunException(pipeToRun, "Pipeline of ["+pipeLine.getOwner().getName()+"] got an path that equals null or has a zero-length value from pipe ["+pipeToRun.getName()+"]. Check the configuration, probably forwards are not defined for this pipe.");
				}
	
				PipeLineExit plExit=(PipeLineExit)pipeLine.getPipeLineExits().get(nextPath);
				if (null!=plExit){
					boolean outputWrapError = false;
					IPipe outputWrapper = pipeLine.getOutputWrapper();
					if (outputWrapper !=null) {
						log.debug("wrapping PipeLineResult");
						PipeRunResult wrapResult = pipeProcessor.processPipe(pipeLine, outputWrapper, messageId, object, pipeLineSession);
						if (wrapResult!=null && !wrapResult.getPipeForward().getName().equals("success")) {
							PipeForward wrapForward=wrapResult.getPipeForward();
							if (wrapForward.getPath()==null) {
								throw new PipeRunException(pipeToRun,"forward ["+wrapForward.getName()+"] of outputWrapper has emtpy forward path");
							}	
							log.warn("setting next pipe to ["+wrapForward.getPath()+"] due to wrap fault");
							outputWrapError = true;
							pipeToRun = pipeLine.getPipe(wrapForward.getPath());
							if (pipeToRun==null) {
								throw new PipeRunException(pipeToRun,"forward ["+wrapForward.getName()+"], path ["+wrapForward.getPath()+"] does not correspond to a pipe");
							}
						} else {
							log.debug("wrap succeeded");
							object = wrapResult.getResult();
						}
						log.debug("PipeLineResult after wrapping [" + object.toString() + "]");
					}

					if (!outputWrapError) {
						IPipe outputValidator = pipeLine.getOutputValidator();
						if (outputValidator !=null && !outputValidated) {
							outputValidated=true;
							log.debug("validating PipeLineResult");
							PipeRunResult validationResult = pipeProcessor.processPipe(pipeLine, outputValidator, messageId, object, pipeLineSession);
							if (validationResult!=null && !validationResult.getPipeForward().getName().equals("success")) {
								PipeForward validationForward=validationResult.getPipeForward();
								if (validationForward.getPath()==null) {
									throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"] of outputValidator has emtpy forward path");
								}	
								log.warn("setting next pipe to ["+validationForward.getPath()+"] due to validation fault");
								pipeToRun = pipeLine.getPipe(validationForward.getPath());
								if (pipeToRun==null) {
									throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"], path ["+validationForward.getPath()+"] does not correspond to a pipe");
								}
							} else {
								log.debug("validation succeeded");
								ready=true;
							}
						} else {
							ready=true;
						}
					} else {
						ready=true;
					}
					if (ready) {
						String state=plExit.getState();
						pipeLineResult.setState(state);
						if (object!=null) {
							pipeLineResult.setResult(object.toString());
						} else { 
							pipeLineResult.setResult(null);
						}
						ready=true;
						if (log.isDebugEnabled()){  // for performance reasons
							log.debug("Pipeline of adapter ["+ pipeLine.getOwner().getName()+ "] finished processing messageId ["+messageId+"] result: ["+ object+ "] with exit-state ["+state+"]");
						}
					}
				} else {
					pipeToRun=pipeLine.getPipe(pipeForward.getPath());
					if (pipeToRun==null) {
						throw new PipeRunException(null, "Pipeline of adapter ["+ pipeLine.getOwner().getName()+"] got an erroneous definition. Pipe to execute ["+pipeForward.getPath()+ "] is not defined.");
					}
				}
			}
		} finally {
			for (int i=0; i<pipeLine.getExitHandlers().size(); i++) {
				IPipeLineExitHandler exitHandler = (IPipeLineExitHandler)pipeLine.getExitHandlers().get(i);
				try {
					if (log.isDebugEnabled()) log.debug("processing ExitHandler ["+exitHandler.getName()+"]");
					exitHandler.atEndOfPipeLine(messageId,pipeLineResult,pipeLineSession);
				} catch (Throwable t) {
					log.warn("Caught Exception processing ExitHandler ["+exitHandler.getName()+"]",t);
				}
			}
		}
		return pipeLineResult;
	}

}
