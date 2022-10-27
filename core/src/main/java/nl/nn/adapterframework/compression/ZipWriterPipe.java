/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.collection.CollectorPipe;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
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
 *
 * @ff.parameter filename specifies the filename if the input is a HttpResponse
 *
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class ZipWriterPipe extends CollectorPipe<ZipWriter> {

	public static final String PARAMETER_FILENAME="filename";

	private @Getter boolean closeInputstreamOnExit=true;
	private @Getter boolean closeOutputstreamOnExit=true;
	private @Getter String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private @Getter boolean completeFileHeader=false;

	private Parameter filenameParameter=null; //used for with action=open for main filename, with action=write for entryfilename

	{
		setCollection("zipwriterhandle");
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getAction() == Action.OPEN) {
			filenameParameter=getParameterList().findParameter(PARAMETER_FILENAME);
		}
		if (getAction() == Action.WRITE || getAction() == Action.STREAM) {
			filenameParameter=getParameterList().findParameter(PARAMETER_FILENAME);
			if (filenameParameter==null) {
				throw new ConfigurationException("a parameter '"+PARAMETER_FILENAME+"' is required");
			}
		}
		if (getAction() == Action.CLOSE) {
			filenameParameter=getParameterList().findParameter(PARAMETER_FILENAME);
			if (filenameParameter!=null) {
				throw new ConfigurationException("with action ["+getAction()+"] parameter '"+PARAMETER_FILENAME+"' cannot not be configured");
			}
		}
	}

	@Override
	protected ZipWriter openCollection(Message message, PipeLineSession session, ParameterValueList pvl) throws PipeRunException {
		log.debug("opening new zipstream");
		OutputStream resultStream=null;
		Object input=message.asObject();
		if (input==null) {
			throw new PipeRunException(this,"input cannot be null, must be OutputStream, HttpResponse or String containing filename");
		}
		if (input instanceof OutputStream) {
			resultStream=(OutputStream)input;
		} else if (input instanceof HttpServletResponse) {
			ParameterValue pv=pvl.get(PARAMETER_FILENAME);
			if (pv==null) {
				throw new PipeRunException(this,"parameter 'filename' not found, but required if stream is HttpServletResponse");
			}
			String filename=pv.asStringValue("download.zip");
			try {
				HttpServletResponse response=(HttpServletResponse)input;
				StreamUtil.openZipDownload(response,filename);
				resultStream=response.getOutputStream();
			} catch (IOException e) {
				throw new PipeRunException(this,"cannot open download for ["+filename+"]",e);
			}
		} else if (input instanceof String) {
			String filename=(String)input;
			if (StringUtils.isEmpty(filename)) {
				throw new PipeRunException(this,"input string cannot be empty but must contain a filename");
			}
			try {
				resultStream =new FileOutputStream(filename);
			} catch (FileNotFoundException e) {
				throw new PipeRunException(this,"cannot create file ["+filename+"] a specified by input message",e);
			}
		}
		if (resultStream==null) {
			throw new PipeRunException(this,"did not find OutputStream or HttpResponse, and could not find filename");
		}
		return ZipWriter.createZipWriter(session,resultStream,isCloseOutputstreamOnExit(), this);
	}



	@IbisDoc({"Only for action='write': If set to <code>false</code>, the inputstream is not closed after the zip entry is written", "true"})
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}

	@IbisDoc({"Only for action='open': If set to <code>false</code>, the outputstream is not closed after the zip creation is finished", "true"})
	public void setCloseOutputstreamOnExit(boolean b) {
		closeOutputstreamOnExit = b;
	}
	@Deprecated
	@ConfigurationWarning("attribute 'closeStreamOnExit' has been renamed to 'closeOutputstreamOnExit'")
	public void setCloseStreamOnExit(boolean b) {
		setCloseOutputstreamOnExit(b);
	}

	@IbisDoc({"Only for action='write': Charset used to write strings to zip entries", "utf-8"})
	public void setCharset(String string) {
		charset = string;
	}

	@IbisDoc({"Session key used to refer to zip session. Must be specified with another value if ZipWriterPipes are nested", "zipwriterhandle"})
	@Deprecated
	@ConfigurationWarning("Replaced with attribute collection")
	public void setZipWriterHandle(String string) {
		setCollection(string);
	}

	@IbisDoc({"Only for action='write': If set to <code>true</code>, the fields 'crc-32', 'compressed size' and 'uncompressed size' in the zip entry file header are set explicitly (note: compression ratio is zero)", "false"})
	public void setCompleteFileHeader(boolean b) {
		completeFileHeader = b;
	}
}
