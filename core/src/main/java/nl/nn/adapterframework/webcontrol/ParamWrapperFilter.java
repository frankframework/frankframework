/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package nl.nn.adapterframework.webcontrol;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

/**
 * ParamWrapperFilter.
 * 
 * @author Alvaro Munoz, HP Fortify Team
 * @author Rene Gielen, Apache Struts Team
 */
public class ParamWrapperFilter implements Filter {

	private static final Log LOG = LogFactory.getLog(ParamWrapperFilter.class);
	protected Logger iaflog = LogUtil.getLogger(this);

	private static final String DEFAULT_BLACKLIST_PATTERN = "(.*\\.|^|.*|\\[('|\"))(c|C)lass(\\.|('|\")]|\\[).*";
	private static final String INIT_PARAM_NAME = "excludeParams";

	private Pattern pattern;

	public void init(FilterConfig filterConfig) throws ServletException {
		final String toCompile;
		final String initParameter = filterConfig
				.getInitParameter(INIT_PARAM_NAME);
		if (initParameter != null && initParameter.trim().length() > 0) {
			toCompile = initParameter;
		} else {
			toCompile = DEFAULT_BLACKLIST_PATTERN;
		}

		iaflog.info("INFO Message: Struts1 'do'-Filter active");
		this.pattern = Pattern.compile(toCompile, Pattern.DOTALL);
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		// to prevent NPE (????)
		request.getParameterMap();

		ParamFilteredRequest wrapper = null;
		if (request instanceof HttpServletRequest)
			wrapper = new ParamFilteredRequest(request, pattern);

		if (wrapper != null)
			chain.doFilter(wrapper, response);
		else
			chain.doFilter(request, response);
	}

	public void destroy() {
	}

	static class ParamFilteredRequest extends HttpServletRequestWrapper {

		private static final int BUFFER_SIZE = 128;
		private static final String CONTENT_LENGTH_PATTERN = "(?i)content-length";

		private final String body;
		private final Pattern pattern;
		private final Pattern content_length_pattern;
		private boolean read_stream = false;

		public ParamFilteredRequest(ServletRequest request, Pattern pattern) {
			super((HttpServletRequest) request);
			this.pattern = pattern;
			this.content_length_pattern = Pattern.compile(
					CONTENT_LENGTH_PATTERN, Pattern.DOTALL);

			StringBuilder stringBuilder = new StringBuilder();
			BufferedReader bufferedReader = null;

			try {
				InputStream inputStream = request.getInputStream();

				if (inputStream != null) {
					String characterEncoding = this.getCharacterEncoding();
					if (characterEncoding == null) {
						bufferedReader = new BufferedReader(
								new InputStreamReader(inputStream));
					} else {
						bufferedReader = new BufferedReader(
								new InputStreamReader(inputStream,
										characterEncoding));
					}
					char[] charBuffer = new char[BUFFER_SIZE];
					int bytesRead = -1;

					while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
						stringBuilder.append(charBuffer, 0, bytesRead);
					}
				} else {
					stringBuilder.append("");
				}
			} catch (IOException ex) {
				logCatchedException(ex);
			} finally {
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException ex) {
						logCatchedException(ex);
					}
				}
			}
			body = stringBuilder.toString();
		}

		@Override
		public Enumeration getParameterNames() {
			List finalParameterNames = new ArrayList();
			List parameterNames = Collections.list((Enumeration) super
					.getParameterNames());
			final Iterator iterator = parameterNames.iterator();
			while (iterator.hasNext()) {
				String parameterName = (String) iterator.next();
				if (!pattern.matcher(parameterName).matches()) {
					finalParameterNames.add(parameterName);
				}
			}
			return Collections.enumeration(finalParameterNames);
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			if (LOG.isTraceEnabled()) {
				LOG.trace(body);
			}
			final ByteArrayInputStream byteArrayInputStream;
			if (pattern.matcher(body).matches()) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("[getInputStream]: found body to match blacklisted parameter pattern");
				}
				byteArrayInputStream = new ByteArrayInputStream("".getBytes());
			} else if (read_stream) {
				byteArrayInputStream = new ByteArrayInputStream("".getBytes());
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("[getInputStream]: OK - body does not match blacklisted parameter pattern");
				}
				byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
				read_stream = true;
			}

			return new ServletInputStream() {
				public int read() throws IOException {
					return byteArrayInputStream.read();
				}
			};
		}

		@Override
		public String getHeader(String name) {
			if (pattern.matcher(body).matches()
					&& content_length_pattern.matcher(name).matches()) {
				return "0";
			}
			return super.getHeader(name);
		}

		@Override
		public int getContentLength() {
			if (pattern.matcher(body).matches()) {
				return 0;
			}
			return super.getContentLength();
		}

		private void logCatchedException(IOException ex) {
			LOG.error("[ParamFilteredRequest]: Exception catched: ", ex);
		}

	}
}
