/*
 * $Log: JsonPipe.java,v $
 * Revision 1.1  2012-06-21 12:09:10  m00f069
 * Added cleaned version from Bipa project (original version was created by Martijn)
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;


/**
 * Perform an JSON to XML transformation 
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.JsonPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>Direction of the transformation. Either json2xml or xml2json</td><td>json2xml</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Martijn Onstwedder
 */

public class JsonPipe extends FixedForwardPipe {
	private String direction="json2xml";

	public void configure() throws ConfigurationException {
		super.configure();
		String dir=getDirection();
		if (dir==null) {
			throw new ConfigurationException(getLogPrefix(null)+"direction must be set");
		}
		if (!dir.equals("json2xml") && !dir.equals("xml2json")) {
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for direction ["+dir+"], must be 'xml2json' or 'json2xml'");
		}
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this,
				getLogPrefix(session)+"got null input");
		}
 	    if (!(input instanceof String)) {
	        throw new PipeRunException(this,
	            getLogPrefix(session)+"got an invalid type as input, expected String, got "
	                + input.getClass().getName());
	    }
	    
		try {
			String stringResult = (String)input;
	
	    	String dir=getDirection();
	    	if (dir.equalsIgnoreCase("json2xml")) {	
					JSONTokener jsonTokener = new JSONTokener (stringResult);
					JSONObject jsonObject  = new JSONObject ( jsonTokener );			
					stringResult = XML.toString(jsonObject);
	    	}
	    	
			if (dir.equalsIgnoreCase("xml2json")) {	
					JSONObject jsonObject = XML.toJSONObject(stringResult);
					stringResult = jsonObject.toString();
				}
			
			return new PipeRunResult(getForward(), stringResult);
	    } 
	    catch (Exception e) {
	        throw new PipeRunException(this, getLogPrefix(session)+" Exception on transforming input", e);
	    } 
	}

	public void setDirection(String string) {
		direction = string;
	}

	public String getDirection() {
		return StringUtils.lowerCase(direction);
	}

}
