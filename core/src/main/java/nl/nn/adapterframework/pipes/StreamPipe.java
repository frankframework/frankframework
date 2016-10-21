/*
   Copyright 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;

/**
 * Stream an input stream to an output stream.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputStreamSessionKey(String) inputStreamSessionKey}</td><td>when set, the input stream object is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputStreamSessionKey(String) outputStreamSessionKey}</td><td>the session key which holds the output stream object unless httpResponseSessionKey is specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHttpResponseSessionKey(String) httpResponseSessionKey}</td><td>the session key which holds an HttpServletResponse object to stream to (the output stream is retrieved by calling getOutputStream() on the HttpServletResponse object)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setContentType(String) contentType}</td><td>the Content-Type header to set in case httpResponseSessionKey was specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setContentDisposition(String) contentDisposition}</td><td>the Content-Disposition header to set in case httpResponseSessionKey was specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author Jaco de Groot
 */
public class StreamPipe extends FixedForwardPipe {
	String inputStreamSessionKey;
	String outputStreamSessionKey;
	String httpResponseSessionKey;
	String contentType;
	String contentDisposition;

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		InputStream inputStream;
		if (StringUtils.isNotEmpty(getInputStreamSessionKey())) {
			inputStream = (InputStream)session.get(getInputStreamSessionKey());
		} else {
			inputStream = (InputStream)input;
		}
		try {
			if (StringUtils.isNotEmpty(getHttpResponseSessionKey())) {
				HttpServletResponse response = (HttpServletResponse)session.get(getHttpResponseSessionKey());
				HttpSender.streamResponseBody(inputStream, contentType,
						contentDisposition, response, log,
						getLogPrefix(session));
			} else if (StringUtils.isNotEmpty(getOutputStreamSessionKey())) {
				Misc.streamToStream(inputStream, (OutputStream)session.get(getOutputStreamSessionKey()));
			}
		} catch (IOException e) {
			throw new PipeRunException(this, "IOException streaming input to output", e);
		}
		return new PipeRunResult(getForward(), input);
	}

	public String getInputStreamSessionKey() {
		return inputStreamSessionKey;
	}

	public void setInputStreamSessionKey(String inputStreamSessionKey) {
		this.inputStreamSessionKey = inputStreamSessionKey;
	}

	public String getOutputStreamSessionKey() {
		return outputStreamSessionKey;
	}

	public void setOutputStreamSessionKey(String outputStreamSessionKey) {
		this.outputStreamSessionKey = outputStreamSessionKey;
	}

	public String getHttpResponseSessionKey() {
		return httpResponseSessionKey;
	}

	public void setHttpResponseSessionKey(String httpResponseSessionKey) {
		this.httpResponseSessionKey = httpResponseSessionKey;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentDisposition() {
		return contentDisposition;
	}

	public void setContentDisposition(String contentDisposition) {
		this.contentDisposition = contentDisposition;
	}

}
