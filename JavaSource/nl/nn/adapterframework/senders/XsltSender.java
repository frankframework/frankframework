/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.senders;

import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Perform an XSLT transformation with a specified stylesheet or XPath-expression.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.senders.XsltSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>stylesheet to apply to the input message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>alternatively: XPath-expression to create stylesheet from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceDefs(String) namespaceDefs}</td><td>namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either 'text' or 'xml'. Only valid for xpathExpression</td><td>text</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) omitXmlDeclaration}</td><td>force the transformer generated from the XPath-expression to omit the xml declaration</td><td>true</td></tr>
 * <tr><td>{@link #setSkipEmptyTags(boolean) skipEmptyTags}</td><td>when set <code>true</code> empty tags in the output are removed</td><td>false</td></tr>
 * <tr><td>{@link #setIndentXml(boolean) indentXml}</td><td>when set <code>true</code>, result is pretty-printed. (only used when <code>skipEmptyTags="true"</code>)</td><td>true</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the sender will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version $Id$
 */
public class XsltSender extends SenderWithParametersBase {

	private String xpathExpression=null;
	private String namespaceDefs = null; 
	private String outputType="text";
	private String styleSheetName;
	private boolean omitXmlDeclaration=true;
	private boolean indentXml=true;
	private boolean skipEmptyTags=false;

	private TransformerPool transformerPool;
	private TransformerPool transformerPoolSkipEmptyTags;

	
	/**
	 * The <code>configure()</code> method instantiates a transformer for the specified
	 * XSL. If the stylesheetname cannot be accessed, a ConfigurationException is thrown.
	 * @throws ConfigurationException
	 */
	public void configure() throws ConfigurationException {
		super.configure();
	
		transformerPool = TransformerPool.configureTransformer(getLogPrefix(), getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), getOutputType(), !isOmitXmlDeclaration(), paramList);
		if (isSkipEmptyTags()) {
			String skipEmptyTags_xslt = XmlUtils.makeSkipEmptyTagsXslt(isOmitXmlDeclaration(),isIndentXml());
			log.debug("test [" + skipEmptyTags_xslt + "]");
			try {
				transformerPoolSkipEmptyTags = new TransformerPool(skipEmptyTags_xslt);
			} catch (TransformerConfigurationException te) {
				throw new ConfigurationException(getLogPrefix() + "got error creating transformer from skipEmptyTags", te);
			}
		}
	}

	public void open() throws SenderException {
		super.open();
		if (transformerPool!=null) {
			try {
				transformerPool.open();
			} catch (Exception e) {
				throw new SenderException(getLogPrefix()+"cannot start TransformerPool", e);
			}
		}
		if (transformerPoolSkipEmptyTags!=null) {
			try {
				transformerPoolSkipEmptyTags.open();
			} catch (Exception e) {
				throw new SenderException(getLogPrefix()+"cannot start TransformerPool SkipEmptyTags", e);
			}
		}
	}

	public void close() throws SenderException {
		super.close();
		if (transformerPool!=null) {
			transformerPool.close();
		}
		if (transformerPoolSkipEmptyTags!=null) {
			transformerPoolSkipEmptyTags.close();
		}
	}
	
	/**
	 * Here the actual transforming is done. Under weblogic the transformer object becomes
	 * corrupt when a not-well formed xml was handled. The transformer is then re-initialized
	 * via the configure() and start() methods.
	 */
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		String stringResult = null;
		if (message==null) {
			throw new SenderException(getLogPrefix()+"got null input");
		}
//		if (log.isDebugEnabled()) {
//			log.debug(getLogPrefix()+" transforming input ["+message+"] using prc ["+prc+"]");
//		}

		try {
			Map parametervalues = null;
			if (paramList!=null) {
				parametervalues = prc.getValueMap(paramList);
			}
//			if (log.isDebugEnabled()) {
//				log.debug(getLogPrefix()+" transformerPool ["+transformerPool+"] transforming using prc ["+prc+"] and parameterValues ["+parametervalues+"]");
//				log.debug(getLogPrefix()+" prc.inputsource ["+prc.getInputSource()+"]");
//			}
			
			stringResult = transformerPool.transform(prc.getInputSource(), parametervalues); 

			if (isSkipEmptyTags()) {
				log.debug(getLogPrefix()+ " skipping empty tags from result [" + stringResult + "]");
				//URL xsltSource = ClassUtils.getResourceURL( this, skipEmptyTags_xslt);
				//Transformer transformer = XmlUtils.createTransformer(xsltSource);
				//stringResult = XmlUtils.transformXml(transformer, stringResult);
				ParameterResolutionContext prc_SkipEmptyTags = new ParameterResolutionContext(stringResult, prc.getSession(), prc.isNamespaceAware()); 
				stringResult = transformerPoolSkipEmptyTags.transform(prc_SkipEmptyTags.getInputSource(), null); 
			}
//			if (log.isDebugEnabled()) {
//				log.debug(getLogPrefix()+" transformed input ["+message+"] to ["+stringResult+"]");
//			}
		} 
		catch (Exception e) {
			log.warn(getLogPrefix()+"intermediate exception logging",e);
			throw new SenderException(getLogPrefix()+" Exception on transforming input", e);
		} 
		return stringResult;
	}

	public boolean isSynchronous() {
		return true;
	}


	/**
	 * Specify the stylesheet to use
	 */
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}
	public String getStyleSheetName() {
		return styleSheetName;
	}

	/**
	 * set the "omit xml declaration" on the transfomer. Defaults to true.
	 * @return true or false
	 */
	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}
	public boolean isOmitXmlDeclaration() {
		return omitXmlDeclaration;
	}


	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}

	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
	public String getNamespaceDefs() {
		return namespaceDefs;
	}

	public void setOutputType(String string) {
		outputType = string;
	}
	public String getOutputType() {
		return outputType;
	}


	public void setSkipEmptyTags(boolean b) {
		skipEmptyTags = b;
	}
	public boolean isSkipEmptyTags() {
		return skipEmptyTags;
	}

	public void setIndentXml(boolean b) {
		indentXml = b;
	}
	public boolean isIndentXml() {
		return indentXml;
	}

}
