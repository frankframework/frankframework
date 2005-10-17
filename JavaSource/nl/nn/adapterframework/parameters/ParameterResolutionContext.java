/*
 * $Log: ParameterResolutionContext.java,v $
 * Revision 1.9  2005-10-17 11:43:34  europe\L190409
 * namespace-awareness configurable
 *
 * Revision 1.8  2005/06/13 11:55:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made namespaceAware
 *
 * Revision 1.7  2005/06/02 11:47:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * obtain source from XmlUtils
 *
 * Revision 1.6  2005/03/31 08:15:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * generalized Source
 *
 * Revision 1.5  2005/02/24 10:49:56  Johan Verrips <johan.verrips@ibissource.org>
 * 4.2.e dd 24-02-2005
 *
 * Revision 1.4  2005/02/10 08:15:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in map-generation
 *
 * Revision 1.3  2005/01/13 08:08:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Xslt parameter handling by Maps instead of by Ibis parameter system
 *
 * Revision 1.2  2004/10/14 16:07:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed from Object,Hashtable to String, PipelineSession
 *
 * Revision 1.1  2004/10/05 09:51:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed from ParameterResolver to ParameterResolutionContext
 * moved to package parameters
 *
 */
package nl.nn.adapterframework.parameters;


import java.util.HashMap;
import java.util.Iterator;

import javax.xml.transform.Source;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;


import org.apache.log4j.Logger;

/*
 * $Log: ParameterResolutionContext.java,v $
 * Revision 1.9  2005-10-17 11:43:34  europe\L190409
 * namespace-awareness configurable
 *
 * Revision 1.8  2005/06/13 11:55:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made namespaceAware
 *
 * Revision 1.7  2005/06/02 11:47:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * obtain source from XmlUtils
 *
 * Revision 1.6  2005/03/31 08:15:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * generalized Source
 *
 * Revision 1.5  2005/02/24 10:49:56  Johan Verrips <johan.verrips@ibissource.org>
 * 4.2.e dd 24-02-2005
 *
 * Revision 1.4  2005/02/10 08:15:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in map-generation
 *
 * Revision 1.3  2005/01/13 08:08:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Xslt parameter handling by Maps instead of by Ibis parameter system
 *
 * Revision 1.2  2004/10/14 16:07:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed from Object,Hashtable to String, PipelineSession
 *
 * Revision 1.1  2004/10/05 09:51:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed from ParameterResolver to ParameterResolutionContext
 * moved to package parameters
 *
 * Revision 1.3  2004/06/16 13:08:20  Johan Verrips <johan.verrips@ibissource.org>
 * removed unused imports
 *
 * Revision 1.2  2004/05/25 09:27:56  unknown <unknown@ibissource.org>
 * Optimize performance by caching the transformer
 *
 * Revision 1.1  2004/05/21 07:58:47  unknown <unknown@ibissource.org>
 * Moved PipeParameter to core
 *
 */
 
/**
 * Determines the parameter values of the specified parameter during runtime
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class ParameterResolutionContext {
	public static final String version="$RCSfile: ParameterResolutionContext.java,v $ $Revision: 1.9 $ $Date: 2005-10-17 11:43:34 $";
	protected Logger log = Logger.getLogger(this.getClass());

	private String input;
	private PipeLineSession session;
	private Source xmlSource;
	private boolean namespaceAware;

	/**
	 * constructor
	 * @param input contains the input (xml formatted) message
	 * @param session 
	 */		
	public ParameterResolutionContext(String input, PipeLineSession session, boolean namespaceAware) {
		this.input = input;
		this.session = session;
		this.namespaceAware = namespaceAware;
	}

	public ParameterResolutionContext(String input, PipeLineSession session) {
		this(input, session, XmlUtils.isNamespaceAwareByDefault());
	}

	public ParameterResolutionContext(Source xmlSource, PipeLineSession session, boolean namespaceAware) {
		this("", session, namespaceAware);
		this.xmlSource=xmlSource;
	}

	public ParameterResolutionContext(Source xmlSource, PipeLineSession session) {
		this(xmlSource, session, XmlUtils.isNamespaceAwareByDefault());
	}
			
	/**
	 * @param p
	 * @return value as a <link>ParameterValue<link> object
	 * @throws IbisException
	 */
	public ParameterValue getValue(Parameter p) throws ParameterException {
		return new ParameterValue(p, p.getValue(this));
	}
	
	/**
	 * @param parameters
	 * @return arraylist of <link>ParameterValue<link> objects
	 */
	public ParameterValueList getValues(ParameterList parameters) throws ParameterException {
		if (parameters == null)
			return null;
		
		ParameterValueList result = new ParameterValueList(parameters.size());
		for (Iterator it= parameters.iterator(); it.hasNext(); ) {
			result.add(getValue((Parameter)it.next()));
		}
		return result;
	}

	/**
	 * @param parameters
	 * @return arraylist of <link>ParameterValue<link> objects
	 */
	public HashMap getValueMap(ParameterList parameters) throws ParameterException {
		if (parameters == null)
			return null;
		
		HashMap result = new HashMap(parameters.size());
		for (Iterator it= parameters.iterator(); it.hasNext(); ) {
			Parameter p=(Parameter)it.next();
			
			result.put(p.getName(),getValue(p).getValue());
		}
		return result;
	}
	

		
	/**
	 * @return the DOM document parsed from the (xml formatted) input
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Source getInputSource() throws DomBuilderException {
		if (xmlSource == null) {
			log.debug("Constructing InputSource for ParameterResolutionContext");
			xmlSource = XmlUtils.stringToSource(input,isNamespaceAware()); 

		}
		return xmlSource;
	}

	/**
	 * @return the (possibly xml formatted) input message
	 */
	public String getInput() {
		return input;
	}

	/**
	 * @return hashtable with session variables
	 */
	public PipeLineSession getSession() {
		return session;
	}

	/**
	 * @param input the (xml formatted) input message
	 */
	public void setInput(String input) {
		this.input = input;
		this.xmlSource = null;
	}

	/**
	 * @param session
	 */
	public void setSession(PipeLineSession session) {
		this.session = session;
	}

	/**
	 * @return
	 */
	public boolean isNamespaceAware() {
		return namespaceAware;
	}

	/**
	 * @param b
	 */
	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}

}
