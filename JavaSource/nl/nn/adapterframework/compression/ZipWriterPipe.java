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
 * $Log: ZipWriterPipe.java,v $
 * Revision 1.7  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.6  2011/11/30 13:51:57  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2010/04/01 11:56:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved configwarning
 *
 * Revision 1.3  2010/03/25 12:56:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed attribute closeStreamOnExit into closeOutputstreamOnExit
 * added attribute closeInputstreamOnExit
 *
 * Revision 1.2  2010/01/07 13:14:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved robustness: filename of file to be written must be specified
 * by input message. Parameter is only used for logical filenames.
 *
 * Revision 1.1  2010/01/06 17:57:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * classes for reading and writing zip archives
 *
 */
package nl.nn.adapterframework.compression;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.StreamUtil;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe that creates a ZipStream.
 * 
 * For action=open, the Pipe will create a new zip, that will be written to a file or stream specified by the input message, that must be a:<ul>
 * <li>String specifying a filename</li>
 * <li>OutputStream</li>
 * <li>HttpResponse</li>
 * </ul>
 * The parameter 'filename' is used to specify the filename if the input is a HttpResponse.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.compression.ZipWriterPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified, then the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * <tr><td>{@link #setBeforeEvent(int) beforeEvent}</td>      <td>METT eventnumber, fired just before a message is processed by this Pipe</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setAfterEvent(int) afterEvent}</td>        <td>METT eventnumber, fired just after message processing by this Pipe is finished</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setExceptionEvent(int) exceptionEvent}</td><td>METT eventnumber, fired when message processing by this Pipe resulted in an exception</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setAction(String) action}</td>  <td>one of <ul>
 *   <li>open: to open a new zip file or stream</li> 
 *   <li>close: to close the zip file or stream</li> 
 *   <li>write: write the input to the zip as a new entry</li> 
 *   <li>stream: create a new zip entry, and provide an outputstream that another pipe can use to write the contents</li> 
 * </ul></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setZipWriterHandle(String) zipWriterHandle}</td>  <td>session key used to refer to zip session. Must be used if ZipWriterPipes are nested</td><td>"zipwriterhandle"</td></tr>
 * <tr><td>{@link #setCloseOutputstreamOnExit(boolean) closeOutputstreamOnExit}</td>  <td>only for action="open": when set to <code>false</code>, the outputstream is not closed after the zip creation is finished</td><td>true</td></tr>
 * <tr><td>{@link #setCloseInputstreamOnExit(boolean) closeInputstreamOnExit}</td>  <td>only for action="write": when set to <code>false</code>, the inputstream is not closed after the zip entry is written</td><td>true</td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td><td>only for action="write": charset used to write strings to zip entries</td><td>UTF-8</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>filename</td><td>string</td><td>filename of the zip or zipentry.</td></tr>
 * </table>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.10
 * @version $Id$
 */
public class ZipWriterPipe extends FixedForwardPipe {

 	private static final String ACTION_OPEN="open";
	private static final String ACTION_WRITE="write";
	private static final String ACTION_STREAM="stream";
	private static final String ACTION_CLOSE="close";
	
	private static final String PARAMETER_FILENAME="filename";
 
 	private String action=null;
	private String zipWriterHandle="zipwriterhandle";
	private boolean closeInputstreamOnExit=true;
	private boolean closeOutputstreamOnExit=true;
	private String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	
	private Parameter filenameParameter=null; //used for with action=open for main filename, with action=write for entryfilename


	public void configure(PipeLine pipeline) throws ConfigurationException {
		super.configure(pipeline);
		if (!(ACTION_OPEN.equals(getAction()) || ACTION_WRITE.equals(getAction())  || ACTION_STREAM.equals(getAction()) || ACTION_CLOSE.equals(getAction()))) {
			throw new ConfigurationException(getLogPrefix(null)+"action must be either '"+ACTION_OPEN+"','"+ACTION_WRITE+"','"+ACTION_STREAM+"' or '"+ACTION_CLOSE+"'");
		}
		if (ACTION_OPEN.equals(getAction())) {
			filenameParameter=getParameterList().findParameter(PARAMETER_FILENAME);
		}
		if (ACTION_WRITE.equals(getAction()) || ACTION_STREAM.equals(getAction())) {
			filenameParameter=getParameterList().findParameter(PARAMETER_FILENAME);
			if (filenameParameter==null) {
				throw new ConfigurationException(getLogPrefix(null)+"a parameter '"+PARAMETER_FILENAME+"' is required");
			}
		}
		if (ACTION_CLOSE.equals(getAction())) {
			filenameParameter=getParameterList().findParameter(PARAMETER_FILENAME);
			if (filenameParameter!=null) {
				throw new ConfigurationException(getLogPrefix(null)+"with action ["+getAction()+"] parameter '"+PARAMETER_FILENAME+"' cannot not be configured");
			}
		}
	}

	
	protected ZipWriter getZipWriter(IPipeLineSession session) {
		return ZipWriter.getZipWriter(session,getZipWriterHandle());
	}

	protected ZipWriter createZipWriter(IPipeLineSession session, ParameterValueList pvl, Object input) throws PipeRunException {
		if (log.isDebugEnabled()) log.debug(getLogPrefix(session)+"opening new zipstream");
		OutputStream resultStream=null;
		if (input==null) {
			throw new PipeRunException(this,getLogPrefix(session)+"input cannot be null, must be OutputStream, HttpResponse or String containing filename");
		}
		if (input instanceof OutputStream) {
			resultStream=(OutputStream)input;
		} else if (input instanceof HttpServletResponse) {
			ParameterValue pv=pvl.getParameterValue(PARAMETER_FILENAME);
			if (pv==null) {
				throw new PipeRunException(this,getLogPrefix(session)+"parameter 'filename' not found, but required if stream is HttpServletResponse");
			}
			String filename=pv.asStringValue("download.zip");
			try {
				HttpServletResponse response=(HttpServletResponse)input;
				StreamUtil.openZipDownload(response,filename);
				resultStream=response.getOutputStream();
			} catch (IOException e) {
				throw new PipeRunException(this,getLogPrefix(session)+"cannot open download for ["+filename+"]",e);
			}
		} else if (input instanceof String) {
			String filename=(String)input;
			if (StringUtils.isEmpty(filename)) {
				throw new PipeRunException(this,getLogPrefix(session)+"input string cannot be empty but must contain a filename");
			}
			try {
				resultStream =new FileOutputStream(filename);
			} catch (FileNotFoundException e) {
				throw new PipeRunException(this,getLogPrefix(session)+"cannot create file ["+filename+"] a specified by input message",e);
			}
		}
		if (resultStream==null) {
			throw new PipeRunException(this,getLogPrefix(session)+"Dit not find OutputStream or HttpResponse, and could not find filename");
		}
		ZipWriter sessionData=ZipWriter.createZipWriter(session,getZipWriterHandle(),resultStream,isCloseOutputstreamOnExit());
		return sessionData;
	}


	protected void closeZipWriterHandle(IPipeLineSession session, boolean mustFind) throws PipeRunException {
		ZipWriter sessionData=getZipWriter(session);
		if (sessionData==null) {
			if (mustFind) {
				throw new PipeRunException(this,getLogPrefix(session)+"cannot find session data");
			} else {
				log.debug(getLogPrefix(session)+"did find session data, assuming already closed");
			}
		} else {
			try {
				sessionData.close();
			} catch (CompressionException e) {
				throw new PipeRunException(this,getLogPrefix(session)+"cannot close",e);
			}
		}
		session.remove(getZipWriterHandle());
	}
	
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		if (ACTION_CLOSE.equals(getAction())) {
			closeZipWriterHandle(session,true);
			return new PipeRunResult(getForward(),input);
		} 
		String msg=null;
		if (input instanceof String) {
			msg=(String)input;
		}
		ParameterResolutionContext prc = new ParameterResolutionContext(msg, session);
		ParameterValueList pvl;
		try {
			pvl = prc.getValues(getParameterList());
		} catch (ParameterException e1) {
			throw new PipeRunException(this,getLogPrefix(session)+"cannot determine filename",e1);
		}

		ZipWriter sessionData=getZipWriter(session);
		if (ACTION_OPEN.equals(getAction())) {
			if (sessionData!=null) {
				throw new PipeRunException(this,getLogPrefix(session)+"zipWriterHandle in session key ["+getZipWriterHandle()+"] is already open");		
			}
			sessionData=createZipWriter(session,pvl,input);
			return new PipeRunResult(getForward(),input);
		} 
		// from here on action must be 'write' or 'stream'
		if (sessionData==null) {
			throw new PipeRunException(this,getLogPrefix(session)+"zipWriterHandle in session key ["+getZipWriterHandle()+"] is not open");		
		} 
		String filename=(String)pvl.getParameterValue(PARAMETER_FILENAME).getValue();
		if (StringUtils.isEmpty(filename)) {
			throw new PipeRunException(this,getLogPrefix(session)+"filename cannot be empty");		
		}
		try {
			if (ACTION_STREAM.equals(getAction())) {
				sessionData.openEntry(filename);
				PipeRunResult prr = new PipeRunResult(getForward(),sessionData.getZipoutput());
				return prr;
			}
			if (ACTION_WRITE.equals(getAction())) {
				try {
					sessionData.writeEntry(filename, input, isCloseInputstreamOnExit(), getCharset());
				} catch (IOException e) {
					throw new PipeRunException(this,getLogPrefix(session)+"cannot add data to zipentry for ["+filename+"]",e);
				}
				return new PipeRunResult(getForward(),input);
			}
			throw new PipeRunException(this,getLogPrefix(session)+"illegal action ["+getAction()+"]");
		} catch (CompressionException e) {
			throw new PipeRunException(this,getLogPrefix(session)+"cannot add zipentry for ["+filename+"]",e);
		}
	}

	protected String getLogPrefix(IPipeLineSession session) {
		return super.getLogPrefix(session)+"action ["+getAction()+"] ";
	}
	
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}
	public boolean isCloseInputstreamOnExit() {
		return closeInputstreamOnExit;
	}

	public void setCloseOutputstreamOnExit(boolean b) {
		closeOutputstreamOnExit = b;
	}
	public void setCloseStreamOnExit(boolean b) {
		ConfigurationWarnings.getInstance().add(getLogPrefix(null)+"attribute 'closeStreamOnExit' has been renamed into 'closeOutputstreamOnExit'");
		setCloseOutputstreamOnExit(b);
	}
	public boolean isCloseOutputstreamOnExit() {
		return closeOutputstreamOnExit;
	}

	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	public void setZipWriterHandle(String string) {
		zipWriterHandle = string;
	}
	public String getZipWriterHandle() {
		return zipWriterHandle;
	}

	public void setAction(String string) {
		action = string;
	}
	public String getAction() {
		return action;
	}
}
