/*
   Copyright 2013, 2020 Nationale-Nederlanden

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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineExitHandler;
import nl.nn.adapterframework.core.IPipeLineSession;
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
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, Message message, IPipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		// Object is the object that is passed to and returned from Pipes
		PipeRunResult pipeRunResult;
		// the PipeLineResult
		PipeLineResult pipeLineResult=new PipeLineResult();

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
					String input;
					try {
						input = message.asString();
					} catch (IOException e) {
						throw new PipeRunException(null, "cannot open stream", e);
					}
					PipeLineResult plr = adapter.processMessage(messageId, input, pipeLineSession);
					if (plr == null || !plr.getState().equals("success")) {
						throw new PipeRunException(null, "adapterToRunBefore [" + pipeLine.getAdapterToRunBeforeOnEmptyInput() + "] ended with state [" + (plr==null?"null":plr.getState()) + "]");
					}
					message = new Message(plr.getResult());
					log.debug("input after running adapterBeforeOnEmptyInput [" + message + "]");
				}
			}
		}
		
		// ready indicates wether the pipeline processing is complete
		boolean ready=false;

		// get the first pipe to run
		IPipe pipeToRun = pipeLine.getPipe(pipeLine.getFirstPipe());

		boolean inputValidateError = false;
		IPipe inputValidator = pipeLine.getInputValidator();
		if (inputValidator!=null) {
			log.debug("validating input");
			PipeRunResult validationResult = pipeProcessor.processPipe(pipeLine, inputValidator, message, pipeLineSession);
			if (validationResult!=null) {
				if (!validationResult.getPipeForward().getName().equals("success")) {
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
				Object validatedMessage = validationResult.getResult();
				if (validatedMessage!=null) {
					message=Message.asMessage(validatedMessage);
				}
			}
		}

		if (!inputValidateError) {
			IPipe inputWrapper = pipeLine.getInputWrapper();
			if (inputWrapper!=null) {
				log.debug("wrapping input");
				PipeRunResult wrapResult = pipeProcessor.processPipe(pipeLine, inputWrapper, message, pipeLineSession);
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
					message = Message.asMessage(wrapResult.getResult());
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
				try{
					TransformerPool tpRemoveNamespaces = XmlUtils.getRemoveNamespacesTransformerPool(true,true);
					String xsltResult = tpRemoveNamespaces.transform(message,null);
					pipeLineSession.put("originalMessageWithoutNamespaces", xsltResult);
				} catch (IOException e) {
					throw new PipeRunException(pipeToRun,"cannot retrieve removeNamespaces", e);
				} catch (ConfigurationException ce) {
					throw new PipeRunException(pipeToRun,"got error creating transformer for removeNamespaces", ce);
				} catch (TransformerException te) {
					throw new PipeRunException(pipeToRun,"got error transforming removeNamespaces", te);
				} catch (SAXException se) {
					throw new PipeRunException(pipeToRun,"caught SAXException", se);
				}
			} else {
				log.warn("original message is not well-formed");
				pipeLineSession.put("originalMessageWithoutNamespaces", message);
			}
		}

		boolean outputValidated=false;
		try {
			while (!ready){

				pipeRunResult = pipeProcessor.processPipe(pipeLine, pipeToRun, message, pipeLineSession);
				Object resultObject=pipeRunResult.getResult();
				message = Message.asMessage(resultObject);

				// TODO: this should be moved to a StatisticsPipeProcessor
				if (!(pipeToRun instanceof AbstractPipe)) {
					if (resultObject!=null && resultObject instanceof String) {
						StatisticsKeeper sizeStat = pipeLine.getPipeSizeStatistics(pipeToRun);
						if (sizeStat!=null) {
							sizeStat.addValue(((String)resultObject).length());
						}
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

				PipeLineExit plExit= pipeLine.getPipeLineExits().get(nextPath);
				if (null!=plExit) {
					boolean outputWrapError = false;
					IPipe outputWrapper = pipeLine.getOutputWrapper();
					if (outputWrapper !=null) {
						log.debug("wrapping PipeLineResult");
						PipeRunResult wrapResult = pipeProcessor.processPipe(pipeLine, outputWrapper, message, pipeLineSession);
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
							Object wrappedObject = wrapResult.getResult();
							message = Message.asMessage(wrappedObject);
						}
						log.debug("PipeLineResult after wrapping [" + message.toString() + "]");
					}

					if (!outputWrapError) {
						IPipe outputValidator = pipeLine.getOutputValidator();
						if ((outputValidator !=null) && !outputValidated) {
							outputValidated=true;
							log.debug("validating PipeLineResult");
							PipeRunResult validationResult;
							validationResult = pipeProcessor.processPipe(pipeLine, outputValidator, message, pipeLineSession);
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
								Object validatedObject = validationResult.getResult();
								message = Message.asMessage(validatedObject);
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
							try {
								pipeLineResult.setResult(message.asString());
							} catch (IOException e) {
								throw new PipeRunException(null, "cannot open result stream", e);
							}
						} else {
							pipeLineResult.setResult(null);
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
							log.debug("Pipeline of adapter ["+ pipeLine.getOwner().getName()+ "] finished processing messageId ["+messageId+"] result: ["+ (message.asObject()==null?"<null>":"("+message.asObject().getClass().getSimpleName()+") ["+message.asObject() +"]" )+ "] with exit-state ["+state+"]");
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
