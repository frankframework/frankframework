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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.Misc;

/**
 * Stream an input stream to an output stream.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * </table>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>inputStream</td><td>the input stream object to use instead of an input stream object taken from pipe input</td></tr>
 * <tr><td>outputStream</td><td>the output stream object to use unless httpResponse parameter is specified</td></tr>
 * <tr><td>httpResponse</td><td>an HttpServletResponse object to stream to (the output stream is retrieved by calling getOutputStream() on the HttpServletResponse object)</td></tr>
 * <tr><td>httpRequest</td><td>an HttpServletRequest object to stream from. Each part is put in a session key and the result of this pipe is a xml with info about these parts and the name of the session key</td></tr>
 * <tr><td>contentType</td><td>the Content-Type header to set in case httpResponse was specified</td></tr>
 * <tr><td>contentDisposition</td><td>the Content-Disposition header to set in case httpResponse was specified</td></tr>
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

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		Object result = input;
		String inputString;
		if (input instanceof String) {
			inputString = (String)input;
		} else {
			inputString = "";
		}
		ParameterResolutionContext prc = new ParameterResolutionContext(inputString, session, isNamespaceAware());
		Map parameters = null;
		ParameterList parameterList = getParameterList();
		if (parameterList != null) {
			try {
				parameters = prc.getValueMap(parameterList);
			} catch (ParameterException e) {
				throw new PipeRunException(this, "Could not resolve parameters", e);
			}
		}
		InputStream inputStream = null;
		OutputStream outputStream = null;
		HttpServletRequest httpRequest = null;
		HttpServletResponse httpResponse = null;
		String contentType = null;
		String contentDisposition = null;
		if (parameters != null) {
			if (parameters.get("inputStream") != null) {
				inputStream = (InputStream)parameters.get("inputStream");
			}
			if (parameters.get("outputStream") != null) {
				outputStream = (OutputStream)parameters.get("outputStream");
			}
			if (parameters.get("httpRequest") != null) {
				httpRequest = (HttpServletRequest)parameters.get("httpRequest");
			}
			if (parameters.get("httpResponse") != null) {
				httpResponse = (HttpServletResponse)parameters.get("httpResponse");
			}
			if (parameters.get("contentType") != null) {
				contentType = (String)parameters.get("contentType");
			}
			if (parameters.get("contentDisposition") != null) {
				contentDisposition = (String)parameters.get("contentDisposition");
			}
		}
		if (inputStream == null) {
			if (input instanceof InputStream) {
				inputStream = (InputStream)input;
			}
		}
		try {
			if (httpResponse != null) {
				HttpSender.streamResponseBody(inputStream, contentType,
						contentDisposition, httpResponse, log,
						getLogPrefix(session));
			} else if (httpRequest != null) {
				StringBuilder resultString = new StringBuilder("<parts>");
				if (ServletFileUpload.isMultipartContent(httpRequest)) {
					log.debug(getLogPrefix(session) + " request contains multipart content");
					DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
					ServletFileUpload servletFileUpload = new ServletFileUpload(
							diskFileItemFactory);
					List<FileItem> items = servletFileUpload
							.parseRequest(httpRequest);
					int fileCounter = 0, stringCounter = 0;
					for (FileItem item : items) {
						if (item.isFormField()) {
							// Process regular form field (input
							// type="text|radio|checkbox|etc", select, etc).
							String fieldName = "part_string" + (++stringCounter>1?stringCounter:"");
							String fieldValue = item.getString();
							log.debug(getLogPrefix(session)
									+ "setting parameter [" + fieldName
									+ "] to [" + fieldValue + "]");
							session.put(fieldName, fieldValue);
							resultString
									.append("<part type=\"string\" sessionKey=\""
											+ fieldName + "\" size=\""
											+ fieldValue.length() + "\"/>");
						} else {
							// Process form file field (input type="file").
							String fieldName = "part_file" + (++fileCounter>1?fileCounter:"");
							String fileName = FilenameUtils
									.getName(item.getName());
							InputStream is = item.getInputStream();
							int size = is.available();
							if (size > 0) {
								log.debug(getLogPrefix(session)
										+ "setting parameter [" + fieldName
										+ "] to input stream of file ["
										+ fileName + "]");
								session.put(fieldName, is);
							} else {
								log.debug(getLogPrefix(session)
										+ "setting parameter [" + fieldName
										+ "] to [" + null + "]");
								session.put(fieldName, null);
							}
							resultString.append("<part type=\"file\" name=\""
									+ fileName + "\" sessionKey=\"" + fieldName
									+ "\" size=\"" + size + "\"/>");
						}
					}
				} else {
					log.debug(getLogPrefix(session) + " request does not contain multipart content");
				}
				resultString.append("</parts>");
				result = resultString.toString();
			} else {
				Misc.streamToStream(inputStream, outputStream);
			}
		} catch (IOException e) {
			throw new PipeRunException(this, "IOException streaming input to output", e);
		} catch (FileUploadException e) {
			throw new PipeRunException(this, "FileUploadException getting multiparts from httpServletRequest", e);
		}
		return new PipeRunResult(getForward(), result);
	}
}
