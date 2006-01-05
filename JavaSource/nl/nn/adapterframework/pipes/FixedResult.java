/*
 * $Log: FixedResult.java,v $
 * Revision 1.14  2006-01-05 14:34:19  europe\L190409
 * allow an empty resultstring to be specified
 *
 * Revision 1.13  2005/12/29 15:17:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.12  2005/09/26 11:07:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * better handling of null-parameter values
 *
 * Revision 1.11  2005/08/18 13:40:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.10  2005/08/11 15:00:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * parameters can now be used to replace ${...} constructs
 *
 * Revision 1.9  2005/04/26 09:19:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added replace facilty (by Peter Leeuwenburgh)
 *
 * Revision 1.8  2004/10/05 10:50:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.7  2004/09/01 07:21:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * correction in documentation
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * Provides an example of a pipe. It may return the contents of a file
 * (in the classpath) when <code>fileName</code> is specified, otherwise the
 * input of <code>returnString</code> is returned.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.FixedResult</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setFileName(String) fileName}</td>        <td>name of the file containing the resultmessage</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReturnString(String) returnString}</td><td>returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSubstituteVars(boolean) substituteVars}</td><td>Should values between ${ and } be resolved from the PipeLineSession</td><td>False</td></tr>
 * <tr><td>{@link #setReplaceFrom(String) replaceFrom}</td><td>string to search for in the returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplaceTo(String) replaceTo}</td><td>string that will replace each of the strings found in the returned message</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>Any parameters defined on the pipe will be used for replacements. Each <code>${name-of-parameter}</code> will be replaced by its corresponding <i>value-of-parameter</i> </td></tr>
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
 * @author Johan Verrips
 */
public class FixedResult extends FixedForwardPipe {
	public static final String version="$RCSfile: FixedResult.java,v $ $Revision: 1.14 $ $Date: 2006-01-05 14:34:19 $";
	
    private String fileName;
    private String returnString;
    private boolean substituteVars=false;
	private String replaceFrom = null;
	private String replaceTo = null;

    /**
     * checks for correct configuration, and translates the fileName to
     * a file, to check existence. 
     * If a fileName was specified, the contents of the file is put in the
     * <code>returnString</code>, so that allways the <code>returnString</code>
     * may be returned.
     * @throws ConfigurationException
     */
    public void configure() throws ConfigurationException {
	    super.configure();
	    
        if (StringUtils.isNotEmpty(fileName)) {
            try {
				returnString = Misc.resourceToString(ClassUtils.getResourceURL(this,fileName), SystemUtils.LINE_SEPARATOR);
            } catch (Throwable e) {
                throw new ConfigurationException("Pipe [" + getName() + "] got exception loading ["+fileName+"]", e);
            }
        }
        if ((StringUtils.isEmpty(fileName)) && returnString==null) {  // allow an empty returnString to be specified
            throw new ConfigurationException("Pipe [" + getName() + "] has neither fileName nor returnString specified");
        }
		if (StringUtils.isNotEmpty(replaceFrom)) {
			returnString = replace(returnString, replaceFrom, replaceTo );
		}
    }
    
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String result=returnString;

		if (getParameterList()!=null) {
			ParameterResolutionContext prc = new ParameterResolutionContext((String)input, session);
			ParameterValueList pvl;
			try {
				pvl = prc.getValues(getParameterList());
			} catch (ParameterException e) {
				throw new PipeRunException(this,getLogPrefix(session)+"exception extracting parameters",e);
			}
			for (int i=0; i<pvl.size(); i++) {
				ParameterValue pv = pvl.getParameterValue(i);
				result=replace(result,"${"+pv.getDefinition().getName()+"}",pv.asStringValue(""));
			}
		}

		if (getSubstituteVars()){
			result=StringResolver.substVars(returnString, session);
		}
	    log.debug(getLogPrefix(session)+ " returning fixed result [" + result + "]");
	
	    return new PipeRunResult(getForward(), result);
	}

	public static String replace (String target, String from, String to) {   
		// target is the original string
		// from   is the string to be replaced
		// to     is the string which will used to replace
		int start = target.indexOf (from);
		if (start==-1) return target;
		int lf = from.length();
		char [] targetChars = target.toCharArray();
		StringBuffer buffer = new StringBuffer();
		int copyFrom=0;
		while (start != -1) {
			buffer.append (targetChars, copyFrom, start-copyFrom);
			buffer.append (to);
			copyFrom=start+lf;
			start = target.indexOf (from, copyFrom);
		}
		buffer.append (targetChars, copyFrom, targetChars.length-copyFrom);
		return buffer.toString();
	}

	public void setSubstituteVars(boolean substitute){
		this.substituteVars=substitute;
	}
	public boolean getSubstituteVars(){
		return this.substituteVars;
	}

    /**
     * Sets the name of the filename. The fileName should not be specified
     * as an absolute path, but as a resource in the classpath.
     *
     * @param fileName the name of the file to return the contents from
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
	public String getFileName() {
		return fileName;
	}

    public void setReturnString(String returnString) {
        this.returnString = returnString;
    }
	public String getReturnString() {
		return returnString;
	}

	public String getReplaceFrom() {
		return replaceFrom;
	}
	public void setReplaceFrom (String replaceFrom){
		this.replaceFrom=replaceFrom;
	}

	public String getReplaceTo() {
		return replaceTo;
	}
	public void setReplaceTo (String replaceTo){
		this.replaceTo=replaceTo;
	}
}
