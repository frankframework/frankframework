/*
 * $Log: ZipWriter.java,v $
 * Revision 1.4  2011-11-30 13:51:57  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2010/03/25 12:55:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added writeEntry()
 *
 * Revision 1.1  2010/01/06 17:57:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * classes for reading and writing zip archives
 *
 */
package nl.nn.adapterframework.compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Helper class to create Zip archives.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.10
 * @version Id
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
