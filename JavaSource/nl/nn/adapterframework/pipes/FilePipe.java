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
import nl.nn.adapterframework.util.FileHandler;


/**
 * <p>See {@link FileHandler}</p>
 * 
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * @author J. Dekker
 * @author Jaco de Groot (***@dynasol.nl)
 * @version $Id$
 *
 */
public class FilePipe extends FixedForwardPipe {
	FileHandler fileHandler;

	FilePipe() {
		fileHandler = new FileHandler();
	}
	
	public void configure() throws ConfigurationException {
		super.configure();
		fileHandler.configure();
	}
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		try {
			return new PipeRunResult(getForward(), fileHandler.handle(input, session));
		}
		catch(Exception e) {
			throw new PipeRunException(this, getLogPrefix(session)+"Error while executing file action(s)", e); 
		}
	}

	public void setCharset(String charset) {
		fileHandler.setCharset(charset);
	}

	public void setActions(String actions) {
		fileHandler.setActions(actions);
	}

	public void setDirectory(String directory) {
		fileHandler.setDirectory(directory);
	}

	public void setWriteSuffix(String suffix) {
		fileHandler.setWriteSuffix(suffix);
	}

	public void setFileName(String filename) {
		fileHandler.setFileName(filename);
	}

	public void setFileNameSessionKey(String filenameSessionKey) {
		fileHandler.setFileNameSessionKey(filenameSessionKey);
	}

	public void setCreateDirectory(boolean b) {
		fileHandler.setCreateDirectory(b);
	}

	public void setWriteLineSeparator(boolean b) {
		fileHandler.setWriteLineSeparator(b);
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
}
