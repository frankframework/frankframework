/*
 * $Log: ParameterResolutionContext.java,v $
 * Revision 1.1  2004-10-05 09:51:54  L190409
 * changed from ParameterResolver to ParameterResolutionContext
 * moved to package parameters
 *
 */
package nl.nn.adapterframework.parameters;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import javax.xml.transform.dom.DOMSource;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.DomBuilderException;

import nl.nn.adapterframework.util.XmlUtils;


import org.w3c.dom.Document;


/*
 * $Log: ParameterResolutionContext.java,v $
 * Revision 1.1  2004-10-05 09:51:54  L190409
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
	private Object input;
	private Hashtable session;
	private DOMSource inputSource;

	/**
	 * constructor
	 * @param input contains the input (xml formatted) message
	 * @param session 
	 */		
	public ParameterResolutionContext(Object input, Hashtable session) {
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
			Document doc = XmlUtils.buildDomDocument((String)input);
			inputSource = new DOMSource(doc); 
		}
		return inputSource;
	}

	/**
	 * @return the (possibly xml formatted) input message
	 */
	public Object getInput() {
		return input;
	}

	/**
	 * @return hashtable with session variables
	 */
	public Hashtable getSession() {
		return session;
	}

	/**
	 * @param input the (xml formatted) input message
	 */
	public void setInput(Object input) {
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
