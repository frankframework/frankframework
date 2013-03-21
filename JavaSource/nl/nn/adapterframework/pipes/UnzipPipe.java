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
/*
 * $Log: UnzipPipe.java,v $
 * Revision 1.7  2013-02-26 12:43:10  europe\m168309
 * UnzipPipe: added collectsResults attribute
 *
 * Revision 1.6  2012/06/01 10:52:49  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.5  2011/11/30 13:51:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2010/09/02 12:34:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synced with Ibis4Belasting
 * force write to specified directory
 * keep filename and extension
 *
 * Revision 1.2  2008/08/27 16:19:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * some fixes
 *
 * Revision 1.1  2008/06/26 12:51:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fisrt version
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Assumes input to be a ZIP archive, and unzips it to a directory.
 * Currently no subdirectories in zip files are supported.
 *
 * <br>
 * The output of each unzipped item is returned in XML as follows:
 * <pre>
 *  &lt;results count="num_of_items"&gt;
 *    &lt;result item="1"&gt;
 *      &lt;zipEntry&gt;name in ZIP archive of first item&lt;/zipEntry&gt;
 *      &lt;fileName&gt;filename of first item&lt;/fileName&gt;
 *    &lt;/result&gt;
 *    &lt;result item="2"&gt;
 *      &lt;zipEntry&gt;name in ZIP archive of second item&lt;/zipEntry&gt;
 *      &lt;fileName&gt;filename of second item&lt;/fileName&gt;
 *    &lt;/result&gt;
 *       ...
 *  &lt;/results&gt;
 * </pre>
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.FixedResult</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setDirectory(String) directory}</td>       <td>directory to extract the archive to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDeleteOnExit(boolean) deleteOnExit}</td><td>when true, file is automatially deleted upon normal JVM termination</td><td>true</td></tr>
 * <tr><td>{@link #setCollectResults(boolean) collectResults}</td><td>if set <code>false</code>, only a small summary is returned</td><td>true</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * @version $Id$
 * @since   4.9
 * @author  Gerrit van Brakel
 */
public class UnzipPipe extends FixedForwardPipe {
	
    private String directory;
    private boolean deleteOnExit=true;
	private boolean collectResults=true;
    
	private File dir; // File representation of directory
    
    public void configure() throws ConfigurationException {
    	super.configure();
    	if (StringUtils.isEmpty(getDirectory())) {
    		throw new ConfigurationException(getLogPrefix(null)+"directory must be specified");
    	}
    	dir= new File(getDirectory());
    	if (!dir.exists()) {
			throw new ConfigurationException(getLogPrefix(null)+"directory ["+getDirectory()+"] does not exist");
    	}
		if (!dir.isDirectory()) {
			throw new ConfigurationException(getLogPrefix(null)+"directory ["+getDirectory()+"] is not a directory");
		}
    }
    
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		InputStream in;
		if (input instanceof InputStream) {
			in=(InputStream)input;
		} else {
			String filename=(String)input;
			try {
				in=new FileInputStream(filename);
			} catch (FileNotFoundException e) {
				throw new PipeRunException(this, "could not find file ["+filename+"]",e);
			}
		}
		String entryResults = "";
		int count = 0;
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in));
		try {
			ZipEntry ze;
			while ((ze=zis.getNextEntry())!=null) {
				if (ze.isDirectory()) {
					log.warn(getLogPrefix(session)+"skipping directory entry ["+ze.getName()+"]");
				} else {
					String filename=ze.getName();
					String extension=null;
					int dotPos=filename.indexOf('.');
					if (dotPos>=0) {
						extension=filename.substring(dotPos);
						filename=filename.substring(0,dotPos);
						log.debug(getLogPrefix(session)+"parsed filename ["+filename+"] extension ["+extension+"]");
					}
					File tmpFile = File.createTempFile(filename, extension, dir);
					if (isDeleteOnExit()) {
						tmpFile.deleteOnExit();
					}
					FileOutputStream fos = new FileOutputStream(tmpFile);
					log.debug(getLogPrefix(session)+"writing ZipEntry ["+ze.getName()+"] to file ["+tmpFile.getPath()+"]");
					count++;
					Misc.streamToStream(zis,fos,false);
					fos.close();
					if (isCollectResults()) {
						entryResults += "<result item=\"" + count + "\"><zipEntry>" + XmlUtils.encodeCdataString(ze.getName()) + "</zipEntry><fileName>" + XmlUtils.encodeCdataString(tmpFile.getPath()) + "</fileName></result>";
					}
				}
			}
		} catch (IOException e) {
			throw new PipeRunException(this,"cannot unzip",e);
		} finally {
			try {
				zis.close();
			} catch (IOException e1) {
				log.warn(getLogPrefix(session)+"exception closing zip",e1);
			}
		}
		String result = "<results count=\"" + count + "\">" + entryResults + "</results>";
		return new PipeRunResult(getForward(),result);
	}

	public void setDirectory(String string) {
		directory = string;
	}
	public String getDirectory() {
		return directory;
	}

	public void setDeleteOnExit(boolean b) {
		deleteOnExit = b;
	}
	public boolean isDeleteOnExit() {
		return deleteOnExit;
	}

	public void setCollectResults(boolean b) {
		collectResults = b;
	}
	public boolean isCollectResults() {
		return collectResults;
	}
}
