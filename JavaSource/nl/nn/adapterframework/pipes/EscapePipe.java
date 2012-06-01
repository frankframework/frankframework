/*
 * $Log: EscapePipe.java,v $
 * Revision 1.4  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.3  2011/11/30 13:51:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.3  2011/11/10 15:48:06  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added direction cdata2text
 *
 * Revision 1.2  2011/10/19 15:01:14  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2008/11/25 10:13:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * introduction of EscapePipe
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe that performs translations between special characters and their xml equivalents.
 * <p>When direction=cdata2text all cdata nodes are converted to text nodes without any other translations.</p>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>either <code>encode</code>, <code>decode</code> or <code>cdata2text</code></td><td>encode</td></tr>
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
			&& !dir.equalsIgnoreCase("decode")
				&& !dir.equalsIgnoreCase("cdata2text")) {
			throw new ConfigurationException(
				getLogPrefix(null)
					+ "illegal value for direction ["
					+ dir
					+ "], must be 'encode', 'decode' or 'cdata2text'");
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

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
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
						if ("decode".equalsIgnoreCase(getDirection())) {
							substring = XmlUtils.decodeChars(substring);
						} else {
							substring = XmlUtils.cdataToText(substring);
						}
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
				if ("decode".equalsIgnoreCase(getDirection())) {
					result = XmlUtils.decodeChars(string);
				} else {
					result = XmlUtils.cdataToText(string);
				}
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