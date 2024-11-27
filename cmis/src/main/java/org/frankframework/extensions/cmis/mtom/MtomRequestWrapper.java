/*
   Copyright 2021, 2022 WeAreFrank!

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
package org.frankframework.extensions.cmis.mtom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Logger;
import org.springframework.mock.web.DelegatingServletInputStream;

import org.frankframework.http.InputStreamDataSource;
import org.frankframework.http.mime.MultipartEntityBuilder;
import org.frankframework.util.LogUtil;

public class MtomRequestWrapper extends HttpServletRequestWrapper {
	protected Logger log = LogUtil.getLogger(this);

	private HttpEntity entity;
	private static final ContentType MTOM_XOP = ContentType.parse("application/xop+xml; charset=UTF-8");

	public MtomRequestWrapper(ServletRequest request) throws IOException {
		this((HttpServletRequest) request);
	}

	public MtomRequestWrapper(HttpServletRequest request) throws IOException {
		super(request);

		String contentType = super.getHeader("content-type");
		if("POST".equalsIgnoreCase(request.getMethod())) {
			try {
				log.trace("found message with ContentType [{}]", contentType);
				boolean isMultipartRequest = contentType.contains("multipart");
				MultipartEntityBuilder multipart = MultipartEntityBuilder.create();
				multipart.setMtomMultipart();

				if(isMultipartRequest) { // Multiple parts we need to iterate over
					InputStreamDataSource dataSource = new InputStreamDataSource(contentType, super.getInputStream());
					MimeMultipart mp = new MimeMultipart(dataSource);

					int count = mp.getCount();
					for(int i = 0; i < count; i++) {
						BodyPart bp = mp.getBodyPart(i);
						String bodyPartName = "part" + i;
						String fileName = bp.getFileName();

						String[] partName = bp.getHeader("content-id");
						if(partName != null && partName.length > 0) {
							String contentId = partName[0];
							bodyPartName = contentId.substring(1, contentId.length() - 1); // Remove pre and post-fix: < & >
						}

						ContentType partType = ContentType.parse(bp.getContentType());
						log.trace("FileName [{}] PartName [{}] ContentType [{}]",
								StringEscapeUtils.escapeJava(fileName),
								StringEscapeUtils.escapeJava(bodyPartName),
								StringEscapeUtils.escapeJava(partType.toString()));
						multipart.addBinaryBody(bodyPartName, bp.getInputStream(), partType, fileName);

					}
				} else { // Single part we need to convert to a multipart
					multipart.addBinaryBody("part0", super.getInputStream(), MTOM_XOP, null);
				}

				entity = multipart.build();
			} catch (Exception e) {
				log.error("unable to parse or convert message", e);
				throw new IOException("unable to parse message", e);
			}
		}
	}

	// Override this to ensure the correct Content-Type header is used
	@Override
	public String getHeader(String name) {
		if("Content-Type".equalsIgnoreCase(name)) {
			return getContentType();
		}

		return super.getHeader(name);
	}

	@Override
	public String getContentType() {
		if(entity != null) {
			return entity.getContentType().getValue();
		} else {
			return super.getContentType();
		}
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if(entity != null) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			entity.writeTo(outputStream);
			return new DelegatingServletInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
		} else {
			return super.getInputStream();
		}
	}
}
