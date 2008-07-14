/*
 * $Log: PipeDescriptionProvider.java,v $
 * Revision 1.1  2008-07-14 17:07:32  europe\L190409
 * first version of debugger
 *
 */
package nl.nn.adapterframework.debug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.w3c.dom.Document;


/**
 * Get a description of a specified pipe. The description contains the XML
 * configuration for the pipe and optionally the XSLT files used by the pipe.
 *
 * @author  Jaco de Groot (***@dynasol.nl)
 * @since   4.9
 * @version Id
 */
public class PipeDescriptionProvider {
	private String adapterName;
	private Document document;
	private Map pipeDescriptionCache = new HashMap();
	private Map styleSheetNameCache = new HashMap();
	
	public static final String PIPEDESCRIPTION_XPATH="//*/adapter[@name=\"$adapterName\"]/pipeline/pipe[@name=\"$pipeName\"]"; 
	public static final String STYLESHEET_XPATH="//@styleSheetName"; 
	
	private TransformerPool pipeDescriptionExtractor;
	private TransformerPool stylesheetExtractor;

	public PipeDescriptionProvider(String adapterName) throws ConfigurationException {
		this.adapterName = adapterName;
		try {
			// TODO: use correct configuration file, not just "Configuration.xml". [GvB]
			document = XmlUtils.buildDomDocument(ClassUtils.getResourceURL(this, "Configuration.xml"));
		} catch(DomBuilderException e) {
			throw new ConfigurationException("cannot read configuration",e);
		}
		try {
			pipeDescriptionExtractor= new TransformerPool(XmlUtils.createXPathEvaluatorSource(PIPEDESCRIPTION_XPATH,"xml"));
			stylesheetExtractor= new TransformerPool(XmlUtils.createXPathEvaluatorSource(STYLESHEET_XPATH));
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("cannot create transformers from",e);
		}
	}

	/**
	 * Get a PipeDescription objectt for the specified pipe. The returned object
	 * is cached.
	 */
	public PipeDescription getPipeDescription(String pipeName) {
		PipeDescription pipeDescription;
		synchronized(pipeDescriptionCache) {
			pipeDescription = (PipeDescription)pipeDescriptionCache.get(pipeName);
			if (pipeDescription == null) {
				pipeDescription = new PipeDescription();
				Map params=new HashMap();
				params.put("adapterName",adapterName);
				params.put("pipeName",pipeName);
				
				try{
					String pipeConfig = pipeDescriptionExtractor.transform(document,params);
					pipeDescription.setDescription(pipeConfig);
				} catch (Exception e) {
					pipeDescription.setDescription("Exception: " + e.getMessage());
				}
//				Node node = document.selectSingleNode( "//*/adapter[@name=\"" + adapterName + "\"]/pipeline/pipe[@name=\"" + pipeName + "\"]" );
//				if (node != null) {
//					StringWriter stringWriter = new StringWriter();
//					OutputFormat outputFormat = OutputFormat.createPrettyPrint();
//					XMLWriter xmlWriter = new XMLWriter(stringWriter, outputFormat);
//					try {
//						xmlWriter.write(node);
//						xmlWriter.flush();
//						pipeDescription.setDescription(stringWriter.toString());
//					} catch(IOException e) {
//						pipeDescription.setDescription("IOException: " + e.getMessage());
//					}
//					List styleSheetNameNodes = node.selectNodes("@styleSheetName");
//					styleSheetNameNodes.addAll(node.selectNodes("*/@styleSheetName"));
//					styleSheetNameNodes.addAll(node.selectNodes("*/*/@styleSheetName"));
//					styleSheetNameNodes.addAll(node.selectNodes("*/*/*/@styleSheetName"));
//					styleSheetNameNodes.addAll(node.selectNodes("@serviceSelectionStylesheetFilename"));
//					Iterator iterator = styleSheetNameNodes.iterator();
//					while (iterator.hasNext()) {
//						String styleSheetName = ((Node)iterator.next()).getStringValue();
//						pipeDescription.addStyleSheetName(styleSheetName);
//					}
//				} else {
//					pipeDescription.setDescription("Pipe not found in configuration.");
//				}
				pipeDescriptionCache.put(pipeName, pipeDescription);
			}
		}
		return pipeDescription;
	}

	/**
	 * Return the content of the specified style sheet. The returned object
	 * is cached.
	 */
	public String getStyleSheet(String styleSheetName) {
		String styleSheet;
		synchronized(styleSheetNameCache) {
			styleSheet = (String)styleSheetNameCache.get(styleSheetName);
			if (styleSheet == null) {
				try {
					styleSheet = Misc.resourceToString(ClassUtils.getResourceURL(this, styleSheetName), "\n", false);
				} catch(IOException e) {
					styleSheet = "IOException: " + e.getMessage();
				}
				styleSheetNameCache.put(styleSheetName, styleSheet);
			}
		}
		return styleSheet;
	}

}
