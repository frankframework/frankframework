package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.WildCardFilter;
import nl.nn.adapterframework.util.Misc;
 
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringStyle;
 
/**
 * File {@link nl.nn.adapterframework.core.IPullingListener listener} that looks in a directory for files according to a wildcard. When a file is
 * found, it is read in a String object and parsed to records. When used in a receiver like 
 * {@link FileRecordReceiver } these records are to be processed by the adapter.
 * After reading the file, the file is renamed and moved to a directory.
 * <p>$Id: FileRecordListener.java,v 1.2 2004-02-04 10:02:12 a1909356#db2admin Exp $</p>
 * @author  Johan Verrips
 */
public class FileRecordListener implements IPullingListener, INamedObject {
	public static final String version="$Id: FileRecordListener.java,v 1.2 2004-02-04 10:02:12 a1909356#db2admin Exp $";
 
    private String inputDirectory;
    private String wildcard;
    private long responseTime=1000;
	private String directoryProcessedFiles;
	private String name;
	private ISender sender;
    protected Logger log = Logger.getLogger(this.getClass());
    private long recordNo=0; // the current record number;
    private Iterator recordIterator=null;
    private String inputFileName; // the name of the file currently in process

/**
 * FileReceiver constructor comment.
 */
public FileRecordListener() {
	super();
}
/**
 * If the state is "success", fire the sender.
 */

public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap threadContext) throws ListenerException {
		String cid= (String) threadContext.get("cid");
	    if (sender!=null) {
		    if (processResult.getState().equalsIgnoreCase("success")){
			    try {
				    sender.sendMessage((String)threadContext.get("cid"), processResult.getResult());
			    } catch (SenderException e) {
				    throw new ListenerException("error sending message with correlationId["+cid+" msg [" +processResult.getResult()+"]",e);
			    }catch (TimeOutException e) {
				    throw new ListenerException("error sending message with correlationId["+cid+" msg [" +processResult.getResult()+"]",e);
			    }
		    }
	    }
    }
/**
 * Moves a file to another directory and places a UUID in the name.
 * @return String with the name of the (renamed and moved) file
 * 
 */
protected String archiveFile(File file) throws ListenerException {
    boolean success = false;
    String directoryTo=getDirectoryProcessedFiles();
    String fullFilePath = "";
    // Destination directory
    File dir = new File(directoryTo);
    try {
        fullFilePath = file.getCanonicalPath();
    } catch (IOException e) {
        log.warn(getName()+" error retrieving canonical path of file [" + file.getName() + "]");
        fullFilePath = file.getName();
    }

    if (!dir.isDirectory()) {
        throw new ListenerException(getName()
                +" error renaming directory: The directory ["
                + directoryTo
                + "] to move the file ["
                + fullFilePath
                + "] is not a directory!");
    }
    // Move file to new directory
    String newFileName=Misc.createSimpleUUID()+"-"+file.getName();

	int dotPosition=file.getName().lastIndexOf(".");
	if (dotPosition>0) 
			newFileName=file.getName().substring(0,dotPosition)+"-"+Misc.createSimpleUUID()+file.getName().substring(dotPosition, file.getName().length());


    success = file.renameTo(new File(dir, newFileName));
    if (!success) {
        log.error(
                getName()
                +" was unable to move file ["
                + fullFilePath
                + "] to ["
                + directoryTo
                + "]");
         throw new ListenerException("unable to move file ["+fullFilePath+"] to ["
                + directoryTo
                + "]");
    } else
        log.info(
           getName()+ " moved file [" + fullFilePath + "] to [" + directoryTo + "]");

    String result=null;
	try {
		result=new File(dir, newFileName).getCanonicalPath();
	} catch (IOException e){
         throw new ListenerException("error retrieving canonical path of renamed file ["+file.getName()+"]",e);
	}
	return result;			
        
}
    public void close() throws ListenerException {
	    try {
		    if (sender!=null) sender.close();
	    } catch (SenderException e){
		    throw new ListenerException("Error closing sender ["+sender.getName()+"]", e);
	    }
    }
    public  void closeThread(HashMap threadContext) throws ListenerException {
	   
    }
/**
 * Configure does some basic checks (directoryProcessedFiles is a directory,  inputDirectory is a directory, wildcard is filled etc.);
 *
 */
public void configure() throws ConfigurationException {
        if (StringUtils.isEmpty(getInputDirectory()))
            throw new ConfigurationException("no value specified for [inputDirectory]");
        if (StringUtils.isEmpty(getWildcard()))
            throw new ConfigurationException("no value specified for [wildcard]");
        if (StringUtils.isEmpty(getDirectoryProcessedFiles()))
        	throw new ConfigurationException("no value specified for [directoryProcessedFiles]");
	    File dir = new File(getDirectoryProcessedFiles());
	    if (!dir.isDirectory()) {
	      throw new ConfigurationException("The value for [directoryProcessedFiles] :[ "
		      	+getDirectoryProcessedFiles()
		      	+"] is invalid. It is not a directory ");
	    }
     File inp = new File(getInputDirectory());
	    if (!inp.isDirectory()) {
	      throw new ConfigurationException("The value for [inputDirectory] :[ "
		      	+getInputDirectory()
		      	+"] is invalid. It is not a directory ");
	      
	    }
	    try {
		    if (sender!=null) sender.configure();
	    } catch (ConfigurationException e) {
		    throw new ConfigurationException("error opening sender ["+sender.getName()+"]",e);
	    }
         	
    }
	/**
	 * Returns the directory in whiche processed files are stored.
	 * @return String
	 */
	public String getDirectoryProcessedFiles() {
		return directoryProcessedFiles;
	}
/**
 * Gets a file to process.
 */
protected File getFileToProcess() {
            WildCardFilter filter = new WildCardFilter(wildcard);
            File dir = new File(getInputDirectory());
            File files[] = dir.listFiles(filter);
            int count = (files == null ? 0 : files.length);
            for (int i = 0; i < count; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    continue;
                }
                return(file);
            }
			return null;
    }
/**
 * Returns the name of the file in process (the {@link #archiveFile(File) archived} file) concatenated with the
 * record number. As te {@link #archiveFile(File) archivedFile} method always renames to a 
 * unique file, the combination of this filename and the recordnumber is unique, enabling tracing in case of errors
 * in the processing of the file.
 * Override this method for your specific needs! 
 */
 public String getIdFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
	    String correlationId =  inputFileName+"-"+recordNo;
	     threadContext.put("cid",correlationId);
	    return correlationId;
    }
    public String getInputDirectory() {
        return inputDirectory;
    }
    public String getName() {
      return name;
    }
/**
 * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
 * is a new file to process and returns the first record.
 */
public synchronized Object getRawMessage(HashMap threadContext) throws ListenerException {
	    String fullInputFileName=null;
	    if (recordIterator!=null) {
			if (recordIterator.hasNext()) {
				recordNo+=1;
				return recordIterator.next();
			}
	    }
	    if (getFileToProcess()!=null){
		    File inputFile=getFileToProcess();
		    log.info(" processing file ["+inputFile.getName()+"] size ["+inputFile.length()+"]");
			String fileContent = "";
	        try {
 	           fullInputFileName=inputFile.getCanonicalPath();
 	           fileContent = Misc.fileToString(fullInputFileName, "\n");
 	           inputFileName=archiveFile(inputFile);

 
	        } catch (IOException e) {
   		         throw new ListenerException(" got exception opening " + inputFile.getName(), e);
     		 } finally {
	     		 recordNo=0;
     		 }
			log.info(" processing file ["+fullInputFileName+"]");
		    recordIterator=parseToRecords(fileContent);
		    if (recordIterator.hasNext()) {
				recordNo+=1;
			    return recordIterator.next();
		    }
	    }

	    // if nothing was found, just sleep thight.
	    try {
		    Thread.currentThread().sleep(responseTime);
	    } catch (InterruptedException e){
		    throw new ListenerException("interupted...", e);
		    }
		
	    return null;
    }
    public long getResponseTime() {
	    return responseTime;
    }
	public ISender getSender() {
		return sender;
	}
	/**
	 * Returns a string of the rawMessage
	 */
   public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException{
	    return rawMessage.toString();
    }
 	/**
	 * get the {@link nl.nn.adapterframework.util.WildCardFilter wildcard}  to look for files in the specifiek directory, e.g. "*.inp"
	 */
  public String getWildcard() {
        return wildcard;
    }
    public void open() throws ListenerException {
	    try {
		    if (sender!=null) sender.open();
	    } catch (SenderException e) {
		    throw new ListenerException("error opening sender ["+sender.getName()+"]",e);
	    }
	    return;
    }
   public  HashMap openThread() throws ListenerException{
	
	   return null;
}
	/**
     * Parse a String to an Iterator with objects (records). This method
     * currently uses the end-of-line character ("\n") as a seperator. 
	 * This method is easy to extend to satisfy your project needs.
	 */
	protected Iterator parseToRecords(String input) {
        StringTokenizer t = new StringTokenizer(input, "\n");
        ArrayList array = new ArrayList();
        while (t.hasMoreTokens()) {
            array.add(t.nextToken());
        }
        return array.iterator();
    }
	/**
	 * Sets the directory to store processed files in
	 * @param directoryProcessedFiles The directoryProcessedFiles to set
	 */
	public void setDirectoryProcessedFiles(String directoryProcessedFiles) {
		this.directoryProcessedFiles = directoryProcessedFiles;
	}
/**
 * set the directory name to look for files in.
 * @see #setWildcard(String)
 */
public void setInputDirectory(String inputDirectory) {
        this.inputDirectory = inputDirectory;
    }
    public void setName(String name) {
        this.name = name;
    }
    /**
     * set the time to delay when no records are to be processed and this class has to look for the arrival of a new file
     */
    public void setResponseTime(long responseTime) {
	    this.responseTime=responseTime;
    }
	public void setSender(ISender sender){
		this.sender=sender;
	}
	/**
	 * set the {@link nl.nn.adapterframework.util.WildCardFilter wildcard}  to look for files in the specifiek directory, e.g. "*.inp"
	 */
	public void setWildcard(String wildcard) {
        this.wildcard = wildcard;
    }
    public String toString() {
        String result = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        ts.setDefaultStyle(ToStringStyle.MULTI_LINE_STYLE);
	    ts.append("name", getName() );
	    ts.append("inputDirectory", getInputDirectory());
	    ts.append("wildcard", getWildcard());
        result += ts.toString();
        return result;

    }
}
