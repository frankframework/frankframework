/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ICorrelatedPullingListener;
import org.frankframework.core.ISender;
import org.frankframework.core.ITransactionalStorage;
import org.frankframework.core.IValidator;
import org.frankframework.core.IWrapperPipe;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.doc.Mandatory;
import org.frankframework.doc.Reintroduce;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.processors.ListenerProcessor;
import org.frankframework.stream.Message;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;

/**
 * Sends a message using an asynchronous {@link ISender sender} and receives a reply from a {@link ICorrelatedPullingListener listener}.
 * 
 * {@inheritClassDoc}
 */
@Category(Category.Type.BASIC)
@EnterpriseIntegrationPattern(Type.ENDPOINT)
public class AsyncSenderWithListenerPipe<M> extends MessageSendingPipe {
	private @Getter ICorrelatedPullingListener<M> listener = null;
	private @Setter ListenerProcessor<M> listenerProcessor;

	private @Getter String correlationIDStyleSheet;
	private @Getter String correlationIDXPath;
	private @Getter String correlationIDNamespaceDefs;
	private @Getter String correlationIDSessionKey = null;
	private @Getter String labelStyleSheet;
	private @Getter String labelXPath;
	private @Getter String labelNamespaceDefs;
	private @Getter String auditTrailSessionKey = null;
	private @Getter String auditTrailXPath;
	private @Getter String auditTrailNamespaceDefs;
	private @Getter boolean useInputForExtract = true;

	private TransformerPool auditTrailTp = null;
	private TransformerPool correlationIDTp = null;
	private TransformerPool labelTp = null;

	@Override
	public void configure() throws ConfigurationException {
		if (listener == null) {
			// We use the Mandatory annotation, but just in case.
			throw new ConfigurationException("Using a listener is mandatory");
		}

		if (getSender().isSynchronous()) {
			throw new ConfigurationException("cannot have listener with synchronous sender");
		}
		try {
			listener.configure();
		} catch (ConfigurationException e) {
			throw new ConfigurationException("while configuring listener",e);
		}
		if (listener instanceof HasPhysicalDestination dest) {
			log.debug("has listener on {}", dest::getPhysicalDestinationName);
		}

		super.configure();

		if (StringUtils.isNotEmpty(getAuditTrailXPath())) {
			auditTrailTp = TransformerPool.configureTransformer(this, getAuditTrailNamespaceDefs(), getAuditTrailXPath(), null, OutputType.TEXT, false, null);
		}
		if (StringUtils.isNotEmpty(getCorrelationIDXPath()) || StringUtils.isNotEmpty(getCorrelationIDStyleSheet())) {
			correlationIDTp = TransformerPool.configureTransformer(this, getCorrelationIDNamespaceDefs(), getCorrelationIDXPath(), getCorrelationIDStyleSheet(), OutputType.TEXT, false, null);
		}
		if (StringUtils.isNotEmpty(getLabelXPath()) || StringUtils.isNotEmpty(getLabelStyleSheet())) {
			labelTp = TransformerPool.configureTransformer(this, getLabelNamespaceDefs(), getLabelXPath(), getLabelStyleSheet(), OutputType.TEXT, false, null);
		}
	}

	@Override
	protected void propagateName() {
		super.propagateName();

		if (StringUtils.isEmpty(listener.getName())) {
			listener.setName(getName() + "-replylistener");
		}
	}

	@Override
	protected String doLogToMessageLog(final Message input, final PipeLineSession session, final Message originalMessage, final String messageID, String correlationID) throws SenderException {
		try {
			String messageTrail="no audit trail";
			if (auditTrailTp!=null) {
				if (isUseInputForExtract()){
					messageTrail=auditTrailTp.transformToString(originalMessage);
				} else {
					messageTrail=auditTrailTp.transformToString(input);
				}
			} else {
				if (StringUtils.isNotEmpty(getAuditTrailSessionKey())) {
					messageTrail = session.getString(getAuditTrailSessionKey());
				}
			}
			String storedMessageID= messageID;
			if (storedMessageID==null) {
				storedMessageID="-";
			}
			if (correlationIDTp!=null) {
				if (StringUtils.isNotEmpty(getCorrelationIDSessionKey())) {
					String sourceString = session.getString(getCorrelationIDSessionKey());
					correlationID =correlationIDTp.transformToString(sourceString,null);
				} else {
					if (isUseInputForExtract()) {
						correlationID =correlationIDTp.transformToString(originalMessage);
					} else {
						correlationID =correlationIDTp.transformToString(input);
					}
				}
				if (StringUtils.isEmpty(correlationID)) {
					correlationID ="-";
				}
			}
			String label=null;
			if (labelTp!=null) {
				if (isUseInputForExtract()) {
					label=labelTp.transformToString(originalMessage);
				} else {
					label=labelTp.transformToString(input);
				}
			}

			return storeMessage(storedMessageID, correlationID, input, messageTrail, label);
		} catch (TransformerException | IOException | SAXException e) {
			throw new SenderException("unable to apply xml transformation", e);
		}
	}

	@Override
	protected PipeRunResult postSendAction(PipeRunResult sendResult, String correlationID, PipeLineSession session) throws ListenerException, TimeoutException {
		Message result = listenerProcessor.getMessage(getListener(), correlationID, session);
		sendResult.setResult(result);
		return sendResult;
	}

	@Override
	public void start() {
		super.start();

		if (StringUtils.isEmpty(getStubFilename())) {
			listener.start();
		}
	}

	@Override
	public void stop() {
		if (StringUtils.isEmpty(getStubFilename())) {
			try {
				listener.stop();
				log.info("closed listener");
			} catch (LifecycleException e) {
				log.warn("Exception closing listener", e);
			}
		}

		super.stop();
	}

	@Override
	@Reintroduce
	public void setMessageLog(ITransactionalStorage<?> messageLog) {
		super.setMessageLog(messageLog);
	}

	@Override
	@Reintroduce
	public void setInputWrapper(IWrapperPipe inputWrapper) {
		super.setInputWrapper(inputWrapper);
	}

	@Override
	@Reintroduce
	public void setInputValidator(IValidator inputValidator) {
		super.setInputValidator(inputValidator);
	}

	@Override
	@Reintroduce
	public void setSender(ISender sender) {
		super.setSender(sender);
	}

	/** Listener for responses on the request sent. */
	@Mandatory
	public void setListener(ICorrelatedPullingListener<M> listener) {
		this.listener = listener;
		log.debug("pipe [{}] registered listener [{}]", this::getName, listener::toString);
	}

	/** Stylesheet to extract correlationid from message */
	public void setCorrelationIDStyleSheet(String string) {
		correlationIDStyleSheet = string;
	}

	/** XPath expression to extract correlationid from message */
	public void setCorrelationIDXPath(String string) {
		correlationIDXPath = string;
	}

	/** Namespace definitions for correlationIDXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceUri</code> definitions. */
	public void setCorrelationIDNamespaceDefs(String correlationIDNamespaceDefs) {
		this.correlationIDNamespaceDefs = correlationIDNamespaceDefs;
	}

	/** Key of a pipeline session variable. If specified, the value of the PipelineSession variable is used as input for the XPathExpression or stylesheet, instead of the current input message. */
	public void setCorrelationIDSessionKey(String string) {
		correlationIDSessionKey = string;
	}

	/** Stylesheet to extract a label from a message. */
	public void setLabelStyleSheet(String string) {
		labelStyleSheet = string;
	}

	/** XPath expression to extract the label from the message. */
	public void setLabelXPath(String string) {
		labelXPath = string;
	}

	/** Namespace definitions for labelXPath. Must be in the form of a comma or space-separated list of <code>prefix=namespaceUri</code> definitions. */
	public void setLabelNamespaceDefs(String labelXNamespaceDefs) {
		this.labelNamespaceDefs = labelXNamespaceDefs;
	}

	/** XPath expression to extract the audit trail from the message. */
	public void setAuditTrailXPath(String string) {
		auditTrailXPath = string;
	}

	/** Namespace definitions for auditTrailXPath. Must be in the form of a comma or space-separated list of <code>prefix=namespaceUri</code> definitions. */
	public void setAuditTrailNamespaceDefs(String auditTrailNamespaceDefs) {
		this.auditTrailNamespaceDefs = auditTrailNamespaceDefs;
	}

	/** Key of a pipeline session variable. If specified, the value of the PipelineSession variable is used as an audit trail (instead of the default 'no audit trail'). */
	public void setAuditTrailSessionKey(String string) {
		auditTrailSessionKey = string;
	}
	/**
	 * If {@code true}, the input of the Pipe is used to extract the audit trail, correlation ID, and label (instead of the wrapped input).
	 * @ff.default true
	 */
	public void setUseInputForExtract(boolean b) {
		useInputForExtract = b;
	}
}
