package nl.nn.adapterframework.extensions.rekenbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.Misc;
/**
 * Perform a call to a RekenBox.
 *
 * The inputmessage is written to a temporary file and passed as inputfile to the rekenbox. The contents of the outputfile of the
 * rekenbox is returned as output message. The name of the rekenbox, as determined from the inputfile, is optionally written to
 * the pipeLineSession.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setRunPath(String) runPath}</td><td>directory on server where rekenbox-executable can be found</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTemplateDir(String) templateDir}</td><td>rekenbox template directory on server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputOutputDirectory(String) inputOutputDirectory}</td><td>directory on server where input and output files are (temporarily) stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCommandLineType(String) commandLineType}</td><td>Format of commandline of rekenbox. Possible values 
 * <ul>
 *   <li>"switches": rekenbox is called like: rekenbox.exe /IinputFileName /UoutputFileName /PtemplateDir</li>
 *   <li>"straigth": rekenbox is called like: rekenbox.exe inputFileName outputFileName templateDir;</li>
 * </ul></td><td>"straigth"</td></tr>
 * <tr><td>{@link #setExecutableExtension(String) executableExtension}</td><td>extension of rekenbox-executable</td><td>exe</td></tr>
 * <tr><td>{@link #setCleanup(boolean) cleanup}</td><td>if true, input and output files are removed after the call to the rekenbox is finished</td><td>true</td></tr>
 * <tr><td>{@link #setCommandLineType(String) commandLineType}</td><td>name of rekenbox to be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRekenboxSessionKey(String) rekenboxSessionKey}</td><td>key in {@link pipeSession} to store rekenbox name in</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 *
 * <p><b>Note:</b><br>
 * The rekenbox-name is currently determined from the first 8 characters of the file, or up
 * to the first space (' ') or colon (':') character. Beware that if the first character of the
 * file is a newline character or something similar, less characters are available to pass the
 * rekenbox-name on. Especially if the inputmessages are constructed by means of an XSLT-stylesheet,
 * messages often start with a newline character.
 * </p>
 * @author Gerrit van Brakel
 * @version Id
 */
public class RekenBoxCaller extends FixedForwardPipe {
	private String runPath;
	private String executableExtension="exe"; //bat, com or exe
	private String inputOutputDirectory;
	private String templateDir;
	private boolean cleanup=true;
	private String commandLineType="straigth";
	public static final String version="$Id: RekenBoxCaller.java,v 1.1 2004-03-11 08:13:38 NNVZNL01#L180564 Exp $";
/**
 * output-property to communicate the name of the rekenbox to adios2Xml converter
 */
private String rekenboxSessionKey=null;
/**
 * RekenBoxCaller constructor comment.
 */
public RekenBoxCaller() {
	super();
}
public void configure() throws ConfigurationException {
	super.configure();
}
/**
 * positie 1 t/m 8 bepalen de naam van de executable
 */
public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
    if (!(input instanceof String))
        throw new PipeRunException(this, 
            "expected java.lang.String, got [" + input.getClass().getName() + "], value ["+input+"]");
    String sInput = (String) input;
//	log.debug("Pipe ["+name+"] got input ["+sInput+"]");


	String rekenboxName = sInput;
	if (rekenboxName.length()>=8)
		rekenboxName=rekenboxName.substring(0,8);
	// find end or position of first colon.
	rekenboxName = (rekenboxName+":").substring(0, (rekenboxName+":").indexOf(":")).trim();

	if (rekenboxName.equals("")) {
		throw new PipeRunException(this, "Pipe ["+getName()+"] cannot determine rekenboxName from ["+sInput+"]");
	}
	
    String exeName = runPath+rekenboxName+ "."+executableExtension;
	if (!(new File(exeName).exists())) {
		throw new PipeRunException(this, "Pipe ["+getName()+"] executable file ["+exeName+"] does not exist; requestmessage: ["+sInput+"]");
	}

	if (getRekenboxSessionKey() != null) {
        session.put(getRekenboxSessionKey(),rekenboxName);
    }

    String baseFileName=Misc.createSimpleUUID();
    String inputFileName=inputOutputDirectory+baseFileName+".INV";
    String outputFileName=inputOutputDirectory+baseFileName+".UIT";

    String callAndArgs;
    
    String callType = getCommandLineType();
    if ((callType==null) || (callType.equals("switches")))  {
      callAndArgs=exeName+" /I"+inputFileName + " /U"+outputFileName + " /P"+templateDir;
    }
    else
	  if (callType.equals("straigth"))  {
        callAndArgs=exeName+" "+inputFileName + " "+outputFileName + " "+templateDir;
    }
	else
	  throw new PipeRunException(this, "unknown commandLineType: "+callType);

    try {
        // put input in a file
		Misc.stringToFile( sInput, inputFileName);
		
		log.debug(getLogPrefix(session)+" will issue command ["+callAndArgs+"]");
  
		// execute 
        Runtime rt = Runtime.getRuntime();

        Process child = rt.exec(callAndArgs);
        child.waitFor();
        log.debug(getLogPrefix(session)+" completed call. Process exit code is: " + child.exitValue());
        
        // read output
		String result=Misc.fileToString(outputFileName, "\n", true);
		
//		log.debug("Pipe ["+name+"] retrieved result ["+result+"]");
	    return new PipeRunResult(getForward(), result);
		
		} catch (IOException e) {
        log.error("Pipe ["+getName()+"] got IOException!", e);
        throw new PipeRunException(this, "Pipe ["+getName()+"] got IOException!", e);
    } catch (InterruptedException e) {
        log.error("Interrupted waiting for process!");
        throw new PipeRunException(this, getName() + "got InterruptedException", e);
    } catch (Exception e) {
	   	log.error("Pipe ["+getName()+"] got Exception : ",e);
	   	throw new PipeRunException(this, getName() + "got Exception", e);
    } finally {
		// cleanup
		if (isCleanup()){
			new File(inputFileName).delete();
			new File(outputFileName).delete();
		}
    }
    
}
/**
 * Insert the method's description here.
 * Creation date: (15-07-2003 10:52:01)
 * @return java.lang.String
 */
public java.lang.String getCommandLineType() {
	return commandLineType;
}
/**
 * Insert the method's description here.
 * Creation date: (16-04-2003 8:38:31)
 * @return java.lang.String
 */
public java.lang.String getExecutableExtension() {
	return executableExtension;
}
/**
 * Insert the method's description here.
 * Creation date: (16-04-2003 8:38:31)
 * @return java.lang.String
 */
public java.lang.String getInputOutputDirectory() {
	return inputOutputDirectory;
}
/**
 * Insert the method's description here.
 * Creation date: (20-08-2003 11:25:21)
 * @return java.lang.String
 */
public java.lang.String getRekenboxSessionKey() {
	return rekenboxSessionKey;
}
public java.lang.String getRunPath() {
	return runPath;
}
public java.lang.String getTemplateDir() {
	return templateDir;
}
public boolean isCleanup() {
	return cleanup;
}
/**
 * Insert the method's description here.
 * Creation date: (16-04-2003 11:15:10)
 * @param newCleanup boolean
 */
public void setCleanup(boolean newCleanup) {
	cleanup = newCleanup;
}
/**
 * Insert the method's description here.
 * Creation date: (15-07-2003 10:52:01)
 * @param newCommandLineType java.lang.String
 */
public void setCommandLineType(java.lang.String newCommandLineType) {
	commandLineType = newCommandLineType;
}
public void setExecutableExtension(String newExecutableExtension) {
	executableExtension = newExecutableExtension;
}
/**
 * Insert the method's description here.
 * Creation date: (16-04-2003 8:38:31)
 * @param newInputOutputDirectory java.lang.String
 */
public void setInputOutputDirectory(java.lang.String newInputOutputDirectory) {
	inputOutputDirectory = newInputOutputDirectory;
}
/**
 * Insert the method's description here.
 * Creation date: (20-08-2003 11:25:21)
 * @param newRekenboxSessionKey java.lang.String
 */
public void setRekenboxSessionKey(java.lang.String newRekenboxSessionKey) {
	rekenboxSessionKey = newRekenboxSessionKey;
}
/**
 * Insert the method's description here.
 * Creation date: (16-04-2003 8:38:31)
 * @param newRunPath java.lang.String
 */
public void setRunPath(java.lang.String newRunPath) {
	runPath = newRunPath;
}
public void setTemplateDir(java.lang.String newTemplateDir) {
	templateDir = newTemplateDir;
}
}
