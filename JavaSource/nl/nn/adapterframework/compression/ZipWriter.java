/*
 * $Log: ZipWriter.java,v $
 * Revision 1.1  2010-01-06 17:57:35  L190409
 * classes for reading and writing zip archives
 *
 */
package nl.nn.adapterframework.compression;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.LogUtil;

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
