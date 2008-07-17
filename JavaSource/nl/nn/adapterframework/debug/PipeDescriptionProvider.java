/*
 * $Log: PipeDescriptionProvider.java,v $
 * Revision 1.2  2008-07-17 16:16:26  europe\L190409
 * made PipeDescription an interface
 *
 * Revision 1.1  2008/07/14 17:07:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of debugger
 *
 */
package nl.nn.adapterframework.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Get a description of a specified pipe. The description contains the XML
 * configuration for the pipe and optionally the XSLT files used by the pipe.
 *
 * @author  Jaco de Groot (***@dynasol.nl)
 * @since   4.9
 * @version Id
 */
public class PipeDescriptionProvider {
	private Logger log = LogUtil.getLogger(this);

	private Map pipeDescriptionCache = new HashMap();
	private Map styleSheetNameCache = new HashMap();

	public PipeDescriptionProvider(PipeLine pipeline) throws ConfigurationException {
	}

	/**
	 * Get a PipeDescription objectt for the specified pipe. The returned object
	 * is cached.
	 */
	public PipeDescription getPipeDescription(IPipe pipe) {
		PipeDescriptionImpl pipeDescription;
		synchronized(pipeDescriptionCache) {
			pipeDescription = (PipeDescriptionImpl)pipeDescriptionCache.get(pipe);
			if (pipeDescription == null) {
				pipeDescription = new PipeDescriptionImpl();
				pipeDescription.setDescription(pipe.toString());
				List styleSheetNameNodes = new ArrayList();
				addStyleSheet(styleSheetNameNodes,pipe,"styleSheetName");
				addStyleSheet(styleSheetNameNodes,pipe,"sender.styleSheetName");
				addStyleSheet(styleSheetNameNodes,pipe,"serviceSelectionStylesheetFilename");
				pipeDescription.setStyleSheetNames(styleSheetNameNodes);
				pipeDescriptionCache.put(pipe, pipeDescription);
			}
		}
		return pipeDescription;
	}

	private void addStyleSheet(List stylesheetList, IPipe pipe, String propertyName) {
		try {
			String property = BeanUtils.getProperty(pipe, propertyName);
			if (StringUtils.isNotEmpty(property)) {
				stylesheetList.add(property);
			}
		} catch (Exception e) {
			log.error("Could not read property ["+propertyName+"] for pipe ["+ pipe.getName()+"]", e);
		}
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
