package nl.nn.adapterframework.extensions.cmis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.FolderImpl;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.repository.ObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.codec.binary.Base64;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

@RunWith(Parameterized.class)
public class TestCreateAction extends SenderBase<CmisSender>{
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	private final static String EMPTY_INPUT = "";
	private final static String INPUT = "<cmis><objectId>dummy</objectId><objectTypeId>cmis:document</objectTypeId><fileName>fileInput.txt</fileName>"
			+ "<properties><property name=\"project:number\" type=\"integer\">123456789</property>"
			+ "<property name=\"project:lastModified\" type=\"datetime\">2019-02-26 16:31:15</property>"
			+ "<property name=\"project:onTime\" type=\"boolean\">true</property></properties></cmis>";
	private final static String CREATE_RESULT = "dummy_id";
	
	@Parameters(name = "{0} - {1} - {2}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "atompub", "create", EMPTY_INPUT, CREATE_RESULT },
				{ "atompub", "create", INPUT, CREATE_RESULT },

				{ "webservices", "create", EMPTY_INPUT, CREATE_RESULT },
				{ "webservices", "create", INPUT, CREATE_RESULT },

				{ "browser", "create", EMPTY_INPUT, CREATE_RESULT },
				{ "browser", "create", INPUT, CREATE_RESULT },
		});
	}
	
	private String bindingType;
	private String action;
	private String input;
	private String expectedResult;

	public TestCreateAction(String bindingType, String action, String input, String expected) {
		this.bindingType = bindingType;
		this.action = action;
		this.input = input;
		this.expectedResult = expected;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public CmisSender createSender() {
		CmisSender sender = spy(new CmisSender());

		sender.setUrl("http://dummy.url");
		sender.setRepository("dummyRepository");
		sender.setUserName("test");
		sender.setPassword("test");
		sender.setKeepSession(false);

		Session cmisSession = mock(Session.class);
		ObjectFactory objectFactory = mock(ObjectFactoryImpl.class);
		doReturn(objectFactory).when(cmisSession).getObjectFactory();

//			GENERIC cmis object
		ObjectId objectId = mock(ObjectIdImpl.class);
		doReturn(objectId).when(cmisSession).createObjectId(anyString());
		CmisObject cmisObject = spy(new CmisTestObject());
		doReturn(cmisObject).when(cmisSession).getObject(any(ObjectId.class));
		doReturn(cmisObject).when(cmisSession).getObject(any(ObjectId.class), any(OperationContext.class));
		
//			CREATE
		Folder folder = mock(FolderImpl.class);
		doReturn(cmisObject).when(folder).createDocument(anyMap(), any(ContentStreamImpl.class), any(VersioningState.class));
		doReturn(folder).when(cmisSession).getRootFolder();
		doReturn(objectId).when(cmisSession).createDocument(anyMap(), any(ObjectId.class), any(ContentStreamImpl.class), any(VersioningState.class));
		doReturn("dummy_id").when(objectId).getId();
		
		try {
			doReturn(cmisSession).when(sender).getSession(any(ParameterResolutionContext.class));
		} catch (SenderException e) {
			//Since we stub the entire session it won't throw exceptions
		}

		return sender;
	}

	
	public void configure() throws ConfigurationException, SenderException, TimeOutException {
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();
	}

	@Test
	public void fileContentFromSessionKeyAsString() throws ConfigurationException, SenderException, TimeOutException {
		ParameterResolutionContext prc = new ParameterResolutionContext("input", session);
		sender.setGetProperties(true);
		session.put("fileContent", new String(Base64.encodeBase64("some content here for test FileContent as String".getBytes())));
		sender.setFileContentSessionKey("fileContent");
		sender.setUseRootFolder(false);
		configure();
		String actualResult = sender.sendMessage(bindingType+"-"+action, input, prc);
		assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}
	
	@Test
	public void fileContentFromSessionKeyAsByteArray() throws ConfigurationException, SenderException, TimeOutException {
		ParameterResolutionContext prc = new ParameterResolutionContext("input", session);
		sender.setGetProperties(true);
		session.put("fileContent", "some content here for test fileContent as byte array".getBytes());
		sender.setFileContentSessionKey("fileContent");
		configure();
		String actualResult = sender.sendMessage(bindingType+"-"+action, input, prc);
		assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}
	
	@Test
	public void fileContentFromSessionKeyAsInputStream() throws ConfigurationException, SenderException, TimeOutException, IOException {
		ParameterResolutionContext prc = new ParameterResolutionContext("input", session);
		sender.setGetProperties(true);
		session.put("fileContent", getClass().getResource("/fileInput.txt").openStream());
		sender.setFileContentSessionKey("fileContent");
		configure();
		String actualResult = sender.sendMessage(bindingType+"-"+action, input, prc);
		assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}
	
	@Test
	public void fileStreamFromSessionKeyAsString() throws ConfigurationException, SenderException, TimeOutException {
		ParameterResolutionContext prc = new ParameterResolutionContext("input", session);
		sender.setGetProperties(true);
		session.put("fis", new String(Base64.encodeBase64("some content here for test FileStream as String".getBytes())));
		sender.setFileInputStreamSessionKey("fis");
		configure();
		String actualResult = sender.sendMessage(bindingType+"-"+action, input, prc);
		assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}
	
	@Test
	public void fileStreamFromSessionKeyAsByteArray() throws ConfigurationException, SenderException, TimeOutException {
		ParameterResolutionContext prc = new ParameterResolutionContext("input", session);
		sender.setGetProperties(true);
		session.put("fis", "some content here for test FileStream as byte array".getBytes());
		sender.setFileInputStreamSessionKey("fis");
		configure();
		String actualResult = sender.sendMessage(bindingType+"-"+action, input, prc);
		assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}
	
	@Test
	public void fileStreamFromSessionKeyAsInputStream() throws ConfigurationException, SenderException, TimeOutException, IOException {
		ParameterResolutionContext prc = new ParameterResolutionContext("input", session);
		sender.setGetProperties(true);
		session.put("fis", getClass().getResource("/fileInput.txt").openStream());
		sender.setFileInputStreamSessionKey("fis");
		configure();
		String actualResult = sender.sendMessage(bindingType+"-"+action, input, prc);
		assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}
	
	@Test
	public void fileStreamFromSessionKeyWithIllegalType() throws ConfigurationException, SenderException, TimeOutException, IOException {
		exception.expect(SenderException.class);
		exception.expectMessage("expected InputStream, ByteArray or Base64-String but got");
		ParameterResolutionContext prc = new ParameterResolutionContext("input", session);
		sender.setGetProperties(true);
		session.put("fis", 1);
		sender.setFileInputStreamSessionKey("fis");
		configure();
		String actualResult = sender.sendMessage(bindingType+"-"+action, input, prc);
		assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

}
