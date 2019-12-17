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

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
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
	public boolean canStreamToTarget() {
		return super.canStreamToTarget() && sender.canStreamToTarget();
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



	@IbisDoc({"1", "Location of stylesheet to apply to the input message", ""})
	public void setStyleSheetName(String stylesheetName) {
		sender.setStyleSheetName(stylesheetName);
	}

	@IbisDoc({"2", "Session key to retrieve stylesheet location. Overrides stylesheetName or xpathExpression attribute", ""})
	public void setStyleSheetNameSessionKey(String newSessionKey) {
		sender.setStyleSheetNameSessionKey(newSessionKey);
	}

	@IbisDoc({"3", "Size of cache of stylesheets retrieved from styleSheetNameSessionKey", "100"})
	public void setStyleSheetCacheSize(int size) {
		sender.setStyleSheetCacheSize(size);
	}
	
	@IbisDoc({"4", "xpath-expression to apply to the input message. it's possible to refer to a parameter (which e.g. contains a value from a sessionkey) by using the parameter name prefixed with $", ""})
	public void setXpathExpression(String string) {
		sender.setXpathExpression(string);
	}
	public String getXpathExpression() {
		return sender.getXpathExpression();
	}

	@IbisDoc({"5", "force the transformer generated from the xpath-expression to omit the xml declaration", "true"})
	public void setOmitXmlDeclaration(boolean b) {
		sender.setOmitXmlDeclaration(b);
	}
	
	@IbisDoc({"6", "namespace defintions for xpathexpression. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setNamespaceDefs(String namespaceDefs) {
		sender.setNamespaceDefs(namespaceDefs);
	}
	public String getNamespaceDefs() {
		return sender.getNamespaceDefs();
	}

	@IbisDoc({"7", "either 'text' or 'xml'. only valid for xpathexpression", "text"})
	public void setOutputType(String string) {
		sender.setOutputType(string);
	}

	@IbisDoc({"8", "when set <code>true</code>, result is pretty-printed. (only used when <code>skipemptytags=true</code>)", "true"})
	public void setIndentXml(boolean b) {
		sender.setIndentXml(b);
	}

	@IbisDoc({"9", "when set <code>true</code> namespaces (and prefixes) in the input message are removed", "false"})
	public void setRemoveNamespaces(boolean b) {
		sender.setRemoveNamespaces(b);
	}

	@IbisDoc({"10", "when set <code>true</code> empty tags in the output are removed", "false"})
	public void setSkipEmptyTags(boolean b) {
		sender.setSkipEmptyTags(b);
	}

	@IbisDoc({"11", "when set to <code>2</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan). <code>0</code> will auto detect", "0"})
	public void setXsltVersion(int xsltVersion) {
		sender.setXsltVersion(xsltVersion);
	}
	@IbisDoc({"12", "Deprecated: when set <code>true</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan)", "false"})
	/**
	 * @deprecated Please remove setting of xslt2, it will be auto detected. Or use xsltVersion.
	 */
	@Deprecated
	public void setXslt2(boolean b) {
		sender.setXslt2(b);
	}
	
	@IbisDoc({"14", "controls namespace-awareness of transformation", "application default"})
	@Override
	public void setNamespaceAware(boolean b) {
		sender.setNamespaceAware(b);
	}
	@Override
	public boolean isNamespaceAware() {
		return sender.isNamespaceAware();
	}

	@IbisDoc({"15", "Sets the name of the key in the <code>PipeLineSession</code> to store the input in", ""})
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
