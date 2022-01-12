package nl.nn.adapterframework.extensions.cmis.mtom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Logger;
import org.springframework.mock.web.DelegatingServletInputStream;

import nl.nn.adapterframework.http.InputStreamDataSource;
import nl.nn.adapterframework.http.mime.MultipartEntityBuilder;
import nl.nn.adapterframework.util.LogUtil;

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
				if(log.isTraceEnabled()) log.trace("found message with ContentType ["+contentType+"]");
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
						if(log.isTraceEnabled()) log.trace("FileName ["+fileName+"] PartName ["+bodyPartName+"] ContentType [" + partType + "]");
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