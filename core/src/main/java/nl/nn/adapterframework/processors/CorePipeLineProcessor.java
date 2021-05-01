/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
package nl.nn.adapterframework.processors;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineExitHandler;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * @author Jaco de Groot
 */
public class CorePipeLineProcessor implements PipeLineProcessor {
	private Logger log = LogUtil.getLogger(this);
	private PipeProcessor pipeProcessor;

	@Override
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, Message message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {

		if (message.isEmpty()) {
			if (StringUtils.isNotEmpty(pipeLine.getAdapterToRunBeforeOnEmptyInput())) {
				log.debug("running adapterBeforeOnEmptyInput");
				IAdapter adapter = pipeLine
						.getAdapter()
						.getConfiguration()
						.getIbisManager()
						.getRegisteredAdapter(pipeLine.getAdapterToRunBeforeOnEmptyInput());
				if (adapter == null) {
					log.warn("adapterToRunBefore with specified name [" + pipeLine.getAdapterToRunBeforeOnEmptyInput() + "] could not be retrieved");
				} else {
					PipeLineResult plr = adapter.processMessage(messageId, message, pipeLineSession);
					if (plr == null || !plr.isSuccessful()) {
						throw new PipeRunException(null, "adapterToRunBefore [" + pipeLine.getAdapterToRunBeforeOnEmptyInput() + "] ended with state [" + (plr==null?"null":plr.getState()) + "]");
					}
					message = plr.getResult();
					log.debug("input after running adapterBeforeOnEmptyInput [" + message + "]");
				}
			}
		}
		
		// ready indicates whether the pipeline processing is complete
		boolean ready=false;

		// get the first pipe to run
		IForwardTarget forwardTarget = pipeLine.getPipe(pipeLine.getFirstPipe());

		boolean inputValidateError = false;
		IPipe inputValidator = pipeLine.getInputValidator();
		if (inputValidator!=null) {
			log.debug("validating input");
			PipeRunResult validationResult = pipeProcessor.processPipe(pipeLine, inputValidator, message, pipeLineSession);
			if (validationResult!=null) {
				if (!validationResult.getPipeForward().getName().equals("success")) {
					forwardTarget = pipeLine.resolveForward(inputValidator, validationResult.getPipeForward());
					log.warn("forwarding execution flow to ["+forwardTarget.getName()+"] due to validation fault");
					inputValidateError = true;
				}
				Message validatedMessage = validationResult.getResult();
				if (!validatedMessage.isEmpty()) {
					message=validatedMessage;
				}
			}
		}

		if (!inputValidateError) {
			IPipe inputWrapper = pipeLine.getInputWrapper();
			if (inputWrapper!=null) {
				log.debug("wrapping input");
				PipeRunResult wrapResult = pipeProcessor.processPipe(pipeLine, inputWrapper, message, pipeLineSession);
				if (wrapResult!=null && !wrapResult.getPipeForward().getName().equals("success")) {
					forwardTarget = pipeLine.resolveForward(inputWrapper, wrapResult.getPipeForward());
					log.warn("forwarding execution flow to ["+forwardTarget.getName()+"] due to wrap fault");
				} else {
					message = wrapResult.getResult();
				}
				log.debug("input after wrapping [" + message + "]");
			}
		}

		if (message.asObject() instanceof String) {
			pipeLine.getRequestSizeStats().addValue(((String)message.asObject()).length());
		}
		
		if (pipeLine.isStoreOriginalMessageWithoutNamespaces()) {
			String input;
			try {
				input = message.asString();
			} catch (IOException e) {
				throw new PipeRunException(null, "cannot open stream", e);
			}
			if (XmlUtils.isWellFormed(input)) {
				IPipe pipe = forwardTarget instanceof IPipe ? (IPipe)forwardTarget : null;
				try{
					TransformerPool tpRemoveNamespaces = XmlUtils.getRemoveNamespacesTransformerPool(true,true);
					String xsltResult = tpRemoveNamespaces.transform(message,null);
					pipeLineSession.put("originalMessageWithoutNamespaces", xsltResult);
				} catch (IOException e) {
					throw new PipeRunException(pipe,"cannot retrieve removeNamespaces", e);
				} catch (ConfigurationException ce) {
					throw new PipeRunException(pipe,"got error creating transformer for removeNamespaces", ce);
				} catch (TransformerException te) {
					throw new PipeRunException(pipe,"got error transforming removeNamespaces", te);
				} catch (SAXException se) {
					throw new PipeRunException(pipe,"caught SAXException", se);
				}
			} else {
				log.warn("original message is not well-formed");
				pipeLineSession.put("originalMessageWithoutNamespaces", message);
			}
		}

		PipeLineResult pipeLineResult=new PipeLineResult();
		boolean outputValidationFailed=false;
		try {
			while (!ready){

				if (forwardTarget instanceof PipeLineExit) {
					PipeLineExit plExit= (PipeLineExit)forwardTarget;
					if(!plExit.getEmptyResult()) {
						boolean outputWrapError = false;
						IPipe outputWrapper = pipeLine.getOutputWrapper();
						if (outputWrapper !=null) {
							log.debug("wrapping PipeLineResult");
							PipeRunResult wrapResult = pipeProcessor.processPipe(pipeLine, outputWrapper, message, pipeLineSession);
							if (wrapResult!=null && !wrapResult.getPipeForward().getName().equals("success")) {
								forwardTarget = pipeLine.resolveForward(outputWrapper, wrapResult.getPipeForward());
								log.warn("forwarding execution flow to ["+forwardTarget.getName()+"] due to wrap fault");
								outputWrapError = true;
							} else {
								log.debug("wrap succeeded");
								message = wrapResult.getResult();
							}
							if(log.isDebugEnabled()) log.debug("PipeLineResult after wrapping: " + (message==null?"<null>":"("+message.getClass().getSimpleName()+") ["+message +"]" ));
						}

						if (!outputWrapError) {
							IPipe outputValidator = pipeLine.getOutputValidator();
							if (outputValidator != null) {
								if (outputValidationFailed) {
									log.debug("validating error message after PipeLineResult validation failed");
								} else {
									log.debug("validating PipeLineResult");
								}
								PipeRunResult validationResult = pipeProcessor.processPipe(pipeLine, outputValidator, message, pipeLineSession);
								if (!validationResult.getPipeForward().getName().equals("success")) {
									if (!outputValidationFailed) {
										outputValidationFailed=true;
										forwardTarget = pipeLine.resolveForward(outputValidator, validationResult.getPipeForward());
										log.warn("forwarding execution flow to ["+forwardTarget.getName()+"] due to validation fault");
									} else {
										log.warn("validation of error message by validator ["+outputValidator.getName()+"] failed, returning result anyhow"); // to avoid endless looping
										message = validationResult.getResult();
										ready=true;
									}
								} else {
									log.debug("validation succeeded");
									message = validationResult.getResult();
									ready=true;
								}
							} else {
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
						pipeLineResult.setExitCode(plExit.getExitCode());
						if (message.asObject()!=null && !plExit.getEmptyResult()) {
							pipeLineResult.setResult(message);
						} else {
							pipeLineResult.setResult(Message.nullMessage());
						}
						ready=true;
						if (log.isDebugEnabled()){  // for performance reasons
							String skString = "";
							for (Iterator<String> it = pipeLineSession.keySet().iterator(); it.hasNext();) {
								String key = it.next();
								Object value = pipeLineSession.get(key);
								skString = skString + "\n " + key + "=[" + value + "]";
							}
							log.debug("Available session keys at finishing pipeline of adapter [" + pipeLine.getOwner().getName() + "]:" + skString);
							log.debug("Pipeline of adapter ["+ pipeLine.getOwner().getName()+ "] finished processing messageId ["+messageId+"] result: " + (message==null?"<null>":"("+message.getClass().getSimpleName()+") ["+message +"]" ) + " with exit-state ["+state+"]");
						}
					}
				} else {
					IPipe pipeToRun=(IPipe)forwardTarget;
					PipeRunResult pipeRunResult = pipeProcessor.processPipe(pipeLine, pipeToRun, message, pipeLineSession);
					message=pipeRunResult.getResult();
	
					// TODO: this should be moved to a StatisticsPipeProcessor
					if (!(pipeToRun instanceof AbstractPipe) && !message.isEmpty()) {
						StatisticsKeeper sizeStat = pipeLine.getPipeSizeStatistics(pipeToRun);
						if (sizeStat!=null) {
							sizeStat.addValue(message.size());
						}
					}

					PipeForward pipeForward=pipeRunResult.getPipeForward();
					// get the next pipe to run
					forwardTarget = pipeLine.resolveForward(pipeToRun, pipeForward);

				}
			}
		} finally {
			for (int i=0; i<pipeLine.getExitHandlers().size(); i++) {
				IPipeLineExitHandler exitHandler = pipeLine.getExitHandlers().get(i);
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
	
	
	public void setPipeProcessor(PipeProcessor pipeProcessor) {
		this.pipeProcessor = pipeProcessor;
	}

}
