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
import java.io.UnsupportedEncodingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.senders.SenderWithParametersBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Sender that writes an entry to a ZipStream, similar to ZipWriterPipe with action='write'.
 * Filename and contents are taken from parameters. If one of the parameters is not present, the input message
 * is used for either filename or contents.
 *
 * @ff.parameter filename filename of the zipentry
 * @ff.parameter contents contents of the zipentry
 *
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class ZipWriterSender extends SenderWithParametersBase {

	private static final String PARAMETER_FILENAME="filename";
	private static final String PARAMETER_CONTENTS="contents";

	private String zipWriterHandle="zipwriterhandle";
	private boolean closeInputstreamOnExit=true;
	private String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;

	private Parameter filenameParameter=null;
	private Parameter contentsParameter=null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		filenameParameter=paramList.findParameter(PARAMETER_FILENAME);
		contentsParameter=paramList.findParameter(PARAMETER_CONTENTS);
		if (filenameParameter==null && contentsParameter==null) {
			throw new ConfigurationException(getLogPrefix()+"parameter '"+PARAMETER_FILENAME+"' or parameter '"+PARAMETER_CONTENTS+"' is required");
		}
	}



	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		ParameterValueList pvl;
		try {
			pvl = paramList.getValues(message, session);
		} catch (ParameterException e) {
			throw new SenderException("cannot determine filename and/or contents of zip entry",e);
		}

		ZipWriter sessionData=ZipWriter.getZipWriter(session,getZipWriterHandle());
		if (sessionData==null) {
			throw new SenderException("zipWriterHandle in session key ["+getZipWriterHandle()+"] is not open");
		}
		try {
			String filename=filenameParameter==null?message.asString():(String)pvl.get(PARAMETER_FILENAME).getValue();
			if (contentsParameter==null) {
				if (message!=null) {
					sessionData.writeEntry(filename,message,isCloseInputstreamOnExit(),getCharset());
				}
			} else {
				Message paramValue=Message.asMessage(pvl.get(PARAMETER_CONTENTS).getValue());
				sessionData.writeEntry(filename,paramValue,isCloseInputstreamOnExit(),getCharset());
			}
			return new SenderResult(message);
		} catch (UnsupportedEncodingException e) {
			throw new SenderException(getLogPrefix()+"cannot encode zip entry", e);
		} catch (CompressionException e) {
			throw new SenderException(getLogPrefix()+"cannot store zip entry", e);
		} catch (IOException e) {
			throw new SenderException(getLogPrefix()+"cannot store zip entry", e);
		}
	}


	@IbisDoc({"when set to <code>false</code>, the inputstream is not closed after it has been used", "true"})
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}
	@Deprecated
	@ConfigurationWarning("attribute 'closeStreamOnExit' has been renamed to 'closeInputstreamOnExit'")
	public void setCloseStreamOnExit(boolean b) {
		setCloseInputstreamOnExit(b);
	}
	public boolean isCloseInputstreamOnExit() {
		return closeInputstreamOnExit;
	}

	@IbisDoc({"characterset used for writing zip entry", "utf-8"})
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

}
