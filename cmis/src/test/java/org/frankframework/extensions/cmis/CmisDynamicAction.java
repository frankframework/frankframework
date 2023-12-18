package org.frankframework.extensions.cmis;

import org.frankframework.extensions.cmis.server.CmisEvent;
import org.frankframework.extensions.cmis.server.CmisEventDispatcher;
import org.junit.jupiter.api.Test;

import org.frankframework.testutil.TestAssertions;

public class CmisDynamicAction extends CmisSenderTestBase {

	@Test
	public void canConfigure() throws Exception {
		sender.setBindingType(CmisSessionBuilder.BindingTypes.BROWSER);
		sender.setAction(CmisSender.CmisAction.DYNAMIC);
		sender.configure();
	}

	@Test
	public void deleteObject() throws Exception {
		sender.setBindingType(CmisSessionBuilder.BindingTypes.BROWSER);
		sender.setAction(CmisSender.CmisAction.DYNAMIC);
		sender.setResultOnNotFound("document-not-found");
		sender.configure();
		session.put(CmisEventDispatcher.CMIS_EVENT_KEY, CmisEvent.DELETE_OBJECT.getLabel());

		String result = sendMessage("<cmis><id>testId</id></cmis>").asString();
		TestAssertions.assertEqualsIgnoreCRLF("<cmis deleted=\"true\"/>", result);
	}

	@Test
	public void createDocument() throws Exception {
		sender.setBindingType(CmisSessionBuilder.BindingTypes.BROWSER);
		sender.setAction(CmisSender.CmisAction.DYNAMIC);
		sender.setResultOnNotFound("document-not-found");
		sender.configure();
		session.put(CmisEventDispatcher.CMIS_EVENT_KEY, CmisEvent.CREATE_DOCUMENT.getLabel());
		session.put("ContentStream", "text");

		String result = sendMessage("<cmis><id>testId</id><properties><property name=\"cmis:name\">dummy</property></properties><versioningState>NONE</versioningState><contentStream filename=\"file.txt\" length=\"4\" mimeType=\"text/plain\"></contentStream></cmis>").asString();
		TestAssertions.assertEqualsIgnoreCRLF("<cmis>\n\t<id>ZHVtbXk=</id>\n</cmis>", result);
	}

	@Test
	public void getContentStream() throws Exception {
		sender.setBindingType(CmisSessionBuilder.BindingTypes.BROWSER);
		sender.setAction(CmisSender.CmisAction.DYNAMIC);
		sender.setResultOnNotFound("document-not-found");
		sender.configure();
		session.put(CmisEventDispatcher.CMIS_EVENT_KEY, CmisEvent.GET_CONTENTSTREAM.getLabel());
		session.put("ContentStream", "text");

		String result = sendMessage("<cmis><id>testId</id></cmis>").asString();
		TestAssertions.assertEqualsIgnoreCRLF("<cmis>\n\t<id>dGVzdElk</id>\n\t<contentStream length=\"12\" mimeType=\"text/xml\"/>\n</cmis>", result);
	}
}
