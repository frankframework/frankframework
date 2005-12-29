/*
 * $Log: RekenBoxCaller.java,v $
 * Revision 1.9  2005-12-29 15:15:03  europe\L190409
 * corrected javadoc
 *
 * Revision 1.8  2004/10/05 10:40:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved filename generation
 *
 * Revision 1.7  2004/09/01 11:17:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unnecessary code
 *
 * Revision 1.6  2004/09/01 08:18:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * create output file in advance
 *
 * Revision 1.5  2004/08/17 15:48:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added 'redirected' commandlinetype
 * added precreation of outputfile, required for L76HB000
 *
 * Revision 1.4  2004/08/09 13:57:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved determination of rekenbox-name
 *
 * Revision 1.3  2004/03/26 09:50:52  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 * Revision 1.2  2004/03/24 15:30:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cleaned up unused comments
 *
 */
package nl.nn.adapterframework.extensions.rekenbox;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.Counter;
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
 * <tr><td>{@link #setRekenBoxName(String) rekenBoxName}</td><td>fixed name of the rekenbox (or wrapper) to be called. If empty, the name is determined from the request</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRunPath(String) runPath}</td><td>directory on server where rekenbox-executable can be found</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTemplateDir(String) templateDir}</td><td>rekenbox template directory on server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputOutputDirectory(String) inputOutputDirectory}</td><td>directory on server where input and output files are (temporarily) stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCommandLineType(String) commandLineType}</td><td>Format of commandline of rekenbox. Possible values 
 * <ul>
 *   <li>"straight": rekenbox is called like: rekenbox.exe inputFileName outputFileName templateDir</li>
 *   <li>"switches": rekenbox is called like: rekenbox.exe /IinputFileName /UoutputFileName /PtemplateDir</li>
 *   <li>"redirected": rekenbox is called like: rekenbox.exe inputFileName templateDir > outputFileName; (This method has not been fully tested)</li>
 * </ul></td><td>"straigth"</td></tr>
 * <tr><td>{@link #setExecutableExtension(String) executableExtension}</td><td>extension of rekenbox-executable</td><td>exe</td></tr>
 * <tr><td>{@link #setCleanup(boolean) cleanup}</td><td>if true, input and output files are removed after the call to the rekenbox is finished</td><td>true</td></tr>
 * <tr><td>{@link #setRekenboxSessionKey(String) rekenboxSessionKey}</td><td>key in {@link nl.nn.adapterframework.core.PipeLineSession pipeLineSession} to store rekenbox name in</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDataFilenamePrefix(String) dataFilenamePrefix}</td><td>first part of filenames that communicate requests and replies to rekenbox</td><td>rb</td></tr>
 * <tr><td>{@link #setMaxRequestNumber(long) maxRequestNumber}</td><td>maximal number that will be concatenated to dataFilenamePrefix</td><td>1000</td></tr>
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
	public static final String version="$RCSfile: RekenBoxCaller.java,v $ $Revision: 1.9 $ $Date: 2005-12-29 15:15:03 $";
	
	private String runPath="";
	private String executableExtension="exe"; //bat, com or exe
	private String inputOutputDirectory;
	private String templateDir;
	private String rekenBoxName; // can be set for fixed rekenbox
	private boolean cleanup=true;
	private String commandLineType="straight";
	private String rekenboxSessionKey=null; // output-property to communicate the name of the rekenbox to adios2Xml converter
	
	private String dataFilenamePrefix ="rb";
	private long maxRequestNumber=1000;
	private NumberFormat formatter;
	private static Counter requestCounter = new Counter(0);

	private File inputOutputDir;

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getCommandLineType()) || 
			!(getCommandLineType().equals("straight") || 
			  getCommandLineType().equals("switches") || 
			  getCommandLineType().equals("redirected"))) {
			  	throw new ConfigurationException(getLogPrefix(null)+"commandLineType ["+getCommandLineType()+"] must be one of 'straigth', 'switches' or 'redirected'");
			  }
		inputOutputDir= new File(getInputOutputDirectory());
		if (!inputOutputDir.exists()) {
			throw new ConfigurationException(getLogPrefix(null)+"inputOutputDirectory ["+getInputOutputDirectory()+"] does not exist");
		}
		if (!inputOutputDir.isDirectory()) {
			throw new ConfigurationException(getLogPrefix(null)+"inputOutputDirectory ["+getInputOutputDirectory()+"] is not a directory");
		}
		formatter = new DecimalFormat("000000000000".substring(0,Long.toString(getMaxRequestNumber()).length()));
		String baseFileName=getBaseFileName();
		log.debug(getLogPrefix(null)+"first filename will be ["+baseFileName+"]");
		requestCounter.decrease();
	}
	
	protected boolean inputFileExists(long requestno,String extension) {
		return new File(inputOutputDir,makeFileName(requestno,extension)).exists();
	}

	protected String makeFileName(long requestno, String extension) {
		return getDataFilenamePrefix() + formatter.format(requestno)+extension;
	}
	
	public String getBaseFileName() {
		boolean didReset=false;
		long requestno;
		
		for(requestno=requestCounter.increase(); inputFileExists(requestno,".INV"); requestno=requestCounter.increase()) {
			if (!didReset && requestno>=getMaxRequestNumber()) {
				synchronized (requestCounter){
					if (requestCounter.getValue()>=getMaxRequestNumber())	{
						requestCounter.decrease(getMaxRequestNumber());
					}
				}
			}
		}
		return makeFileName(requestno,"");
	}

	/**
	 * positie 1 t/m 8 bepalen de naam van de executable, of tot aan de ':' (wat het eerst komt)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
	    if (!(input instanceof String))
	        throw new PipeRunException(this, 
	            getLogPrefix(session)+"expected java.lang.String, got [" + input.getClass().getName() + "], value ["+input+"]");
	    String sInput = (String) input;
	//	log.debug("Pipe ["+name+"] got input ["+sInput+"]");
	
	
		String rekenboxName=getRekenBoxName();
		if (StringUtils.isEmpty(rekenboxName)) {
			rekenboxName = sInput;
			if (rekenboxName.length()>=8)
				rekenboxName=rekenboxName.substring(0,8);
			// find end or position of first colon.
			rekenboxName = (rekenboxName+":").substring(0, (rekenboxName+":").indexOf(":")).trim();
		}
	
		if (rekenboxName.equals("")) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot determine rekenboxName from ["+sInput+"]");
		}
		
		int i=rekenboxName.length();
		int n=sInput.length();
		while (i<n && " :;".indexOf(sInput.charAt(i))>=0) {
			i++;
		}
		String rekenboxInput = sInput.substring(i);
		
	    String exeName = runPath+rekenboxName+ "."+executableExtension;
		if (!(new File(exeName).exists())) {
			throw new PipeRunException(this, getLogPrefix(session)+"executable file ["+exeName+"] does not exist; requestmessage: ["+sInput+"]");
		}
	
		if (getRekenboxSessionKey() != null) {
	        session.put(getRekenboxSessionKey(),rekenboxName);
	    }

	    String baseFileName=getBaseFileName();
	    String inputFileName=inputOutputDirectory+baseFileName+".INV";
	    String outputFileName=inputOutputDirectory+baseFileName+".UIT";
	    
	    String callAndArgs;
		String callType = getCommandLineType();
		if ((callType==null) || (callType.equals("switches")))  {
			callAndArgs =  exeName+" /I"+inputFileName + " /U"+outputFileName + " /P"+templateDir;
		}
		else
		  if (callType.equals("straight"))  {
			callAndArgs =  exeName+" "+inputFileName + " "+outputFileName + " "+templateDir;
		}
		else
		  if (callType.equals("redirected"))  {
			callAndArgs =  exeName+" "+inputFileName + " "+templateDir;
		}
		else
		  throw new PipeRunException(this, getLogPrefix(session)+"unknown commandLineType: "+callType);
	
	    try {
	        // put input in a file
			Misc.stringToFile( rekenboxInput, inputFileName);

			// precreating outputfile is necessary for L76HB000
			log.debug(getLogPrefix(session)+" precreating outputfile ["+outputFileName+"]");
			new File(outputFileName).createNewFile();
			
			log.debug(getLogPrefix(session)+" will issue command ["+callAndArgs+"]");
	  
			// execute 
	        Runtime rt = Runtime.getRuntime();
	
	        Process child = rt.exec(callAndArgs);
			String result;
			
			if (callType.equals("redirected"))  {
				result=Misc.streamToString(child.getInputStream(),"\n", true);
				
			} else {
				child.waitFor();
	        
				// read output
				result=Misc.fileToString(outputFileName, "\n", true);
			}
			log.debug(getLogPrefix(session)+" completed call. Process exit code is: " + child.exitValue());
	        
			
	//		log.debug("Pipe ["+name+"] retrieved result ["+result+"]");
		    return new PipeRunResult(getForward(), result);
			
	    } catch (Exception e) {
		   	throw new PipeRunException(this, getLogPrefix(session)+"got Exception executing rekenbox", e);
	    } finally {
			// cleanup
			if (isCleanup()){
				new File(inputFileName).delete();
				new File(outputFileName).delete();
			}
	    }
	    
	}

	public void setRekenBoxName(String string) {
		rekenBoxName = string;
	}
	public String getRekenBoxName() {
		return rekenBoxName;
	}

	public void setRunPath(String newRunPath) {
		runPath = newRunPath;
	}
	public String getRunPath() {
		return runPath;
	}

	public void setTemplateDir(String newTemplateDir) {
		templateDir = newTemplateDir;
	}
	public String getTemplateDir() {
		return templateDir;
	}

	public void setInputOutputDirectory(String newInputOutputDirectory) {
		inputOutputDirectory = newInputOutputDirectory;
	}
	public String getInputOutputDirectory() {
		return inputOutputDirectory;
	}

	public void setCommandLineType(String newCommandLineType) {
		commandLineType = newCommandLineType;
	}
	public String getCommandLineType() {
		return commandLineType;
	}
	
	public void setExecutableExtension(String newExecutableExtension) {
		executableExtension = newExecutableExtension;
	}
	public String getExecutableExtension() {
		return executableExtension;
	}

	public void setCleanup(boolean newCleanup) {
		cleanup = newCleanup;
	}
	public boolean isCleanup() {
		return cleanup;
	}
	
	public void setRekenboxSessionKey(String newRekenboxSessionKey) {
		rekenboxSessionKey = newRekenboxSessionKey;
	}
	public String getRekenboxSessionKey() {
		return rekenboxSessionKey;
	}

	public void setDataFilenamePrefix(String string) {
		dataFilenamePrefix = string;
	}
	public String getDataFilenamePrefix() {
		return dataFilenamePrefix;
	}

	public void setMaxRequestNumber(long l) {
		maxRequestNumber = l;
	}
	public long getMaxRequestNumber() {
		return maxRequestNumber;
	}


}
