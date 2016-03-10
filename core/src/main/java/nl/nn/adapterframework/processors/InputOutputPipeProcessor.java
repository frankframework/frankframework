/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.CompactSaxHandler;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author Jaco de Groot
 */
public class InputOutputPipeProcessor extends PipeProcessorBase {
	private final static String ME_START = "{sessionKey:";
	private final static String ME_END = "}";

	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, String messageId, Object message, IPipeLineSession pipeLineSession) throws PipeRunException {
		Object preservedObject = message;
		PipeRunResult pipeRunResult = null;
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
			if ((message == null || StringUtils.isEmpty(message.toString()))
					&& StringUtils.isNotEmpty(pe.getEmptyInputReplacement())) {
				if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] replacing empty input for pipe ["+pe.getName()+"] with fixed value ["+pe.getEmptyInputReplacement()+"]");
				message = pe.getEmptyInputReplacement();
			}
		}

		if (pipe instanceof FixedForwardPipe) {
			FixedForwardPipe ffPipe = (FixedForwardPipe) pipe;
			pipeRunResult = ffPipe.doInitialPipe(message, pipeLineSession);
		}
		
		if (pipeRunResult==null){
			pipeRunResult=pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
		}
		if (pipeRunResult==null){
			throw new PipeRunException(pipe, "Pipeline of ["+pipeLine.getOwner().getName()+"] received null result from pipe ["+pipe.getName()+"]d");
		}

		if (pe !=null) {
			if (pe.isRestoreMovedElements()) {
				if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] restoring from compacted result for pipe ["+pe.getName()+"]");
				Object result = pipeRunResult.getResult();
				if (result!=null) {
					String resultString = (String)result;
					pipeRunResult.setResult(restoreMovedElements(resultString, pipeLineSession));
				}
			}
			
			if (pe.getChompCharSize() != null || pe.getElementToMove() != null || pe.getElementToMoveChain() != null) {
				log.debug("Pipeline of adapter ["+owner.getName()+"] compact received message");
				Object result = pipeRunResult.getResult();
				if (result!=null) {
					String resultString = (String)result;
					try {
						InputStream xmlInput = IOUtils.toInputStream(resultString, "UTF-8");
						CompactSaxHandler handler = new CompactSaxHandler();
						handler.setChompCharSize(pe.getChompCharSize());
						handler.setElementToMove(pe.getElementToMove());
						handler.setElementToMoveChain(pe.getElementToMoveChain());
						handler.setElementToMoveSessionKey(pe.getElementToMoveSessionKey());
						handler.setRemoveCompactMsgNamespaces(pe.isRemoveCompactMsgNamespaces());
						handler.setContext(pipeLineSession);
						SAXParserFactory parserFactory = XmlUtils.getSAXParserFactory();
						parserFactory.setNamespaceAware(true);
						SAXParser saxParser = parserFactory.newSAXParser();
						try {
							saxParser.parse(xmlInput, handler);
							resultString = handler.getXmlString();
						} catch (Exception e) {
							log.warn("Pipeline of adapter ["+owner.getName()+"] could not compact received message: " + e.getMessage());
						}
						handler = null;
					} catch (Exception e) {
						throw new PipeRunException(pipe, "Pipeline of ["+pipeLine.getOwner().getName()+"] got error during compacting received message to more compact format: " + e.getMessage());
					}
					pipeRunResult.setResult(resultString);
				}
			}
			
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

	private String restoreMovedElements(String invoerString, IPipeLineSession pipeLineSession) {
		StringBuffer buffer = new StringBuffer();
		int startPos = invoerString.indexOf(ME_START);
		if (startPos == -1)
			return invoerString;
		char[] invoerChars = invoerString.toCharArray();
		int copyFrom = 0;
		while (startPos != -1) {
			buffer.append(invoerChars, copyFrom, startPos - copyFrom);
			int nextStartPos =
				invoerString.indexOf(
					ME_START,
					startPos + ME_START.length());
			if (nextStartPos == -1) {
				nextStartPos = invoerString.length();
			}
			int endPos =
				invoerString.indexOf(ME_END, startPos + ME_START.length());
			if (endPos == -1 || endPos > nextStartPos) {
				log.warn("Found a start delimiter without an end delimiter while restoring from compacted result at position ["
						+ startPos + "] in ["+ invoerString+ "]");
				buffer.append(invoerChars, startPos, nextStartPos - startPos);
				copyFrom = nextStartPos;
			} else {
				String movedElementSessionKey = invoerString.substring(startPos + ME_START.length(),endPos);
				if (pipeLineSession.containsKey(movedElementSessionKey)) {
					String movedElementValue = (String) pipeLineSession.get(movedElementSessionKey);
					buffer.append(movedElementValue);
					copyFrom = endPos + ME_END.length();
				} else {
					log.warn("Did not find sessionKey [" + movedElementSessionKey + "] while restoring from compacted result");
					buffer.append(invoerChars, startPos, nextStartPos - startPos);
					copyFrom = nextStartPos;
				}
			}
			startPos = invoerString.indexOf(ME_START, copyFrom);
		}
		buffer.append(invoerChars, copyFrom, invoerChars.length - copyFrom);
		return buffer.toString();
	}
}
