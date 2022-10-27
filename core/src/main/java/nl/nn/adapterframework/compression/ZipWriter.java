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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.collection.CollectionException;
import nl.nn.adapterframework.collection.ICollector;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * Helper class to create Zip archives.
 *
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class ZipWriter implements ICollector {
	protected Logger log = LogUtil.getLogger(this);

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
	public Message writeItem(Message input, PipeLineSession session, ParameterValueList pvl, Object attributeSource) throws CollectionException, TimeoutException {
		String filename = pvl.get(ZipWriterPipe.PARAMETER_FILENAME).asStringValue();
		String charset;
		boolean completeFileHeader;
		try {
			charset=ClassUtils.invokeStringGetter(attributeSource, "getCharset");
			completeFileHeader=(Boolean)ClassUtils.invokeGetter(attributeSource, "isCompleteFileHeader");
		} catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			throw new CollectionException("cannot read field values", e);
		}
		try {
			if (completeFileHeader) {
				writeEntryWithCompletedHeader(filename, input, false, charset);
			} else {
				writeEntry(filename, input, false, charset);
			}
			return input;
		} catch (IOException | CompressionException e) {
			throw new CollectionException("cannot write item", e);
		}
	}

	@Override
	public Message streamItem(Message input, PipeLineSession session, ParameterValueList pvl, Object attributeSource) throws CollectionException {
		String filename = pvl.get(ZipWriterPipe.PARAMETER_FILENAME).asStringValue();
		try {
			openEntry(filename);
		} catch (CompressionException e) {
			throw new CollectionException("cannot prepare collection to stream item", e);
		}
		return Message.asMessage(getZipoutput());
	}


	public void writeEntry(String filename, Message contents, boolean close, String charset) throws CompressionException, IOException {
		if (StringUtils.isEmpty(filename)) {
			throw new CompressionException("filename cannot be empty");
		}
		openEntry(filename);
		if (contents!=null) {
			try (InputStream is = contents.asInputStream( charset)) {
				Misc.streamToStream(is,getZipoutput());
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
