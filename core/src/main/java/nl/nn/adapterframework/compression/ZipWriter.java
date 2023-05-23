/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package nl.nn.adapterframework.compression;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.collection.CollectionException;
import nl.nn.adapterframework.collection.CollectorPipeBase.Action;
import nl.nn.adapterframework.collection.ICollector;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.PathMessage;
import nl.nn.adapterframework.util.FileUtils;

public class ZipWriter implements ICollector<MessageZipEntry> {

	static final String PARAMETER_FILENAME="filename";
	static final String PARAMETER_CONTENTS="contents";

	private final boolean includeFileHeaders;

	static void configure(Action action, ParameterList parameterList) throws ConfigurationException {
		switch (action) {
			case OPEN:
				break;
			case WRITE:
			case LAST:
				if(parameterList == null) {
					throw new ConfigurationException("parameter '"+PARAMETER_FILENAME+"' or parameter '"+PARAMETER_CONTENTS+"' is required");
				}
				Parameter filenameParameter=parameterList.findParameter(PARAMETER_FILENAME);
				Parameter contentsParameter=parameterList.findParameter(PARAMETER_CONTENTS);
				if (filenameParameter==null && contentsParameter==null) {
					throw new ConfigurationException("parameter '"+PARAMETER_FILENAME+"' or parameter '"+PARAMETER_CONTENTS+"' is required");
				}
				break;
			case CLOSE:
				if (parameterList != null && parameterList.findParameter(PARAMETER_FILENAME)!=null) {
					throw new ConfigurationException("parameter '"+PARAMETER_FILENAME+"' cannot not be configured on action [close]");
				}
				break;
			default:
				throw new ConfigurationException("unknwon action ["+action+"]");
		}
	}

	public ZipWriter(boolean includeFileHeaders) {
		this.includeFileHeaders = includeFileHeaders;
	}

	@Override
	public MessageZipEntry createPart(Message input, PipeLineSession session, ParameterValueList pvl) throws CollectionException {
		String filename = ParameterValueList.getValue(pvl, PARAMETER_FILENAME, "");
		try {
			Message contents = ParameterValueList.getValue(pvl, PARAMETER_CONTENTS, input);
			if (StringUtils.isEmpty(filename) && contents != input) {
				filename = input.asString();
			}
			MessageZipEntry entry = new MessageZipEntry(contents, filename);
			if (includeFileHeaders) {
				entry.computeFileHeaders();
			}
			return entry;
		} catch (IOException e) {
			throw new CollectionException("cannot write item", e);
		}
	}

	@Override
	public Message build(List<MessageZipEntry> parts) throws IOException {
		File collectorsTempFolder = FileUtils.getTempDirectory("collectors");
		File tempFile = File.createTempFile("msg", ".dat", collectorsTempFolder);

		try (FileOutputStream fos = new FileOutputStream(tempFile)) {
			try (ZipOutputStream zipoutput = new ZipOutputStream(fos)) {
				for(MessageZipEntry entry : parts) {
					entry.writeEntry(zipoutput);
				}
			}
		}

		return PathMessage.asTemporaryMessage(tempFile.toPath());
	}

	@Override
	public void close() throws Exception {
		// nothing to close
	}
}
