package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * Provides an example of a pipe. It may return the contents of a file
 * (in the classpath) when <code>setFileName</code> is specified, otherwise the
 * input of <code>setReturnString</code> is returned.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setFileName(String) fileName}</td>        <td>name of the file containing the resultmessage</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReturnString(String) returnString}</td><td>returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSubstituteVars(boolean) substitute}</td><td>Should values between ${ and } be resolved from the PipeLineSession</td><td>False</td></tr>
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
	public static final String version="$Id: FixedResult.java,v 1.6 2004-08-27 08:16:54 NNVZNL01#L180564 Exp $";
    private String fileName;
    private String returnString;
    private boolean substituteVars=false;

	public void setSubstituteVars(boolean substitute){
		this.substituteVars=substitute;
	}

	public boolean getSubstituteVars(){
		return this.substituteVars;
	}

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
        if ((StringUtils.isEmpty(fileName)) && (StringUtils.isEmpty(returnString))) {
            throw new ConfigurationException("Pipe [" + getName() + "] has neither fileName nor returnString specified");
        }
    }
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String result=returnString;
		if (getSubstituteVars()){
			result=StringResolver.substVars(returnString, session);
		}
	    log.debug(getLogPrefix(session)+ " returning fixed result [" + result + "]");
	
	    return new PipeRunResult(getForward(), result);
	}
    public String getFileName() {
        return fileName;
    }
    public String getReturnString() {
        return returnString;
    }
    /**
     * Sets the name of the filename. The fileName should not be specified
     * as an absolute path, but as a file in the classpath.
     *
     * @param fileName the name of the file to return the contents from
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public void setReturnString(String returnString) {
        this.returnString = returnString;
    }
}
