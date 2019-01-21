package nl.nn.adapterframework.filesystem;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.parameters.ParameterList;

public interface IFileHandler {
	public void configure() throws ConfigurationException;

	public Object handle(Object input, IPipeLineSession session, ParameterList paramList) throws Exception;
	
	public void setActions(String actions);
	public void setCharset(String charset);
	public void setOutputType(String outputType);

	public void setFileSource(String fileSource);
	
	/**
	 * @param directory in which the file resides or has to be created
	 */
	public void setDirectory(String directory);

	/**
	 * @param suffix of the file that is written
	 */
//	public void setWriteSuffix(String suffix);

	/**
	 * @param filename of the file that is written
	 */
	public void setFileName(String filename);

	public void setWriteSuffix(String suffix);
	
	/**
	 * @param filenameSessionKey the session key that contains the name of the file to be created
	 */
	public void setFileNameSessionKey(String filenameSessionKey);

	public void setCreateDirectory(boolean b);

	public void setWriteLineSeparator(boolean b);
	public void setTestCanWrite(boolean b);

	public void setSkipBOM(boolean b);
	public void setDeleteEmptyDirectory(boolean b);
	public void setStreamResultToServlet(boolean b);

}
