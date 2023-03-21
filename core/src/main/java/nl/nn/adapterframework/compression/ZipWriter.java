/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.collection.CollectionActor.Action;
import nl.nn.adapterframework.collection.CollectionException;
import nl.nn.adapterframework.collection.ICollector;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Helper class to create Zip archives.
 *
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class ZipWriter implements ICollector<IZipWritingElement> {
	protected static Logger log = LogUtil.getLogger(ZipWriter.class);

	public static final String PARAMETER_FILENAME="filename";
	public static final String PARAMETER_CONTENTS="contents";

	private @Getter ZipOutputStream zipoutput;
	private boolean entryOpen=false;
	private @Getter @Setter boolean closeOnExit;

	public ZipWriter(OutputStream resultStream, boolean closeOnExit) {
		super();
		this.closeOnExit=closeOnExit;
		zipoutput=new ZipOutputStream(resultStream);
	}

	public static ZipWriter getZipWriter(PipeLineSession session, String handlekey) {
		return (ZipWriter)session.get(handlekey);
	}

	public static ZipWriter createZipWriter(PipeLineSession session, String handlekey, OutputStream resultStream, boolean closeOnExit) {
		ZipWriter handle=new ZipWriter(resultStream,closeOnExit);
		session.put(handlekey,handle);
		handle.log.debug("opened new zipstream [{}]", handlekey);
		return handle;
	}

	public static void configure(Action action, ParameterList parameterList) throws ConfigurationException {
		switch (action) {
			case OPEN:
				break;
			case WRITE:
			case LAST:
			case STREAM:
				Parameter filenameParameter=parameterList.findParameter(PARAMETER_FILENAME);
				Parameter contentsParameter=parameterList.findParameter(PARAMETER_CONTENTS);
				if (filenameParameter==null && contentsParameter==null) {
					throw new ConfigurationException("parameter '"+PARAMETER_FILENAME+"' or parameter '"+PARAMETER_CONTENTS+"' is required");
				}
				break;
			case CLOSE:
				if (parameterList.findParameter(PARAMETER_FILENAME)!=null) {
					throw new ConfigurationException("with action ["+action+"] parameter '"+PARAMETER_FILENAME+"' cannot not be configured");
				}
				break;
			default:
				throw new ConfigurationException("unknwon action ["+action+"]");
		}
	}

	public static ZipWriter openCollection(Message message, PipeLineSession session, ParameterValueList pvl, IZipWritingElement writingElement) throws CollectionException {
		log.debug("opening new zipstream");
		OutputStream resultStream=null;
		Object input=message.asObject();
		if (input==null) {
			throw new CollectionException("input cannot be null, must be OutputStream, HttpResponse or String containing filename");
		}
		if (input instanceof OutputStream) {
			resultStream=(OutputStream)input;
		} else if (input instanceof HttpServletResponse) {
			ParameterValue pv=pvl.get(PARAMETER_FILENAME);
			if (pv==null) {
				throw new CollectionException("parameter '"+PARAMETER_FILENAME+"' not found, but required if stream is HttpServletResponse");
			}
			String filename=pv.asStringValue("download.zip");
			try {
				HttpServletResponse response=(HttpServletResponse)input;
				resultStream = openZipDownload(response,filename);
			} catch (IOException e) {
				throw new CollectionException("cannot open download for ["+filename+"]",e);
			}
		} else if (input instanceof String) {
			String filename=(String)input;
			if (StringUtils.isEmpty(filename)) {
				throw new CollectionException("input string cannot be empty but must contain a filename");
			}
			try {
				resultStream =new FileOutputStream(filename);
			} catch (FileNotFoundException e) {
				throw new CollectionException("cannot create file ["+filename+"] a specified by input message",e);
			}
		}
		if (resultStream==null) {
			throw new CollectionException("did not find OutputStream or HttpResponse, and could not find filename");
		}
		return new ZipWriter(resultStream,writingElement.isCloseOutputstreamOnExit());
	}

	private static ZipOutputStream openZipDownload(HttpServletResponse response, String filename) throws IOException {
		OutputStream out = response.getOutputStream();
		response.setContentType("application/x-zip-compressed");
		response.setHeader("Content-Disposition","attachment; filename=\""+filename+"\"");
		return new ZipOutputStream(out);
	}

	public void openEntry(String filename) throws CompressionException {
		closeEntry();
		ZipEntry entry = new ZipEntry(filename);
		try {
			zipoutput.putNextEntry(entry);
			entryOpen=true;
		} catch (IOException e) {
			throw new CompressionException("cannot add zipentry for ["+filename+"]",e);
		}
	}

	public void closeEntry() throws CompressionException {
		if (entryOpen) {
			entryOpen=false;
			try {
				zipoutput.closeEntry();
			} catch (IOException e) {
				throw new CompressionException("cannot close zipentry",e);
			}
		}
	}


	@Override
	public void close() throws CompressionException {
		closeEntry();
		try {
			if (isCloseOnExit()) {
				zipoutput.close();
			} else {
				zipoutput.finish();
			}
		} catch (IOException e) {
			throw new CompressionException("Cannot close ZipStream",e);
		}
	}

	@Override
	public Message writeItem(Message input, PipeLineSession session, ParameterValueList pvl, IZipWritingElement writingElement) throws CollectionException, TimeoutException {
		String filename = ParameterValueList.getValue(pvl, PARAMETER_FILENAME, "");
		String charset = writingElement.getCharset();
		boolean completeFileHeader = writingElement.isCompleteFileHeader();
		try {
			Message contents = ParameterValueList.getValue(pvl, PARAMETER_CONTENTS, input);
			if (StringUtils.isEmpty(filename) && contents!=input) {
				filename = input.asString();
			}
			if (completeFileHeader) {
				writeEntryWithCompletedHeader(filename, contents, writingElement.isCloseInputstreamOnExit(), charset);
			} else {
				writeEntry(filename, contents, writingElement.isCloseInputstreamOnExit(), charset);
			}
			return contents==input ? Message.nullMessage() : input;
		} catch (IOException | CompressionException e) {
			throw new CollectionException("cannot write item", e);
		}
	}

	@Override
	public MessageOutputStream provideOutputStream(PipeLineSession session, ParameterValueList pvl, IZipWritingElement writingElement) throws CollectionException {
		String filename = ParameterValueList.getValue(pvl, PARAMETER_FILENAME, "");
		String charset = writingElement.getCharset();
		boolean completeFileHeader = writingElement.isCompleteFileHeader();

		if (completeFileHeader || StringUtils.isEmpty(filename)) {
			return null;
		}
		Message contents = ParameterValueList.getValue(pvl, PARAMETER_CONTENTS, Message.nullMessage());
		if (!Message.isNull(contents)) { // cannot provide OutputStream if contents is something else as the input message
			return null;
		}
		try {
			openEntry(filename);
			OutputStream stream = StreamUtil.dontClose(getZipoutput());
			return new MessageOutputStream(writingElement, stream, (IForwardTarget)null, charset) {

				@Override
				public void afterClose() throws Exception {
					stream.flush();
					closeEntry();
				}

			};
		} catch (CompressionException e) {
			throw new CollectionException("cannot prepare collection to stream item", e);
		}
	}

	@Override
	public OutputStream streamItem(Message input, PipeLineSession session, ParameterValueList pvl, IZipWritingElement writingElement) throws CollectionException {
		try {
			String filename = ParameterValueList.getValue(pvl, PARAMETER_FILENAME, input.asString());
			openEntry(filename);
		} catch (CompressionException | IOException e) {
			throw new CollectionException("cannot prepare collection to stream item", e);
		}
		return StreamUtil.dontClose(getZipoutput());
	}


	public void writeEntry(String filename, Message contents, boolean close, String charset) throws CompressionException, IOException {
		if (StringUtils.isEmpty(filename)) {
			throw new CompressionException("filename cannot be empty");
		}
		openEntry(filename);
		if (contents!=null) {
			try (InputStream is = contents.asInputStream( charset)) {
				StreamUtil.streamToStream(is,getZipoutput());
			}
		} else {
			log.warn("contents of zip entry ["+filename+"] is null");
		}
		closeEntry();
	}

	public void writeEntryWithCompletedHeader(String filename, Message contents, boolean close, String charset) throws CompressionException, IOException {
		if (StringUtils.isEmpty(filename)) {
			throw new CompressionException("filename cannot be empty");
		}

		byte[] contentBytes = null;
		int size = 0;
		if (contents!=null) {
			contentBytes = contents.asByteArray(charset);
		} else {
			log.warn("contents of zip entry ["+filename+"] is null");
		}

		CRC32 crc = new CRC32();
		crc.reset();
		if (contentBytes!=null) {
			size = contentBytes.length;
			crc.update(contentBytes, 0, size);
		}
		ZipEntry entry = new ZipEntry(filename);
		entry.setMethod(ZipEntry.STORED);
		entry.setCompressedSize(size);
		entry.setSize(size);
		entry.setCrc(crc.getValue());
		getZipoutput().putNextEntry(entry);
		if (contentBytes!=null) {
			getZipoutput().write(contentBytes, 0, contentBytes.length);
		}
		getZipoutput().closeEntry();
	}


}
