/*
 * $Log: ParameterResolutionContext.java,v $
 * Revision 1.3  2005-01-13 08:08:33  L190409
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.transform.dom.DOMSource;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.DomBuilderException;

import nl.nn.adapterframework.util.XmlUtils;


import org.apache.log4j.Logger;
import org.w3c.dom.Document;


/*
 * $Log: ParameterResolutionContext.java,v $
 * Revision 1.3  2005-01-13 08:08:33  L190409
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
 * @author John Dekker
 * @version Id
 */
public class ParameterResolutionContext {
	public static final String version="$Id: ParameterResolutionContext.java,v 1.3 2005-01-13 08:08:33 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());

	private String input;
	private PipeLineSession session;
	private DOMSource inputSource;

	/**
	 * constructor
	 * @param input contains the input (xml formatted) message
	 * @param session 
	 */		
	public ParameterResolutionContext(String input, PipeLineSession session) {
		this.input = input;
		this.session = session;
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
			
			result.put(p.getName(),getValue(p));
		}
		return result;
	}
	
	/**
	 * @author John Dekker
	 * interface to be used as callback handler in the forAllParameterValues methode
	 */
	public interface ValueHandler {
		/**
		 * @param pType the parameter type
		 * @param value the raw value 
		 * @param callbackObject  
		 */
		void handle(Parameter pType, Object value, Object callbackObject);	
	}

	/**
	 * Iterator through all parameters and call the handler for each parameter
	 * @param parameters
	 * @param handler
	 * @param callbackObject
	 * @throws IbisException
	 */		
	public void forAllParameterValues(ArrayList parameters, ValueHandler handler, Object callbackObject) throws IbisException {
		if (parameters != null) {
			for (Iterator it= parameters.iterator(); it.hasNext(); ) {
				Parameter p = (Parameter)it.next();
				Object val = p.getValue(this);
				handler.handle(p, val, callbackObject);
			}			
		}
	}
	
	/**
	 * @return the DOM document parsed from the (xml formatted) input
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public DOMSource getInputSource() throws DomBuilderException {
		if (inputSource == null) {
			// TODO try SaxInputSource
			log.debug("Constructing DomInputSource for ParameterResolutionContext");
			Document doc = XmlUtils.buildDomDocument(input);
			inputSource = new DOMSource(doc); 
		}
		return inputSource;
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
		this.inputSource = null;
	}

	/**
	 * @param session
	 */
	public void setSession(PipeLineSession session) {
		this.session = session;
	}

}
