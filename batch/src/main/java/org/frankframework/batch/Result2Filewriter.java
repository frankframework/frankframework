/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
import java.io.FileWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.core.PipeLineSession;
import org.frankframework.util.FileUtils;

/**
 * Resulthandler that writes the transformed record to a file.
 *
 * @author  John Dekker
 * @deprecated Warning: non-maintained functionality.
 */
public class Result2Filewriter extends ResultWriter {

	private @Getter String outputDirectory;
	private @Getter String move2dirAfterFinalize;
	private @Getter String filenamePattern;

	private final Map<String, File> openFiles = Collections.synchronizedMap(new HashMap<>());

	public Result2Filewriter() {
		super();
		setOnOpenDocument("");
		setOnCloseDocument("");
	}

	@Override
	protected Writer createWriter(PipeLineSession session, String streamId) throws Exception {
		log.debug("create writer [{}]", streamId);
		String outputFilename = FileUtils.getFilename(null, session, new File(streamId), getFilenamePattern());
		File outputFile = new File(outputDirectory, outputFilename);
		if (outputFile.exists() && outputFile.isFile()) {
			log.warn("Outputfile {} exists in {}", outputFilename, outputDirectory);
		}
		openFiles.put(streamId,outputFile);
		return new FileWriter(outputFile, false);
	}

	@Override
	public void closeDocument(PipeLineSession session, String streamId) {
		openFiles.remove(streamId);
	}

	@Override
	public String finalizeResult(PipeLineSession session, String streamId, boolean error) throws Exception {
		log.debug("finalizeResult [{}]", streamId);
		super.finalizeResult(session,streamId, error);
		super.closeDocument(session,streamId);

		File file = openFiles.get(streamId);
		if (file==null) {
			return null;
		}
		if (error) {
			file.delete();
			return null;
		}
		if (!StringUtils.isEmpty(getMove2dirAfterFinalize())) {
			File movedFile = new File(getMove2dirAfterFinalize(), file.getName());
			if (movedFile.exists()) {
				log.warn("File [{}] exists, file gets overwritten", movedFile.getAbsolutePath());
				movedFile.delete();
			}
			file.renameTo(movedFile);
			return movedFile.getAbsolutePath();
		}
		return file.getAbsolutePath();
	}


	/** Directory in which the resultfile must be stored */
	public void setOutputDirectory(String string) {
		outputDirectory = string;
	}

	/** Directory to which the created file must be moved after finalization (is optional) */
	public void setMove2dirAfterFinalize(String string) {
		move2dirAfterFinalize = string;
	}

	/** Name of the file is created using the messageformat. Params: 1=inputfilename, 2=extension of file, 3=current date */
	public void setFilenamePattern(String string) {
		filenamePattern = string;
	}

}
