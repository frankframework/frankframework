/*
 * $Log: ReplacerPipe.java,v $
 * Revision 1.1  2004-08-03 11:28:12  L190409
 * first version
 * 
 */
package nl.nn.adapterframework.pipes;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Variant;

/**
 * Replaces all occurrences of one string with another.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFind(String) find}</td><td>string to search for</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplace(String) replace}</td><td>string that will replace each of the strings found</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLineSeparatorSymbol(String) lineSeparatorSymbol}</td><td>Sets the string the representation in find and replace of the line separator</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
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
 * @author Gerrit van Brakel
 * @since 4.2
 */
public class ReplacerPipe extends FixedForwardPipe {
	public static final String version="$Id: ReplacerPipe.java,v 1.1 2004-08-03 11:28:12 L190409 Exp $";

	private String find;
	private String replace;
	private String lineSeparatorSymbol=null;
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (getFind() == null) {
			throw new ConfigurationException(getLogPrefix(null) + "cannot have empty find-attribute");
		}
		log.info(getLogPrefix(null)+ "finds ["+getFind()+"] relaces with ["+getReplace()+"]");
		if (!StringUtils.isEmpty(getLineSeparatorSymbol())) {
			find=replace(find,lineSeparatorSymbol,System.getProperty("line.separator"));
			replace=replace(replace,lineSeparatorSymbol,System.getProperty("line.separator"));
		}
	}

	protected static String replace(String target, String from, String to) {
		// target is the original string
		// from   is the string to be replaced
		// to     is the string which will used to replace
		int start = target.indexOf(from);
		if (start == -1)
			return target;
		int lf = from.length();
		char[] targetChars = target.toCharArray();
		StringBuffer buffer = new StringBuffer();
		int copyFrom = 0;
		while (start != -1) {
			buffer.append(targetChars, copyFrom, start - copyFrom);
			buffer.append(to);
			copyFrom = start + lf;
			start = target.indexOf(from, copyFrom);
		}
		buffer.append(targetChars, copyFrom, targetChars.length - copyFrom);
		return buffer.toString();
	}


	public PipeRunResult doPipe(Object input, PipeLineSession session)
		throws PipeRunException {

		return new PipeRunResult(getForward(),replace(new Variant(input).asString(),getFind(),getReplace()));
	}
	
	/**
	 * Sets the string that is searched for.
	 */ 
	public void setFind(String find) {
		this.find = find;
	}
	public String getFind() {
		return find;
	}
	
	/**
	 * Sets the string that will replace each of the occurrences of the find-string.
	 */ 
	public void setReplace(String replace) {
		this.replace = replace;
	}
	public String getReplace() {
		return replace;
	}

	/**
	 * Sets the string the representation in find and replace of the line separator.
	 */ 
	public String getLineSeparatorSymbol() {
		return lineSeparatorSymbol;
	}
	public void setLineSeparatorSymbol(String string) {
		lineSeparatorSymbol = string;
	}

}

