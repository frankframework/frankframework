package org.frankframework.management.web.spring;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.Description;
import org.frankframework.management.web.Relation;
import org.frankframework.util.RequestUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlEncodingUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.security.RolesAllowed;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

@RestController
public class TestServiceListener extends FrankApiBase {

	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Relation("testing")
	@Description("view a list of all available service-listeners")
	@GetMapping(value = "/test-servicelistener", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getServiceListeners() throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SERVICE_LISTENER, BusAction.GET);
		return callSyncGateway(builder);
	}

	@RolesAllowed("IbisTester")
	@Relation("testing")
	@Description("send a message to a service listeners, triggering an adapter to process the message")
	@PostMapping(value = "/test-servicelistener", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> postServiceListener(ServiceListenerMultipartBody multipartBody) throws ApiException {
		if(multipartBody == null) {
			throw new ApiException("Missing post parameters");
		}

		String message = null;

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SERVICE_LISTENER, BusAction.UPLOAD);
		builder.addHeader("service", RequestUtils.resolveRequiredProperty("service", multipartBody.getService(), null));
		String fileEncoding = RequestUtils.resolveRequiredProperty("encoding", multipartBody.getEncoding(), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		MultipartFile filePart = multipartBody.getFile();
		if(filePart != null) {
			try {
				InputStream file = filePart.getInputStream();
				message = XmlEncodingUtils.readXml(file, fileEncoding);
			} catch (UnsupportedEncodingException e) {
				throw new ApiException("unsupported file encoding ["+fileEncoding+"]");
			} catch (IOException e) {
				throw new ApiException("error reading file", e);
			}
		} else {
			message = RequestUtils.resolveStringWithEncoding("message", multipartBody.getMessage(), fileEncoding);
		}

		if(StringUtils.isEmpty(message)) {
			throw new ApiException("Neither a file nor a message was supplied", 400);
		}

		builder.setPayload(message);
		return callSyncGateway(builder);
	}

	@Getter
	@Setter
	public static class ServiceListenerMultipartBody {
		private String service;
		private String encoding;
		private MultipartFile file;
		private MultipartFile message;
	}

}
