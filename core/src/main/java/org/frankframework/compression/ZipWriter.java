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
package org.frankframework.compression;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import org.frankframework.collection.AbstractCollectorPipe.Action;
import org.frankframework.collection.CollectionException;
import org.frankframework.collection.ICollector;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;

public class ZipWriter implements ICollector<MessageZipEntry> {

	private static final MediaType MIMETYPE_ZIP = new MediaType("application", "zip");
	static final String PARAMETER_FILENAME="filename";
	static final String PARAMETER_CONTENTS="contents";

	private final boolean includeFileHeaders;
	private final String zipLocation;

	static void validateParametersForAction(Action action, ParameterList parameterList) throws ConfigurationException {
		switch (action) {
			case OPEN:
				break;
			case WRITE:
			case LAST:
				if(parameterList == null) {
					throw new ConfigurationException("parameter '"+PARAMETER_FILENAME+"' or parameter '"+PARAMETER_CONTENTS+"' is required");
				}
				if (!parameterList.hasParameter(PARAMETER_FILENAME) && !parameterList.hasParameter(PARAMETER_CONTENTS)) {
					throw new ConfigurationException("parameter '"+PARAMETER_FILENAME+"' or parameter '"+PARAMETER_CONTENTS+"' is required");
				}
				break;
			case CLOSE:
				if (parameterList != null && parameterList.hasParameter(PARAMETER_FILENAME)) {
					throw new ConfigurationException("parameter '"+PARAMETER_FILENAME+"' cannot not be configured on action [close]");
				}
				break;
			case STREAM:
				if(parameterList == null || !parameterList.hasParameter(PARAMETER_FILENAME)) {
					throw new ConfigurationException("parameter '"+PARAMETER_FILENAME+"' is required");
				}
				break;
			default:
				throw new ConfigurationException("unknown action ["+action+"]");
		}
	}

	public ZipWriter(boolean includeFileHeaders) {
		this(includeFileHeaders, null);
	}

	/**
	 * Create a new ZipWriterCollector
	 * @param includeFileHeaders whether to calculate the size and crc for each entry
	 * @param zipLocation if exists the file should be placed here, for legacy / deprecated purposes only!
	 */
	public ZipWriter(boolean includeFileHeaders, String zipLocation) {
		this.includeFileHeaders = includeFileHeaders;
		this.zipLocation = zipLocation;
	}

	@Override
	public MessageZipEntry createPart(Message input, PipeLineSession session, ParameterValueList pvl) throws CollectionException {
		String filename = ParameterValueList.getValue(pvl, PARAMETER_FILENAME, "");
		try {
			Message contents = ParameterValueList.getValue(pvl, PARAMETER_CONTENTS, input);
			if (StringUtils.isEmpty(filename) && contents != input) {
				filename = input.asString();
			}
			session.unscheduleCloseOnSessionExit(contents);
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
		MessageBuilder messageBuilder;
		if(StringUtils.isEmpty(zipLocation)) {
			messageBuilder = new MessageBuilder();
		} else {
			messageBuilder = new MessageBuilder(Paths.get(zipLocation));
		}

		try (ZipOutputStream zipoutput = new ZipOutputStream(messageBuilder.asOutputStream())) {
			for(MessageZipEntry entry : parts) {
				entry.writeEntry(zipoutput);
			}
		}

		Message result = messageBuilder.build();
		result.getContext().withMimeType(MIMETYPE_ZIP);
		return result;
	}

	@Override
	public void close() throws Exception {
		// nothing to close
	}
}
