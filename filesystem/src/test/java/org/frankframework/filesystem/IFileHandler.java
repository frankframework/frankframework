package org.frankframework.filesystem;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;

public interface IFileHandler {
	void configure() throws ConfigurationException;

	Object handle(Message input, PipeLineSession session, ParameterList paramList) throws Exception;

	void setActions(String actions);
	void setCharset(String charset);
	void setOutputType(String outputType);

	void setFileSource(String fileSource);

	/**
	 * @param directory in which the file resides or has to be created
	 */
	void setDirectory(String directory);

	/**
	 * @param filename of the file that is written
	 */
	void setFileName(String filename);

	void setWriteSuffix(String suffix);

	/**
	 * @param filenameSessionKey the session key that contains the name of the file to be created
	 */
	void setFileNameSessionKey(String filenameSessionKey);

	void setCreateDirectory(boolean b);

	void setWriteLineSeparator(boolean b);
	void setTestCanWrite(boolean b);

	void setSkipBOM(boolean b);
	void setDeleteEmptyDirectory(boolean b);
	void setStreamResultToServlet(boolean b);

}
