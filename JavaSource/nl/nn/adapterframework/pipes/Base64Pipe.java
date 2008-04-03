/*
 * $Log: Base64Pipe.java,v $
 * Revision 1.3.16.1  2008-04-03 08:14:16  europe\L190409
 * synch from HEAD
 *
 * Revision 1.4  2008/03/20 12:06:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2006/04/25 06:56:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute convert2String
 *
 * Revision 1.2  2005/10/13 11:44:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * <tr><td>{@link #setConvert2String(boolean) convert2String}</td><td>If <code>true</code> and decoding, result is returned as a string, otherwise as a byte array. If <code>true</code> and encoding, input is read as a string, otherwise as a byte array.</td><td><code>true</code></td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel / Jaco de Groot (***@dynasol.nl)
 * @since   4.4
 * @version Id
 */
public class Base64Pipe extends FixedForwardPipe {
	public static final String version="$RCSfile: Base64Pipe.java,v $ $Revision: 1.3.16.1 $ $Date: 2008-04-03 08:14:16 $";

	private String direction="encode";
	private boolean convert2String=true;

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
		Object result=null;
		if (invoer!=null) {
			if ("encode".equalsIgnoreCase(getDirection())) {
				BASE64Encoder encoder = new BASE64Encoder();
				if (convert2String) {
					result=encoder.encode(invoer.toString().getBytes());
				} else {
					result=encoder.encode((byte[])invoer);
				}
			} else {
				BASE64Decoder decoder = new BASE64Decoder();
				String in=invoer.toString();
				try {
					if (convert2String) {
						result=new String(decoder.decodeBuffer(in));
					} else {
						result=decoder.decodeBuffer(in);
					}
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

	public void setConvert2String(boolean b) {
		convert2String = b;
	}

}
