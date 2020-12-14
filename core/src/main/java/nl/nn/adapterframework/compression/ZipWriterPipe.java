/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

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
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>filename</td><td>string</td><td>filename of the zip or zipentry.</td></tr>
 * </table>
 *
 * @author  Gerrit van Brakel
 * @since   4.9.10
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
	private boolean completeFileHeader=false;

	private Parameter filenameParameter=null; //used for with action=open for main filename, with action=write for entryfilename


	@Override
	public void configure(PipeLine pipeline) throws ConfigurationException {
		super.configure(pipeline);
		if (!(ACTION_OPEN.equals(getAction()) || ACTION_WRITE.equals(getAction())  || ACTION_STREAM.equals(getAction()) || ACTION_CLOSE.equals(getAction()))) {
			throw new ConfigurationException("action must be either '"+ACTION_OPEN+"','"+ACTION_WRITE+"','"+ACTION_STREAM+"' or '"+ACTION_CLOSE+"'");
		}
		if (ACTION_OPEN.equals(getAction())) {
			filenameParameter=getParameterList().findParameter(PARAMETER_FILENAME);
		}
		if (ACTION_WRITE.equals(getAction()) || ACTION_STREAM.equals(getAction())) {
			filenameParameter=getParameterList().findParameter(PARAMETER_FILENAME);
			if (filenameParameter==null) {
				throw new ConfigurationException("a parameter '"+PARAMETER_FILENAME+"' is required");
			}
		}
		if (ACTION_CLOSE.equals(getAction())) {
			filenameParameter=getParameterList().findParameter(PARAMETER_FILENAME);
			if (filenameParameter!=null) {
				throw new ConfigurationException("with action ["+getAction()+"] parameter '"+PARAMETER_FILENAME+"' cannot not be configured");
			}
		}
	}


	protected ZipWriter getZipWriter(IPipeLineSession session) {
		return ZipWriter.getZipWriter(session,getZipWriterHandle());
	}

	protected ZipWriter createZipWriter(IPipeLineSession session, ParameterValueList pvl, Message message) throws PipeRunException {
		if (log.isDebugEnabled()) log.debug(getLogPrefix(session)+"opening new zipstream");
		OutputStream resultStream=null;
		Object input=message.asObject();
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
			throw new PipeRunException(this,getLogPrefix(session)+"did not find OutputStream or HttpResponse, and could not find filename");
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

	@Override
	public PipeRunResult doPipe(Message input, IPipeLineSession session) throws PipeRunException {
		if (ACTION_CLOSE.equals(getAction())) {
			closeZipWriterHandle(session,true);
			return new PipeRunResult(getForward(),input);
		}
		ParameterValueList pvl=null;
		try {
			if (getParameterList()!=null) {
				pvl = getParameterList().getValues(input, session);
			}
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
					if (isCompleteFileHeader()) {
						sessionData.writeEntryWithCompletedHeader(filename, input, isCloseInputstreamOnExit(), getCharset());
					} else {
						sessionData.writeEntry(filename, input, isCloseInputstreamOnExit(), getCharset());
					}
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

	@Override
	protected String getLogPrefix(IPipeLineSession session) {
		return super.getLogPrefix(session)+"action ["+getAction()+"] ";
	}

	@IbisDoc({"only for action='write': when set to <code>false</code>, the inputstream is not closed after the zip entry is written", "true"})
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}
	public boolean isCloseInputstreamOnExit() {
		return closeInputstreamOnExit;
	}

	@IbisDoc({"only for action='open': when set to <code>false</code>, the outputstream is not closed after the zip creation is finished", "true"})
	public void setCloseOutputstreamOnExit(boolean b) {
		closeOutputstreamOnExit = b;
	}
	@Deprecated
	@ConfigurationWarning("attribute 'closeStreamOnExit' has been renamed to 'closeOutputstreamOnExit'")
	public void setCloseStreamOnExit(boolean b) {
		setCloseOutputstreamOnExit(b);
	}
	public boolean isCloseOutputstreamOnExit() {
		return closeOutputstreamOnExit;
	}

	@IbisDoc({"only for action='write': charset used to write strings to zip entries", "utf-8"})
	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	@IbisDoc({"session key used to refer to zip session. must be used if zipwriterpipes are nested", "zipwriterhandle"})
	public void setZipWriterHandle(String string) {
		zipWriterHandle = string;
	}
	public String getZipWriterHandle() {
		return zipWriterHandle;
	}

	@IbisDoc({"one of <ul><li>open: to open a new zip file or stream</li> <li>close: to close the zip file or stream</li> <li>write: write the input to the zip as a new entry</li> <li>stream: create a new zip entry, and provide an outputstream that another pipe can use to write the contents</li> </ul>", ""})
	public void setAction(String string) {
		action = string;
	}
	public String getAction() {
		return action;
	}

	@IbisDoc({"only for action='write': when set to <code>true</code>, the fields 'crc-32', 'compressed size' and 'uncompressed size' in the zip entry file header are set explicitly (note: compression ratio is zero)", "false"})
	public void setCompleteFileHeader(boolean b) {
		completeFileHeader = b;
	}
	public boolean isCompleteFileHeader() {
		return completeFileHeader;
	}
}
