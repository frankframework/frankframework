/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * Helper class to create Zip archives.
 *
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class ZipWriter {
	protected Logger log = LogUtil.getLogger(this);

	private ZipOutputStream zipoutput;
	private boolean entryOpen=false;
	private boolean closeOnExit;

	private ZipWriter(OutputStream resultStream, boolean closeOnExit) {
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
		if (handle.log.isDebugEnabled()) handle.log.debug(handle.getLogPrefix(handlekey)+"opened new zipstream");
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


	protected void close() throws CompressionException {
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

	public void writeEntry(String filename, Message contents, boolean close, String charset) throws CompressionException, IOException {
		if (StringUtils.isEmpty(filename)) {
			throw new CompressionException("filename cannot be empty");
		}
		openEntry(filename);
		if (contents!=null) {
			try (InputStream is = contents.asInputStream( charset)) {
				Misc.streamToStream(is,getZipoutput(), null, false); // Do not close the inputStream yet
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

	public String getLogPrefix(String handlekey) {
		return "ZipWriterHandle ["+handlekey+"] ";
	}

	public void setCloseOnExit(boolean b) {
		closeOnExit = b;
	}
	public boolean isCloseOnExit() {
		return closeOnExit;
	}

	public ZipOutputStream getZipoutput() {
		return zipoutput;
	}

}
