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
package nl.nn.adapterframework.pipes;

import javax.xml.transform.TransformerException;

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


/**
 * Perform an XSLT transformation with a specified stylesheet.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XsltPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(java.lang.Object, nl.nn.adapterframework.core.IPipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of transformation</td><td>application default</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>stylesheet to apply to the input message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>XPath-expression to apply to the input message. It's possible to refer to a parameter (which e.g. contains a value from a sessionKey) by using the parameter name prefixed with $</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceDefs(String) namespaceDefs}</td><td>namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either 'text' or 'xml'. Only valid for xpathExpression</td><td>text</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) omitXmlDeclaration}</td><td>force the transformer generated from the XPath-expression to omit the xml declaration</td><td>true</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>If specified, the result is put 
 * in the PipeLineSession under the specified key, and the result of this pipe will be 
 * the same as the input (the xml). If NOT specified, the result of the xpath expression 
 * will be the result of this pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSkipEmptyTags(boolean) skipEmptyTags}</td><td>when set <code>true</code> empty tags in the output are removed</td><td>false</td></tr>
 * <tr><td>{@link #setIndentXml(boolean) indentXml}</td><td>when set <code>true</code>, result is pretty-printed. (only used when <code>skipEmptyTags="true"</code>)</td><td>true</td></tr>
 * <tr><td>{@link #setRemoveNamespaces(boolean) removeNamespaces}</td><td>when set <code>true</code> namespaces (and prefixes) in the input message are removed</td><td>false</td></tr>
 * <tr><td>{@link #setXslt2(boolean) xslt2}</td><td>when set <code>true</code> XSLT processor 2.0 (net.sf.saxon) will be used, otherwise XSLT processor 1.0 (org.apache.xalan)</td><td>false</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 */

public class XsltPipe extends FixedForwardPipe {

	private String sessionKey=null;
	
	private XsltSender sender = new XsltSender();

	{
		setSizeStatistics(true);
	}
	
	/**
	 * The <code>configure()</code> method instantiates a transformer for the specified
	 * XSL. If the stylesheetname cannot be accessed, a ConfigurationException is thrown.
	 * @throws ConfigurationException
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
	
//	protected ParameterResolutionContext getInput(String input, IPipeLineSession session) throws PipeRunException, DomBuilderException, TransformerException, IOException {
//		if (isRemoveNamespaces()) {
//			log.debug(getLogPrefix(session)+ " removing namespaces from input message");
//			ParameterResolutionContext prc_RemoveNamespaces = new ParameterResolutionContext(input, session, isNamespaceAware()); 
//			input = transformerPoolRemoveNamespaces.transform(prc_RemoveNamespaces.getInputSource(), null); 
//			log.debug(getLogPrefix(session)+ " output message after removing namespaces [" + input + "]");
//		}
//		return new ParameterResolutionContext(input, session, isNamespaceAware());
//	}

	/**
	 * @param input  
	 * @param session  
	 */
	protected String getInputXml(Object input, IPipeLineSession session) throws TransformerException {
		return (String)input;
	}
	
//	/*
//	 * Allow to override transformation, so JsonXslt can prefix and suffix...
//	 */
//	protected String transform(TransformerPool tp, Source source, Map<String,Object> parametervalues) throws TransformerException, IOException {
//		return tp.transform(source, parametervalues);
//	}
	/*
	 * Allow to override transformation, so JsonXslt can prefix and suffix...
	 */
	protected String transform(Object input, IPipeLineSession session) throws SenderException, TransformerException {
 	    String inputXml=getInputXml(input, session);
		ParameterResolutionContext prc = new ParameterResolutionContext(inputXml, session, isNamespaceAware()); 
		return sender.sendMessage(null, inputXml, prc);
	}
	/**
	 * Here the actual transforming is done. Under weblogic the transformer object becomes
	 * corrupt when a not-well formed xml was handled. The transformer is then re-initialized
	 * via the configure() and start() methods.
	 */
	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this, getLogPrefix(session)+"got null input");
		}
 	    if (!(input instanceof String)) {
	        throw new PipeRunException(this, getLogPrefix(session)+"got an invalid type as input, expected String, got " + input.getClass().getName());
	    }

	    try {
	    	String stringResult = transform(input, session);
		
			if (StringUtils.isEmpty(getSessionKey())){
				return new PipeRunResult(getForward(), stringResult);
			}
			session.put(getSessionKey(), stringResult);
			return new PipeRunResult(getForward(), input);
	    } 
	    catch (Exception e) {
	        throw new PipeRunException(this, getLogPrefix(session)+" Exception on transforming input", e);
	    } 
	}

	/**
	 * Specify the stylesheet to use
	 */
	public void setStyleSheetName(String stylesheetName) {
		sender.setStyleSheetName(stylesheetName);
	}
	public String getStyleSheetName() {
		return sender.getStyleSheetName();
	}

	/**
	 * set the "omit xml declaration" on the transfomer. Defaults to true.
	 */
	public void setOmitXmlDeclaration(boolean b) {
		sender.setOmitXmlDeclaration(b);
	}
	public boolean isOmitXmlDeclaration() {
		return sender.isOmitXmlDeclaration();
	}


	public void setXpathExpression(String string) {
		sender.setXpathExpression(string);
	}
	public String getXpathExpression() {
		return sender.getXpathExpression();
	}

	public void setNamespaceDefs(String namespaceDefs) {
		sender.setNamespaceDefs(namespaceDefs);
	}
	public String getNamespaceDefs() {
		return sender.getNamespaceDefs();
	}

	/**
	 * The name of the key in the <code>PipeLineSession</code> to store the input in
	 * @see nl.nn.adapterframework.core.IPipeLineSession
	 */
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}
	public String getSessionKey() {
		return sessionKey;
	}


	public void setOutputType(String string) {
		sender.setOutputType(string);
	}
	public String getOutputType() {
		return sender.getOutputType();
	}


	public void setSkipEmptyTags(boolean b) {
		sender.setSkipEmptyTags(b);
	}
	public boolean isSkipEmptyTags() {
		return sender.isSkipEmptyTags();
	}

	public void setIndentXml(boolean b) {
		sender.setIndentXml(b);
	}
	public boolean isIndentXml() {
		return sender.isIndentXml();
	}

	public void setRemoveNamespaces(boolean b) {
		sender.setRemoveNamespaces(b);
	}
	public boolean isRemoveNamespaces() {
		return sender.isRemoveNamespaces();
	}

	@Override
	public void setNamespaceAware(boolean b) {
		sender.setNamespaceAware(b);
	}
	@Override
	public boolean isNamespaceAware() {
		return sender.isNamespaceAware();
	}

	public boolean isXslt2() {
		return sender.isXslt2();
	}

	public void setXslt2(boolean b) {
		sender.setXslt2(b);
	}

	@Override
	public ParameterList getParameterList() {
		return sender.getParameterList();
	}

	@Override
	public void addParameter(Parameter rhs) {
		sender.addParameter(rhs);
	}
}
