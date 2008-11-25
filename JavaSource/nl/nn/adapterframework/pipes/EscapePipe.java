/*
 * $Log: EscapePipe.java,v $
 * Revision 1.1  2008-11-25 10:13:51  m168309
 * introduction of EscapePipe
 *
 */
package nl.nn.adapterframework.pipes;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe that performs translations between special characters and their xml equivalents.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>either <code>encode</code> or <code>decode</code></td><td>encode</td></tr>
 * <tr><td>{@link #setSubstringStart(String) substringStart}</td><td>substring to start translation</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSubstringEnd(String) substringEnd}</td><td>substring to end translation</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setEncodeSubstring(boolean) decodeSubstring}</td><td>when set <code>true</code> special characters in <code>substringStart</code> and <code>substringEnd</code> are first translated to their xml equivalents</td><td>false</td></tr>
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
 * @author Peter Leeuwenburgh
 */
public class EscapePipe extends FixedForwardPipe {
	public static final String version = "$Id: EscapePipe.java,v 1.1 2008-11-25 10:13:51 m168309 Exp $";

	private String substringStart;
	private String substringEnd;
	private String direction = "encode";
	private boolean encodeSubstring = false;


	public void configure() throws ConfigurationException {
		super.configure();
		String dir = getDirection();
		if (dir == null) {
			throw new ConfigurationException(
				getLogPrefix(null) + "direction must be set");
		}
		if (!dir.equalsIgnoreCase("encode")
			&& !dir.equalsIgnoreCase("decode")) {
			throw new ConfigurationException(
				getLogPrefix(null)
					+ "illegal value for direction ["
					+ dir
					+ "], must be 'encode' or 'decode'");
		}
		if ((substringStart != null && substringEnd == null)
			|| (substringStart == null && substringEnd != null)) {
			throw new ConfigurationException(
				getLogPrefix(null)
					+ "cannot have only one of substringStart or substringEnd");
		}
		if (isEncodeSubstring()) {
			substringStart = XmlUtils.encodeChars(substringStart);
			substringEnd = XmlUtils.encodeChars(substringEnd);
		}
	}

	public PipeRunResult doPipe(Object input, PipeLineSession session)
		throws PipeRunException {

		String string = input.toString();
		String substring = null;
		String result = string;
		int i = -1;
		int j = -1;
		log.debug("string [" + string + "]");
		log.debug("substringStart [" + substringStart + "]");
		log.debug("substringEnd [" + substringEnd + "]");
		if (substringStart != null && substringEnd != null) {
			i = string.indexOf(substringStart);
			if (i != -1) {
				j = string.indexOf(substringEnd, i);
				if (j != -1) {
					substring =
						string.substring(i + substringStart.length(), j);
					if ("encode".equalsIgnoreCase(getDirection())) {
						substring = XmlUtils.encodeChars(substring);
					} else {
						substring = XmlUtils.decodeChars(substring);
					}
					result =
						string.substring(0, i + substringStart.length())
							+ substring
							+ string.substring(j);
				}
			}
		} else {
			if ("encode".equalsIgnoreCase(getDirection())) {
				result = XmlUtils.encodeChars(string);
			} else {
				result = XmlUtils.decodeChars(string);
			}
		}

		return new PipeRunResult(getForward(), result);
	}

	public String getSubstringStart() {
		return substringStart;
	}
	public void setSubstringStart(String substringStart) {
		this.substringStart = substringStart;
	}

	public String getSubstringEnd() {
		return substringEnd;
	}
	public void setSubstringEnd(String substringEnd) {
		this.substringEnd = substringEnd;
	}

	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}

	public boolean isEncodeSubstring() {
		return encodeSubstring;
	}
	public void setEncodeSubstring(boolean b) {
		encodeSubstring = b;
	}
}