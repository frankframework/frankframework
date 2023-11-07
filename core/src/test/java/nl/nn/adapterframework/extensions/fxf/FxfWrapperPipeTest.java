package nl.nn.adapterframework.extensions.fxf;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import nl.nn.adapterframework.core.IWrapperPipe.Direction;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;

public class FxfWrapperPipeTest extends PipeTestBase<FxfWrapperPipe> {

	private String fxfDirectory;

	@Override
	public FxfWrapperPipe createPipe() {
		return new FxfWrapperPipe();
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		String logDir = AppConstants.getInstance().getString("log.dir", null);
		File fxfFileDir = new File(logDir, "fxf");
		fxfFileDir.mkdirs();
		fxfDirectory = fxfFileDir.getCanonicalPath();
		log.info("using fxf directory [{}]", fxfDirectory);
		AppConstants.getInstance().setProperty("fxf.dir", fxfDirectory);
	}

	@Test
	public void testBasicUnwrap() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(new Message("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"<SOAP-ENV:Body>\n" +
				"<ns0:OnCompletedTransferNotify_Action xmlns:ns0=\"http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/OnCompletedTransferNotify/1\">\n" +
				"<ns0:TransferFlowId>FlowId</ns0:TransferFlowId>\n" +
				"<ns0:UserData>FlowId-Instance</ns0:UserData>\n" +
				"<ns0:ClientFilename>/filename.xml.zip</ns0:ClientFilename>\n" +
				"<ns0:ServerFilename>/opt/data/FXF/instancename/flowid/in/filename.xml.zip</ns0:ServerFilename>\n" +
				"<ns0:LocalTransactionID>R629100615</ns0:LocalTransactionID>\n" +
				"</ns0:OnCompletedTransferNotify_Action>\n" +
				"</SOAP-ENV:Body>\n" +
				"</SOAP-ENV:Envelope>\n" +
				""));

		String expected = fxfDirectory+File.separator +"FlXwId"+File.separator +"in"+File.separator +"filename.xml.zip";
		assertEquals(expected, pipeRunResult.getResult().asString());

		String clientFileName = session.getString(pipe.getClientFilenameSessionKey());
		assertEquals("/filename.xml.zip", clientFileName);
	}

	@Test
	public void testUnwrapClientFilenameWithoutSlash() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(new Message("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"<SOAP-ENV:Body>\n" +
				"<ns0:OnCompletedTransferNotify_Action xmlns:ns0=\"http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/OnCompletedTransferNotify/1\">\n" +
				"<ns0:TransferFlowId>FlowId</ns0:TransferFlowId>\n" +
				"<ns0:UserData>FlowId-Instance</ns0:UserData>\n" +
				"<ns0:ClientFilename>filename.xml.zip</ns0:ClientFilename>\n" +
				"<ns0:ServerFilename>/opt/data/FXF/instancename/flowid/in/filename.xml.zip</ns0:ServerFilename>\n" +
				"<ns0:LocalTransactionID>R629100615</ns0:LocalTransactionID>\n" +
				"</ns0:OnCompletedTransferNotify_Action>\n" +
				"</SOAP-ENV:Body>\n" +
				"</SOAP-ENV:Envelope>\n" +
				""));

		String expected = fxfDirectory+File.separator +"FlXwId"+File.separator +"in"+File.separator +"filename.xml.zip";
		assertEquals(expected, pipeRunResult.getResult().asString());

		String clientFileName = session.getString(pipe.getClientFilenameSessionKey());
		assertEquals("filename.xml.zip", clientFileName);
	}

	@Test
	public void testClientFileNameContainsPath() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(new Message("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"<SOAP-ENV:Body>\n" +
				"<ns0:OnCompletedTransferNotify_Action xmlns:ns0=\"http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/OnCompletedTransferNotify/1\">\n" +
				"<ns0:TransferFlowId>FlowId</ns0:TransferFlowId>\n" +
				"<ns0:UserData>FlowId-Instance</ns0:UserData>\n" +
				"<ns0:ClientFilename>/opt/data/FXF/instancename/flowid/in/ClientFilename.xml.zip</ns0:ClientFilename>\n" +
				"<ns0:ServerFilename>/opt/data/FXF/instancename/flowid/in/ServerFilename.xml.zip</ns0:ServerFilename>\n" +
				"<ns0:LocalTransactionID>R629100615</ns0:LocalTransactionID>\n" +
				"</ns0:OnCompletedTransferNotify_Action>\n" +
				"</SOAP-ENV:Body>\n" +
				"</SOAP-ENV:Envelope>\n" +
				""));
		String expected = fxfDirectory+File.separator +"FlXwId"+File.separator +"in"+File.separator +"ClientFilename.xml.zip";
		assertEquals(expected,pipeRunResult.getResult().asString());
	}

	@Test
	public void testUseServerFilename() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.setUseServerFilename(true);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(new Message("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"<SOAP-ENV:Body>\n" +
				"<ns0:OnCompletedTransferNotify_Action xmlns:ns0=\"http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/OnCompletedTransferNotify/1\">\n" +
				"<ns0:TransferFlowId>FlowId</ns0:TransferFlowId>\n" +
				"<ns0:UserData>FlowId-Instance</ns0:UserData>\n" +
				"<ns0:ClientFilename>/opt/data/FXF/instancename/flowid/in/ClientFilename.xml.zip</ns0:ClientFilename>\n" +
				"<ns0:ServerFilename>/opt/data/FXF/instancename/flowid/in/ServerFilename.xml.zip</ns0:ServerFilename>\n" +
				"<ns0:LocalTransactionID>R629100615</ns0:LocalTransactionID>\n" +
				"</ns0:OnCompletedTransferNotify_Action>\n" +
				"</SOAP-ENV:Body>\n" +
				"</SOAP-ENV:Envelope>\n" +
				""));
		String expected = fxfDirectory+File.separator +"FlXwId"+File.separator +"in"+File.separator +"ServerFilename.xml.zip";
		assertEquals(expected,pipeRunResult.getResult().asString());
	}

	@Test
	public void testClientFilenameContainsWindowsPath() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.configure();

		PipeRunResult pipeRunResult = doPipe(new Message("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"<SOAP-ENV:Body>\n" +
				"<ns0:OnCompletedTransferNotify_Action xmlns:ns0=\"http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/OnCompletedTransferNotify/1\">\n" +
				"<ns0:TransferFlowId>FlowId</ns0:TransferFlowId>\n" +
				"<ns0:UserData>FlowId-Instance</ns0:UserData>\n" +
				"<ns0:ClientFilename>C:\\data\\test\\ClientFilename.xml.zip</ns0:ClientFilename>\n" +
				"<ns0:ServerFilename>/opt/data/FXF/instancename/flowid/in/ServerFilename.xml.zip</ns0:ServerFilename>\n" +
				"<ns0:LocalTransactionID>R629100615</ns0:LocalTransactionID>\n" +
				"</ns0:OnCompletedTransferNotify_Action>\n" +
				"</SOAP-ENV:Body>\n" +
				"</SOAP-ENV:Envelope>\n" +
				""));
		String expected = fxfDirectory+File.separator +"FlXwId"+File.separator +"in"+File.separator + "ClientFilename.xml.zip";
		assertEquals(expected, pipeRunResult.getResult().asString());
	}
}
