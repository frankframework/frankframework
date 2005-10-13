/*
 * $Log: Base64Pipe.java,v $
 * Revision 1.2  2005-10-13 11:44:53  europe\L190409
 * switched encode and decode code
 *
 * Revision 1.1  2005/10/05 07:38:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of Base64Pipe
 *
 */
package nl.nn.adapterframework.pipes;


import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;


/**
 * Pipe that performs base64 encoding and decoding.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>either <code>encode</code> or <code>decode</code></td><td>"encode"</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.4
 * @version Id
 */
public class Base64Pipe extends FixedForwardPipe {
	public static final String version="$RCSfile: Base64Pipe.java,v $ $Revision: 1.2 $ $Date: 2005-10-13 11:44:53 $";

	private String direction="encode";

	public void configure() throws ConfigurationException {
		super.configure();
		String dir=getDirection();
		if (dir==null) {
			throw new ConfigurationException(getLogPrefix(null)+"direction must be set");
		}
		if (!dir.equalsIgnoreCase("encode") && !dir.equalsIgnoreCase("decode")) {
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for direction ["+dir+"], must be 'encode' or 'decode'");
		}
	}

	public PipeRunResult doPipe(Object invoer, PipeLineSession session) throws PipeRunException {
		String in=invoer.toString();
		String result=null;
		
		if (in!=null) {
			if ("encode".equalsIgnoreCase(getDirection())) {
				BASE64Encoder encoder = new BASE64Encoder();
				result=encoder.encode(in.getBytes());
			} else {
				BASE64Decoder decoder = new BASE64Decoder();
				try {
					result=new String(decoder.decodeBuffer(in));
				} catch (IOException e) {
					throw new PipeRunException(this, getLogPrefix(session)+"cannot decode base64", e);
				}
			}
		} else {
			log.debug(getLogPrefix(session)+"has null input, returning null");
		}
		return new PipeRunResult(getForward(), result);
	}
	

	public void setDirection(String string) {
		direction = string;
	}
	public String getDirection() {
		return direction;
	}

}
