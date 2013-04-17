/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Variant;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

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
 * <tr><td>{@link #setReplaceNonXmlChars(boolean) replaceNonXmlChars}</td><td>Replace all non XML chars (not in the <a href="http://www.w3.org/TR/2006/REC-xml-20060816/#NT-Char">character range as specified by the XML specification</a>) with {@link #setReplaceNonValidXmlChar(String) replaceNonXmlChar}</td><td>true</td></tr>
 * <tr><td>{@link #setReplaceNonXmlChar(String) replaceNonXmlChar}</td><td>character that will replace each non valid XML character (empty character is also possible)</td><td>0x00BF</td></tr>
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
 * @version $Id$
 * @author Gerrit van Brakel
 * @since 4.2
 */
public class ReplacerPipe extends FixedForwardPipe {

	private String find;
	private String replace;
	private String lineSeparatorSymbol=null;
	private boolean replaceNonXmlChars=false;
	private String replaceNonXmlString=null;
	private char replaceNonXmlChar=0x00BF;

	{
		setSizeStatistics(true);
	}

	public void configure() throws ConfigurationException {
		super.configure();
//		if (StringUtils.isEmpty(getFind())) {
//			throw new ConfigurationException(getLogPrefix(null) + "cannot have empty find-attribute");
//		}
		if (StringUtils.isNotEmpty(getFind())) {
			if (getReplace() == null) {
				throw new ConfigurationException(getLogPrefix(null) + "cannot have a null replace-attribute");
			}		
			log.info(getLogPrefix(null)+ "finds ["+getFind()+"] replaces with ["+getReplace()+"]");
			if (!StringUtils.isEmpty(getLineSeparatorSymbol())) {
				find=replace(find,lineSeparatorSymbol,System.getProperty("line.separator"));
				replace=replace(replace,lineSeparatorSymbol,System.getProperty("line.separator"));
			}
		}
		if (isReplaceNonXmlChars()) {
			if (getReplaceNonXmlChar()!=null) {
				if (getReplaceNonXmlChar().length()>1) {
					throw new ConfigurationException(getLogPrefix(null) + "replaceNonXmlChar ["+getReplaceNonXmlChar()+"] has to be one character");
				}
				if (getReplaceNonXmlChar().length()==1) {
					replaceNonXmlChar = getReplaceNonXmlChar().charAt(0);
				}
			}
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

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
		throws PipeRunException {
		String string = new Variant(input).asString();
		if (StringUtils.isNotEmpty(getFind())) {
			string = replace(string,getFind(),getReplace());
		}
		if (isReplaceNonXmlChars()) {
			if (StringUtils.isEmpty(getReplaceNonXmlChar())) {
				string = XmlUtils.stripNonValidXmlCharacters(string);
			} else {
				string = XmlUtils.replaceNonValidXmlCharacters(string,replaceNonXmlChar);
			}
		}
		return new PipeRunResult(getForward(),string);
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

	public void setReplaceNonXmlChars(boolean b) {
		replaceNonXmlChars = b;
	}

	public boolean isReplaceNonXmlChars() {
		return replaceNonXmlChars;
	}

	public void setReplaceNonXmlChar(String replaceNonXmlString) {
		this.replaceNonXmlString = replaceNonXmlString;
	}
	public String getReplaceNonXmlChar() {
		return replaceNonXmlString;
	}
}

