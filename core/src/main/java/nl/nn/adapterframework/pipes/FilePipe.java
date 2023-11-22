/*
   Copyright 2013, 2015, 2018, 2020 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.FileHandler;


/**
 * <p>See {@link FileHandler}</p>
 *
 * @author J. Dekker
 * @author Jaco de Groot (***@dynasol.nl)
 *
 * @deprecated Please use LocalFileSystemPipe instead
 *
 */
@Deprecated
@ConfigurationWarning("Please use LocalFileSystemPipe instead, or when retrieving files from the classpath use the FixedResultPipe")
public class FilePipe extends FixedForwardPipe {
	FileHandler fileHandler;

	public FilePipe() {
		fileHandler = new FileHandler();
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		fileHandler.configure();
	}

	/**
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Message, PipeLineSession)
	 */
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			return new PipeRunResult(getSuccessForward(), fileHandler.handle(message, session, getParameterList()));
		}
		catch(Exception e) {
			if (findForward(PipeForward.EXCEPTION_FORWARD_NAME) != null) {
				return new PipeRunResult(findForward(PipeForward.EXCEPTION_FORWARD_NAME), message);
			}
			throw new PipeRunException(this, "Error while executing file action(s)", e);
		}
	}

	public void setCharset(String charset) {
		fileHandler.setCharset(charset);
	}

	public void setOutputType(String outputType) {
		fileHandler.setOutputType(outputType);
	}

	public void setActions(String actions) {
		fileHandler.setActions(actions);
	}

	public void setFileSource(String fileSource) {
		fileHandler.setFileSource(fileSource);
	}

	public void setDirectory(String directory) {
		fileHandler.setDirectory(directory);
	}

	public void setWriteSuffix(String suffix) {
		fileHandler.setWriteSuffix(suffix);
	}

	public void setFilename(String filename) {
		fileHandler.setFilename(filename);
	}

	public void setFilenameSessionKey(String filenameSessionKey) {
		fileHandler.setFilenameSessionKey(filenameSessionKey);
	}

	public void setCreateDirectory(boolean b) {
		fileHandler.setCreateDirectory(b);
	}

	public void setWriteLineSeparator(boolean b) {
		fileHandler.setWriteLineSeparator(b);
	}

	public void setTestExists(boolean b) {
		fileHandler.setTestExists(b);
	}

	public void setTestCanWrite(boolean b) {
		fileHandler.setTestCanWrite(b);
	}

	public void setSkipBOM(boolean b) {
		fileHandler.setSkipBOM(b);
	}

	public void setDeleteEmptyDirectory(boolean b) {
		fileHandler.setDeleteEmptyDirectory(b);
	}

	public void setStreamResultToServlet(boolean b) {
		fileHandler.setStreamResultToServlet(b);
	}
}
