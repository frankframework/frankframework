/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.adapterframework.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import lombok.Getter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.FileUtils;

/**
 * Pipe for transforming a (batch)file with records. Records in the file must be separated
 * with new line characters.
 * You can use the &lt;child&gt; tag to register RecordHandlers, RecordHandlerManagers, ResultHandlers
 * and RecordHandlingFlow elements. This is deprecated, however. Since 4.7 one should use &lt;manager&gt;,
 * &lt;recordHandler&gt;, &lt;resultHandler&gt; and &lt;flow&gt;
 * 
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link IReaderFactory readerFactory}</td><td>Factory for reader of inputstream. Default implementation {@link InputStreamReaderFactory} just converts using the specified characterset</td></tr>
 * <tr><td>{@link IRecordHandlerManager manager}</td><td>Manager determines which handlers are to be used for the current line.
 * 			If no manager is specified, a default manager and flow are created. The default manager 
 * 			always uses the default flow. The default flow always uses the first registered recordHandler 
 * 			(if available) and the first registered resultHandler (if available).</td></tr>
 * <tr><td>{@link RecordHandlingFlow manager/flow}</td><td>Element that contains the handlers for a specific record type, to be assigned to the manager</td></tr>
 * <tr><td>{@link IRecordHandler recordHandler}</td><td>Handler for transforming records of a specific type</td></tr>
 * <tr><td>{@link IResultHandler resultHandler}</td><td>Handler for processing transformed records</td></tr>
 * </table>
 * </p>
 * 
 * 
 * For files containing only a single type of lines, a simpler configuration without managers and flows
 * can be specified. A single recordHandler with key="*" and (optional) a single resultHandler need to be specified.
 * Each line will be handled by this recordHandler and resultHandler.
 * 
 * @author  John Dekker
 */
public class BatchFileTransformerPipe extends StreamTransformerPipe {

	private @Getter String move2dirAfterTransform;
	private @Getter String move2dirAfterError;
	private @Getter int numberOfBackups = 5;
	private @Getter boolean overwrite = false;
	private @Getter boolean delete = false;

	@Override
	protected String getStreamId(Message input, PipeLineSession session) {
		String filename = (String)input.asObject();
		File file = new File(filename);
		return file.getName();
	}
	@Override
	protected InputStream getInputStream(String streamId, Message input, PipeLineSession session) throws PipeRunException {
		try {
			String filename	= (String)input.asObject();
			File file = new File(filename);
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new PipeRunException(this,"cannot find file ["+streamId+"]",e);
		}
	}
	

	/**
	 * Open a reader for the file named according the input messsage and 
	 * transform it.
	 * Move the input file to a done directory when transformation is finished
	 * and return the names of the generated files. 
	 * 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Message, PipeLineSession)
	 */
	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this,"got null input instead of String containing filename");
		}
		
		if (!(input.asObject() instanceof String)) {
			throw new PipeRunException(this,"expected String containing filename as input, got ["+ClassUtils.nameOf(input)+"], value ["+input+"]");
		}
		String filename	= (String)input.asObject();
		File file = new File(filename);

		try {
			PipeRunResult result = super.doPipe(input,session);
			try {
				FileUtils.moveFileAfterProcessing(file, getMove2dirAfterTransform(), isDelete(),isOverwrite(), getNumberOfBackups()); 
			} catch (Exception e) {
				log.error(getLogPrefix(session),e);
			}
			return result;
		} catch (PipeRunException e) {
			try {
				FileUtils.moveFileAfterProcessing(file, getMove2dirAfterError(), isDelete(),isOverwrite(), getNumberOfBackups()); 
			} catch (Exception e2) {
				log.error(getLogPrefix(session)+"Could not move file after exception ["+e2+"]");
			}
			throw e;
		}
	}

	
	@IbisDoc({"1", "Directory in which the transformed file(s) is stored", ""})
	public void setMove2dirAfterTransform(String readyDir) {
		move2dirAfterTransform = readyDir;
	}

	@IbisDoc({"2", "Directory to which the inputfile is moved in case an error occurs", ""})
	public void setMove2dirAfterError(String errorDir) {
		move2dirAfterError = errorDir;
	}


	@IbisDoc({"3", "Number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.", "5"})
	public void setNumberOfBackups(int i) {
		numberOfBackups = i;
	}

	@IbisDoc({"4", "If set <code>true</code>, the destination file will be deleted if it already exists", "false"})
	public void setOverwrite(boolean b) {
		overwrite = b;
	}

	@IbisDoc({"5", "If set <code>true</code>, the file processed will deleted after being processed, and not stored", "false"})
	public void setDelete(boolean b) {
		delete = b;
	}
}
