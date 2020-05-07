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
package nl.nn.adapterframework.compression;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

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

	public static ZipWriter getZipWriter(IPipeLineSession session, String handlekey) {
		return (ZipWriter)session.get(handlekey);
	}

	public static ZipWriter createZipWriter(IPipeLineSession session, String handlekey, OutputStream resultStream, boolean closeOnExit) {
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

	public void writeEntry(String filename, Object contents, boolean close, String charset) throws CompressionException, IOException {
		if (StringUtils.isEmpty(filename)) {
			throw new CompressionException("filename cannot be empty");		
		}
		openEntry(filename);
		if (contents!=null) {
			if (contents instanceof byte[]) {
				getZipoutput().write((byte[])contents);
			} else if (contents instanceof InputStream) {
				InputStream is = (InputStream)contents;
				try {
					Misc.streamToStream(is,getZipoutput());
				} finally {
					if (close) {
						is.close();
					}
				}
			} else {
				getZipoutput().write(contents.toString().getBytes(charset));
			}
		} else { 
			log.warn("contents of zip entry ["+filename+"] is null");
		}
		closeEntry();
	}

	public void writeEntryWithCompletedHeader(String filename, Object contents, boolean close, String charset) throws CompressionException, IOException {
		if (StringUtils.isEmpty(filename)) {
			throw new CompressionException("filename cannot be empty");		
		}
		
		byte[] contentBytes = null;
		BufferedInputStream bis = null;
		long size = 0;
		if (contents!=null) {
			if (contents instanceof byte[]) {
				contentBytes = (byte[])contents;
			} else if (contents instanceof InputStream) {
				contentBytes = Misc.streamToBytes((InputStream)contents);
			} else {
				contentBytes = contents.toString().getBytes(charset);
			}
			bis = new BufferedInputStream(new ByteArrayInputStream(contentBytes));
			size = bis.available();
		} else { 
			log.warn("contents of zip entry ["+filename+"] is null");
		}
		
		int bytesRead;
		byte[] buffer = new byte[1024];
		CRC32 crc = new CRC32();
		crc.reset();
		if (bis!=null) {
			while ((bytesRead = bis.read(buffer)) != -1) {
				crc.update(buffer, 0, bytesRead);
			}
			bis.close();
		}
		if (contents!=null) {
			bis = new BufferedInputStream(new ByteArrayInputStream(contentBytes));
		}
		ZipEntry entry = new ZipEntry(filename);
		entry.setMethod(ZipEntry.STORED);
		entry.setCompressedSize(size);
		entry.setSize(size);
		entry.setCrc(crc.getValue());
		getZipoutput().putNextEntry(entry);
		if (bis!=null) {
			while ((bytesRead = bis.read(buffer)) != -1) {
				getZipoutput().write(buffer, 0, bytesRead);
			}
			bis.close();
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
