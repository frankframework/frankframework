package org.frankframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.runtime.FolderImpl;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.repository.ObjectFactoryImpl;
import org.apache.chemistry.opencmis.client.runtime.util.EmptyItemIterable;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.frankframework.senders.SenderTestBase;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.StreamUtil;

public class CmisSenderTestBase extends SenderTestBase<CmisSender> {
	protected static final boolean STUBBED = true;
	private static final String ENDPOINT = "http://localhost:8080";

	@Override
	public CmisSender createSender() throws Exception {
		CmisSender sender = new CmisSender() {
			@Override
			public void setBindingType(CmisSessionBuilder.BindingTypes bindingType) {
				super.setBindingType(bindingType);

				switch (bindingType) {
				case ATOMPUB:
					setUrl(ENDPOINT+"/atom11");
					break;
				case WEBSERVICES:
					setUrl(ENDPOINT+"/services11/cmis");
					break;
				case BROWSER:
					setUrl(ENDPOINT+"/browser");
					break;
				default:
					fail("BindingType ["+bindingType+"] not implemented");
					break;
				}
			}
		};
		sender.setUsername("test");
		sender.setPassword("test");
		sender.setRepository("test");

		if(!STUBBED) {
			return sender;
		}

		sender = spy(sender);

		sender.setKeepSession(false);

		CloseableCmisSession cmisSession = mock(CloseableCmisSession.class);
		ObjectFactory objectFactory = mock(ObjectFactoryImpl.class);
		doAnswer(new ContentStreamMock()).when(objectFactory).createContentStream(anyString(), anyLong(), anyString(), any(InputStream.class));
		doReturn(objectFactory).when(cmisSession).getObjectFactory();

//		GENERIC cmis object
		doAnswer(new ObjectIdMock()).when(cmisSession).createObjectId(anyString());
		CmisTestObject cmisObject = CmisTestObject.newInstance();

		doAnswer(cmisObject).when(cmisSession).getObject(any(ObjectId.class));
		doAnswer(cmisObject).when(cmisSession).getObject(any(ObjectId.class), any(OperationContext.class));

//		GET
		OperationContext operationContext = mock(OperationContextImpl.class);
		doReturn(operationContext).when(cmisSession).createOperationContext();

//		CREATE
		Folder folder = mock(FolderImpl.class);
		doAnswer(cmisObject).when(folder).createDocument(anyMap(), any(), any(VersioningState.class)); //improve test to validate the ContentStream!
		doReturn(folder).when(cmisSession).getRootFolder();
		doAnswer(cmisObject).when(cmisSession).createDocument(anyMap(), any(), any(), any());

//		FIND
		ItemIterable<QueryResult> query = new EmptyItemIterable<>();
		doReturn(query).when(cmisSession).query(anyString(), anyBoolean(), any());

		doReturn(new ContentStreamMock()).when(cmisSession).getContentStream(any(), any(), any(), any());

		doReturn(cmisSession).when(sender).createCmisSession(any());

		return sender;
	}

	@Override
	@Disabled("the CmisSender is tested in other classes")
	@Test
	@SuppressWarnings("java:S2699") // Disabled test, does not require an assertion!
	public void testIfToStringWorks() {
		// Disable this test, the CmisSender is tested in other classes.
	}

	public static class ObjectIdMock implements Answer<ObjectId> {
		@Override
		public ObjectId answer(InvocationOnMock invocation) throws Throwable {
			String id = (String) invocation.getArguments()[0];
			return new ObjectIdImpl(id);
		}
	}

	public static class ContentStreamMock implements ContentStream, Answer<ContentStream> {
		private byte[] stream = "dummy_stream".getBytes();
		private String name = null;
		private String mimeType = "text/xml";

		@Override
		public ContentStream answer(InvocationOnMock invocation) throws Throwable {
			name = (String) invocation.getArguments()[0];
			long length = (long) invocation.getArguments()[1];
			this.mimeType = (String) invocation.getArguments()[2];
			InputStream content = (InputStream) invocation.getArguments()[3];

			stream = StreamUtil.streamToBytes(content);
			if(length > 0) { //if a length has been provided, validate it.
				assertEquals(stream.length, length);
			}

			if(name.startsWith("/fileInput-")) {
				String testFileContent = TestFileUtils.getTestFile("/fileInput.txt");
				assertEquals(testFileContent, new String(stream));
			}

			return this;
		}

		@Override
		public void setExtensions(List<CmisExtensionElement> extensions) {
			//Can't set something that doesn't exist!
		}

		@Override
		public List<CmisExtensionElement> getExtensions() {
			return null;
		}

		@Override
		public InputStream getStream() {
			return new ByteArrayInputStream(stream);
		}

		@Override
		public String getMimeType() {
			return mimeType;
		}

		@Override
		public long getLength() {
			return stream.length;
		}

		@Override
		public String getFileName() {
			return name;
		}

		@Override
		public BigInteger getBigLength() {
			return null;
		}
	}
}
