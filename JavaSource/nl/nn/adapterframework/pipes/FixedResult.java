package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.IOException;

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
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * <p>$Id: FixedResult.java,v 1.2 2004-02-04 10:01:57 a1909356#db2admin Exp $</p>
 * @author Johan Verrips
 */
public class FixedResult extends FixedForwardPipe {
	public static final String version="$Id: FixedResult.java,v 1.2 2004-02-04 10:01:57 a1909356#db2admin Exp $";
    private String fileName;
    private String returnString;
    /**
     * checks for correct configuration, and translates the fileName to
     * a file, to check existence. The field fileName is overriden with the
     * URL.getFile() information.
     * @throws ConfigurationException
     */
    public void configure() throws ConfigurationException {
	    super.configure();
	    
        if (StringUtils.isNotEmpty(fileName)) {
            try {
                Misc.resourceToString(ClassUtils.getResourceURL(this,fileName));
            } catch (Throwable e) {
                throw new ConfigurationException("Pipe [" + getName() + "] got exception loading ["+fileName+"]", e);
            }
        }
        if ((StringUtils.isEmpty(fileName)) && (StringUtils.isEmpty(returnString))) {
            throw new ConfigurationException("Pipe [" + getName() + "] has neither fileName nor returnString specified");
        }
    }
public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {

    log.debug(getLogPrefix(session)+ " returning fixed result [" + returnString + "]");

    return new PipeRunResult(getForward(), returnString);
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
    /**
     * If a fileName was specified, the contents of the file is put in the
     * <code>returnString</code>, so that allways the <code>returnString</code>
     * may be returned.
     * @throws PipeStartException
     */
    public void start() throws PipeStartException {
        if (StringUtils.isNotEmpty(fileName)) {
            try {
                returnString = Misc.resourceToString(ClassUtils.getResourceURL(this, fileName), SystemUtils.LINE_SEPARATOR);
            } catch (IOException e) {
                throw new PipeStartException("Pipe [" + getName() + "] got exception", e);
            }
        }
    }
}
