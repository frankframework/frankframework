package nl.nn.adapterframework.extensions.cmis;

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
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.FolderImpl;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.repository.ObjectFactoryImpl;
import org.apache.chemistry.opencmis.client.runtime.util.EmptyItemIterable;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import nl.nn.adapterframework.senders.SenderTestBase;

public class CmisSenderTestBase extends SenderTestBase<CmisSender> {
	protected static final boolean STUBBED = false;

	@Override
	public CmisSender createSender() throws Exception {
		CmisSender sender = new CmisSender();
		sender.setUsername("test");
		sender.setPassword("test");
		sender.setRepository("test");

		if(!STUBBED) {
			return sender;
		}

		sender = spy(sender);

		sender.setUrl("http://dummy.url");
		sender.setKeepSession(false);

		Session cmisSession = mock(Session.class);
		ObjectFactory objectFactory = mock(ObjectFactoryImpl.class);
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
		doAnswer(cmisObject).when(folder).createDocument(anyMap(), any(), any(VersioningState.class));
		doReturn(folder).when(cmisSession).getRootFolder();
		doAnswer(cmisObject).when(cmisSession).createDocument(anyMap(), any(), any(), any());

//		FIND
		ItemIterable<QueryResult> query = new EmptyItemIterable<QueryResult>();
		doReturn(query).when(cmisSession).query(anyString(), anyBoolean(), any());

		doReturn(new ContentStreamMock()).when(cmisSession).getContentStream(any(), any(), any(), any());

		doReturn(cmisSession).when(sender).createCmisSession(any());

		return sender;
	}

	public static class ObjectIdMock implements Answer<ObjectId> {
		@Override
		public ObjectId answer(InvocationOnMock invocation) throws Throwable {
			String id = (String) invocation.getArguments()[0];
			return new ObjectIdImpl(id);
		}
	}

	public static class ContentStreamMock implements ContentStream {

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
			return new ByteArrayInputStream("dummy_stream".getBytes());
		}

		@Override
		public String getMimeType() {
			return "text/xml";
		}

		@Override
		public long getLength() {
			return 0;
		}

		@Override
		public String getFileName() {
			return null;
		}

		@Override
		public BigInteger getBigLength() {
			return null;
		}
	}
}
