/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.io.StringWriter;

import nl.nn.adapterframework.doc.IbisDocRef;
import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.XsltSender;
import nl.nn.adapterframework.stream.IThreadCreator;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;


/**
 * Perform an XSLT transformation with a specified stylesheet.
 *
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link Parameter param}</td><td>any parameters defined on the pipe will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 */

public class XsltPipe extends StreamingPipe implements IThreadCreator {

	private String sessionKey=null;
	
	private XsltSender sender = createXsltSender();
	
	private final String XSLTSENDER = "nl.nn.adapterframework.senders.XsltSender";

	{
		setSizeStatistics(true);
	}
	
	
	protected XsltSender createXsltSender() {
		return new XsltSender();
	}
	
	/**
	 * The <code>configure()</code> method instantiates a transformer for the specified
	 * XSL. If the stylesheetname cannot be accessed, a ConfigurationException is thrown.
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		sender.setName(getName());
		sender.configure();
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		try {
			sender.open();
		} catch (SenderException e) {
			throw new PipeStartException(e);
		}
	}
	
	@Override
	public void stop() {
		try {
			sender.close();
		} catch (SenderException e) {
			log.warn(getLogPrefix(null)+"exception closing XsltSender",e);
		}
		super.stop();
	}
	
	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session, MessageOutputStream target) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this, getLogPrefix(session)+"got null input");
		}
		Message message = new Message(input);
		ParameterResolutionContext prc = new ParameterResolutionContext(message, session, isNamespaceAware()); 
		try {
			Object result = sender.sendMessage(null, message, prc, target);
			if (result instanceof StringWriter) {
				result = result.toString();
			}

			if (StringUtils.isEmpty(getSessionKey())) {
				return new PipeRunResult(getForward(), result);
			}
			session.put(getSessionKey(), result);
			return new PipeRunResult(getForward(), input);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + " Exception on transforming input", e);
		}
	}

	@Override
	public boolean canProvideOutputStream() {
		return super.canProvideOutputStream() && sender.canProvideOutputStream();
	}

	@Override
	public boolean requiresOutputStream() {
		return super.requiresOutputStream() && sender.requiresOutputStream();
	}

	@Override
	public boolean supportsOutputStreamPassThrough() {
		return false;
	}


	@Override
	public MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		return sender.provideOutputStream(correlationID, session, target);
	}

	@Override
	public ParameterList getParameterList() {
		return sender.getParameterList();
	}

	@Override
	public void addParameter(Parameter rhs) {
		sender.addParameter(rhs);
	}



	@IbisDocRef({"1", XSLTSENDER})
	public void setStyleSheetName(String stylesheetName) {
		sender.setStyleSheetName(stylesheetName);
	}

	@IbisDocRef({"2", XSLTSENDER})
	public void setStyleSheetNameSessionKey(String newSessionKey) {
		sender.setStyleSheetNameSessionKey(newSessionKey);
	}

	@IbisDocRef({"3", XSLTSENDER})
	public void setStyleSheetCacheSize(int size) {
		sender.setStyleSheetCacheSize(size);
	}

	@IbisDocRef({"4", XSLTSENDER})
	public void setXpathExpression(String string) {
		sender.setXpathExpression(string);
	}
	public String getXpathExpression() {
		return sender.getXpathExpression();
	}

	@IbisDocRef({"5", XSLTSENDER})
	public void setOmitXmlDeclaration(boolean b) {
		sender.setOmitXmlDeclaration(b);
	}
	
	@IbisDocRef({"6", XSLTSENDER})
	public void setNamespaceDefs(String namespaceDefs) {
		sender.setNamespaceDefs(namespaceDefs);
	}
	public String getNamespaceDefs() {
		return sender.getNamespaceDefs();
	}

	@IbisDocRef({"7", XSLTSENDER})
	public void setOutputType(String string) {
		sender.setOutputType(string);
	}

	@IbisDocRef({"8", XSLTSENDER})
	public void setIndentXml(boolean b) {
		sender.setIndentXml(b);
	}

	@IbisDocRef({"9", XSLTSENDER})
	public void setRemoveNamespaces(boolean b) {
		sender.setRemoveNamespaces(b);
	}

	@IbisDocRef({"10", XSLTSENDER})
	public void setSkipEmptyTags(boolean b) {
		sender.setSkipEmptyTags(b);
	}

	@IbisDocRef({"11", XSLTSENDER})
	public void setXsltVersion(int xsltVersion) {
		sender.setXsltVersion(xsltVersion);
	}
	@IbisDocRef({"12", XSLTSENDER})
	/**
	 * @deprecated Please remove setting of xslt2, it will be auto detected. Or use xsltVersion.
	 */
	@Deprecated
	public void setXslt2(boolean b) {
		sender.setXslt2(b);
	}
	
	@IbisDocRef({"14", XSLTSENDER})
	@Override
	public void setNamespaceAware(boolean b) {
		sender.setNamespaceAware(b);
	}
	@Override
	public boolean isNamespaceAware() {
		return sender.isNamespaceAware();
	}

	@IbisDocRef({"15", XSLTSENDER})
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}
	public String getSessionKey() {
		return sessionKey;
	}

	@Override
	public void setName(String name) {
		super.setName(name);
		sender.setName("Sender of Pipe ["+name+"]");
	}

	@Override
	public void setThreadLifeCycleEventListener(ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener) {
		sender.setThreadLifeCycleEventListener(threadLifeCycleEventListener);
	}

	protected XsltSender getSender() {
		return sender;
	}

}
