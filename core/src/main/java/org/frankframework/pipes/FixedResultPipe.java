/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.pipes;

import java.net.URL;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.util.ClassLoaderUtils;

/**
 * This Pipe opens and returns a file from the classpath. The filename is a mandatory parameter to use. You can
 * provide this by using the <code>filename</code> attribute or with a <code>param</code> element to be able to
 * use a sessionKey for instance.
 *
 * @author Johan Verrips
 * @ff.parameters The <code>filename</code> parameter is used to specify the file to open from the classpath. This can be an
 * 		absolute or relative path. If a file is referenced by a relative path, the path is relative to the configuration's root directory.
 * @ff.forward filenotfound the configured file was not found (when this forward isn't specified an exception will be thrown)
 */
@Category("Basic")
@ElementType(ElementTypes.TRANSLATOR)
public class FixedResultPipe extends FixedForwardPipe {

	private static final String FILE_NOT_FOUND_FORWARD = "filenotfound";

	private static final String PARAMETER_FILENAME = "filename";

	private @Getter String filename;

	/**
	 * checks for correct configuration, and checks whether the given filename actually exists
	 */
	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();

		this.filename = determineFilename();

		if (StringUtils.isNotEmpty(getFilename())) {
			URL resource = null;
			try {
				resource = ClassLoaderUtils.getResourceURL(this, getFilename());
			} catch (Throwable e) {
				throw new ConfigurationException("got exception searching for [" + getFilename() + "]", e);
			}
			if (resource == null) {
				throw new ConfigurationException("cannot find resource [" + getFilename() + "]");
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		message.closeOnCloseOf(session, this); // avoid connection leaking when the message itself is not consumed.

		Message result;

		if (StringUtils.isNotEmpty(filename)) {
			URL resource;
			try {
				resource = ClassLoaderUtils.getResourceURL(this, filename);
			} catch (Throwable e) {
				throw new PipeRunException(this, "got exception searching for [" + filename + "]", e);
			}

			if (resource == null) {
				PipeForward fileNotFoundForward = findForward(FILE_NOT_FOUND_FORWARD);
				if (fileNotFoundForward != null) {
					return new PipeRunResult(fileNotFoundForward, message);
				}
				throw new PipeRunException(this, "cannot find resource [" + filename + "]");
			}

			result = new UrlMessage(resource);

			log.debug("returning fixed result filename [{}]", filename);
			if (!Message.isNull(result)) {
				return new PipeRunResult(getSuccessForward(), result);
			}
		}

		return new PipeRunResult(getSuccessForward(), message);
	}

	private String determineFilename() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getFilename())) {
			return getFilename();
		}

		ParameterList parameterList = getParameterList();
		if (parameterList != null && parameterList.findParameter(PARAMETER_FILENAME) != null) {
			return parameterList.findParameter(PARAMETER_FILENAME).getValue();
		}

		throw new ConfigurationException("No filename parameter found");
	}

	/**
	 * Name of the file containing the result message.
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
}
