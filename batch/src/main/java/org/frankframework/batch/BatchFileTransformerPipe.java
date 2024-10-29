/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021-2022 WeAreFrank!

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
package org.frankframework.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.util.FileUtils;

/**
 * Pipe for transforming a (batch)file with records. Records in the file must be separated
 * with new line characters.
 * You can use the &lt;child&gt; tag to register RecordHandlers, RecordHandlerManagers, ResultHandlers
 * and RecordHandlingFlow elements. This is deprecated, however. Since 4.7 one should use &lt;manager&gt;,
 * &lt;recordHandler&gt;, &lt;resultHandler&gt; and &lt;flow&gt;
 *
 * For files containing only a single type of lines, a simpler configuration without managers and flows
 * can be specified. A single recordHandler with key="*" and (optional) a single resultHandler need to be specified.
 * Each line will be handled by this recordHandler and resultHandler.
 *
 * @author  John Dekker
 * @deprecated Warning: non-maintained functionality.
 */
public class BatchFileTransformerPipe extends StreamTransformerPipe {

	private @Getter String move2dirAfterTransform;
	private @Getter String move2dirAfterError;
	private @Getter int numberOfBackups = 5;
	private @Getter boolean overwrite = false;
	private @Getter boolean delete = false;

	@Override
	protected String getStreamId(Message input, PipeLineSession session) {
		String filename = null;
		try {
			filename = input.asString();
		} catch (IOException e) {
			log.error("Could not read message [{}] as String", input, e);
		}
		File file = new File(filename);
		return file.getName();
	}

	@Override
	protected InputStream getInputStream(String streamId, Message input, PipeLineSession session) throws PipeRunException {
		try {
			String filename = input.asString();
			File file = new File(filename);
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new PipeRunException(this,"cannot find file ["+streamId+"]",e);
		} catch (IOException e) {
			throw new PipeRunException(this, "could not read message ["+input+"] as String", e);
		}
	}

	/**
	 * Open a reader for the file named according the input message and transform it.
	 * Move the input file to a done directory when transformation is finished
	 * and return the names of the generated files.
	 *
	 * @see IPipe#doPipe(Message, PipeLineSession)
	 */
	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this,"got null input instead of String containing filename");
		}

		String filename;
		try {
			filename = input.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "could not read message ["+input+"] as String", e);
		}
		File file = new File(filename);

		try {
			PipeRunResult result = super.doPipe(input,session);
			try {
				moveFileAfterProcessing(file, getMove2dirAfterTransform(), isDelete(),isOverwrite(), getNumberOfBackups());
			} catch (Exception e) {
				log.error("Could not move file", e);
			}
			return result;
		} catch (PipeRunException e) {
			try {
				moveFileAfterProcessing(file, getMove2dirAfterError(), isDelete(), isOverwrite(), getNumberOfBackups());
			} catch (Exception e2) {
				log.error("Could not move file after exception [{}]", e2);
			}
			throw e;
		}
	}

	static void moveFileAfterProcessing(File orgFile, String destDir, boolean delete, boolean overwrite, int numBackups) throws InterruptedException, IOException {
		if (!delete) {
			return;
		}
		if (orgFile.exists()) {
			orgFile.delete();
			return;
		}
		if (StringUtils.isNotEmpty(destDir)) {
			File dstFile = new File(destDir, orgFile.getName());
			FileUtils.moveFile(orgFile, dstFile, overwrite, numBackups, 5, 500);
		}
	}

	/** Directory in which the transformed file(s) is stored */
	public void setMove2dirAfterTransform(String readyDir) {
		move2dirAfterTransform = readyDir;
	}

	/** Directory to which the inputfile is moved in case an error occurs */
	public void setMove2dirAfterError(String errorDir) {
		move2dirAfterError = errorDir;
	}

	/**
	 * Number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.
	 * @ff.default 5
	 */
	public void setNumberOfBackups(int i) {
		numberOfBackups = i;
	}

	/**
	 * If set <code>true</code>, the destination file will be deleted if it already exists
	 * @ff.default false
	 */
	public void setOverwrite(boolean b) {
		overwrite = b;
	}

	/**
	 * If set <code>true</code>, the file processed will be deleted after being processed, and not stored
	 * @ff.default false
	 */
	public void setDelete(boolean b) {
		delete = b;
	}
}
