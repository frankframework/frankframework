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
package nl.nn.adapterframework.senders;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Perform an XSLT transformation with a specified stylesheet or XPath-expression.
 *
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link Parameter param}</td><td>any parameters defined on the sender will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class XsltSender extends SenderWithParametersBase {

	private String xpathExpression=null;
	private String namespaceDefs = null; 
	private String outputType="text";
	private String styleSheetName;
	private boolean omitXmlDeclaration=true;
	private boolean indentXml=true;
	private boolean skipEmptyTags=false;
	private boolean removeNamespaces=false;
	private int xsltVersion=0; // set to 0 for auto detect.
	private boolean namespaceAware=XmlUtils.isNamespaceAwareByDefault();

	private TransformerPool transformerPoolSkipEmptyTags;
	private TransformerPool transformerPoolRemoveNamespaces;
	
	private LRUMap transformerPoolMap;
	private int transformerPoolMapSize = 100;
	
	private String defaultStyleSheetName;
	private String styleSheetNameSessionKey=null;

	/**
	 * The <code>configure()</code> method instantiates a transformer for the specified
	 * XSL. If the stylesheetname cannot be accessed, a ConfigurationException is thrown.
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		
		transformerPoolMap = new LRUMap(transformerPoolMapSize);
		
		defaultStyleSheetName = getStyleSheetName();
		transformerPoolMap.put(defaultStyleSheetName,
				TransformerPool.configureTransformer0(getLogPrefix(), getClassLoader(), getNamespaceDefs(), getXpathExpression(), defaultStyleSheetName, getOutputType(), !isOmitXmlDeclaration(), getParameterList(), getXsltVersion()));
		if (isSkipEmptyTags()) {
			transformerPoolSkipEmptyTags = XmlUtils.getSkipEmptyTagsTransformerPool(isOmitXmlDeclaration(),isIndentXml());
		}
		if (isRemoveNamespaces()) {
			if (XmlUtils.XPATH_NAMESPACE_REMOVAL_VIA_XSLT) {
				transformerPoolRemoveNamespaces = XmlUtils.getRemoveNamespacesTransformerPool(isOmitXmlDeclaration(),isIndentXml());
			}
		}

		if (getXsltVersion()>=2) {
			ParameterList parameterList = getParameterList();
			if (parameterList!=null) {
				for (int i=0; i<parameterList.size(); i++) {
					Parameter parameter = parameterList.getParameter(i);
					if (StringUtils.isNotEmpty(parameter.getType()) && "node".equalsIgnoreCase(parameter.getType())) {
						throw new ConfigurationException(getLogPrefix() + "type \"node\" is not permitted in combination with XSLT 2.0, use type \"domdoc\"");
					}
				}
			}
		}
	}

	@Override
	public void open() throws SenderException {
		super.open();

		TransformerPool defaultPool = (TransformerPool) transformerPoolMap.get(defaultStyleSheetName);
		if (defaultPool!=null) {
			try {
				defaultPool.open();
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
		if (transformerPoolRemoveNamespaces!=null) {
			try {
				transformerPoolRemoveNamespaces.open();
			} catch (Exception e) {
				throw new SenderException(getLogPrefix()+"cannot start TransformerPool RemoveNamespaces", e);
			}
		}
	}

	@Override
	public void close() throws SenderException {
		super.close();
		
		if (!transformerPoolMap.isEmpty()) {
			for(Object tp : transformerPoolMap.values()) {
				((TransformerPool)tp).close();
			}
		}
		if (transformerPoolSkipEmptyTags!=null) {
			transformerPoolSkipEmptyTags.close();
		}
		if (transformerPoolRemoveNamespaces!=null) {
			transformerPoolRemoveNamespaces.close();
		}
	}

	protected Source adaptInput(String input, ParameterResolutionContext prc) throws PipeRunException, DomBuilderException, TransformerException, IOException {
		if (transformerPoolRemoveNamespaces!=null) {
			log.debug(getLogPrefix()+ " removing namespaces from input message");
			input = transformerPoolRemoveNamespaces.transform(prc.getInputSource(true), null); 
			log.debug(getLogPrefix()+ " output message after removing namespaces [" + input + "]");
			return XmlUtils.stringToSourceForSingleUse(input, true);
		}
		return prc.getInputSource(isNamespaceAware());
	}

	/**
	 * Here the actual transforming is done. Under weblogic the transformer object becomes
	 * corrupt when a not-well formed xml was handled. The transformer is then re-initialized
	 * via the configure() and start() methods.
	 */
	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		String stringResult = null;
		if (message==null) {
			throw new SenderException(getLogPrefix()+"got null input");
		}
//		if (log.isDebugEnabled()) {
//			log.debug(getLogPrefix()+" transforming input ["+message+"] using prc ["+prc+"]");
//		}

		try {
			Source inputMsg=adaptInput(message, prc);
			Map<String,Object> parametervalues = null;
			if (paramList!=null) {
				parametervalues = prc.getValueMap(paramList);
			}
//			if (log.isDebugEnabled()) {
//				log.debug(getLogPrefix()+" transformerPool ["+transformerPool+"] transforming using prc ["+prc+"] and parameterValues ["+parametervalues+"]");
//				log.debug(getLogPrefix()+" prc.inputsource ["+prc.getInputSource()+"]");
//			}

			Map<String, TransformerPool> map = Collections.synchronizedMap(transformerPoolMap);
			String styleSheetNameToUse = defaultStyleSheetName;
			TransformerPool poolToUse = map.get(defaultStyleSheetName);
			
			if(styleSheetNameSessionKey != null && !styleSheetNameSessionKey.isEmpty()) {
				styleSheetNameToUse = prc.getSession().get(styleSheetNameSessionKey).toString();

				if(!map.containsKey(styleSheetNameToUse)) {
					map.put(styleSheetNameToUse, poolToUse = TransformerPool.configureTransformer(getLogPrefix(), getClassLoader(), getNamespaceDefs(), getXpathExpression(), styleSheetNameToUse, getOutputType(), !isOmitXmlDeclaration(), getParameterList()));
					poolToUse.open();
				} else {
					poolToUse = map.get(styleSheetNameToUse);
				}
			}
			
			stringResult = poolToUse.transform(inputMsg, parametervalues);

			if (isSkipEmptyTags()) {
				log.debug(getLogPrefix()+ " skipping empty tags from result [" + stringResult + "]");
				//URL xsltSource = ClassUtils.getResourceURL( this, skipEmptyTags_xslt);
				//Transformer transformer = XmlUtils.createTransformer(xsltSource);
				//stringResult = XmlUtils.transformXml(transformer, stringResult);
				stringResult = transformerPoolSkipEmptyTags.transform(XmlUtils.stringToSourceForSingleUse(stringResult, isNamespaceAware()), null); 
			}
//			if (log.isDebugEnabled()) {
//				log.debug(getLogPrefix()+" transformed input ["+message+"] to ["+stringResult+"]");
//			}
		} 
		catch (Exception e) {
			//log.warn(getLogPrefix()+"intermediate exception logging",e);
			throw new SenderException(getLogPrefix()+" Exception on transforming input", e);
		} 
		return stringResult;
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@IbisDoc({"stylesheet to apply to the input message", ""})
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}
	public String getStyleSheetName() {
		return styleSheetName;
	}

	@IbisDoc({"force the transformer generated from the xpath-expression to omit the xml declaration", "true"})
	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}
	public boolean isOmitXmlDeclaration() {
		return omitXmlDeclaration;
	}


	@IbisDoc({"alternatively: xpath-expression to create stylesheet from", ""})
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}

	@IbisDoc({"namespace defintions for xpathexpression. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
	public String getNamespaceDefs() {
		return namespaceDefs;
	}

	@IbisDoc({"either 'text' or 'xml'. only valid for xpathexpression", "text"})
	public void setOutputType(String string) {
		outputType = string;
	}
	public String getOutputType() {
		return outputType;
	}


	@IbisDoc({"when set <code>true</code> empty tags in the output are removed", "false"})
	public void setSkipEmptyTags(boolean b) {
		skipEmptyTags = b;
	}
	public boolean isSkipEmptyTags() {
		return skipEmptyTags;
	}

	@IbisDoc({"when set <code>true</code>, result is pretty-printed. (only used when <code>skipemptytags=true</code>)", "true"})
	public void setIndentXml(boolean b) {
		indentXml = b;
	}
	public boolean isIndentXml() {
		return indentXml;
	}

	@IbisDoc({"when set <code>true</code> namespaces (and prefixes) in the input message are removed", "false"})
	public void setRemoveNamespaces(boolean b) {
		removeNamespaces = b;
	}
	public boolean isRemoveNamespaces() {
		return removeNamespaces;
	}

	@IbisDoc({"when set to <code>2</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan). <code>0</code> will auto detect", "0"})
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}
	public int getXsltVersion() {
		return xsltVersion;
	}

	@IbisDoc({"Deprecated: when set <code>true</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan)", "false"})
	/**
	 * @deprecated Please remove setting of xslt2, it will be auto detected. Or use xsltVersion.
	 */
	@Deprecated
	public void setXslt2(boolean b) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: the attribute 'xslt2' has been deprecated. Its value is now auto detected. If necessary, replace with a setting of xsltVersion";
		configWarnings.add(log, msg);
		xsltVersion=b?2:1;
	}

	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}
	public boolean isNamespaceAware() {
		return namespaceAware;
	}

	public void setStyleSheetNameSessionKey(String newSessionKey) {
		styleSheetNameSessionKey = newSessionKey;
	}
	public String getStyleSheetNameSessionKey() {
		return styleSheetNameSessionKey;
	}

	public void setStyleSheetCacheSize(int size) {
		transformerPoolMapSize = size;
	}
	public int getStyleSheetCacheSize() {
		return transformerPoolMapSize;
	}
}
