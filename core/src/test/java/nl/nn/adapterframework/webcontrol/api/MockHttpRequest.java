package nl.nn.adapterframework.webcontrol.api;

import java.util.Date;
import java.util.List;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

public class MockHttpRequest implements Request {

	@Override
	public String getMethod() {
		return null;
	}

	@Override
	public Variant selectVariant(List<Variant> variants) {
		return null;
	}

	@Override
	public ResponseBuilder evaluatePreconditions(EntityTag eTag) {
		return null;
	}

	@Override
	public ResponseBuilder evaluatePreconditions(Date lastModified) {
		return null;
	}

	@Override
	public ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
		return null;
	}

	@Override
	public ResponseBuilder evaluatePreconditions() {
		return null;
	}

}
