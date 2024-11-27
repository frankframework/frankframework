package org.frankframework.extensions.fxf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.IWrapperPipe.Direction;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;

public class FxfWrapperPipeTest extends PipeTestBase<FxfWrapperPipe> {

	private String fxfDirectory;

	@Override
	public FxfWrapperPipe createPipe() {
		return new FxfWrapperPipe();
	}

	@Override
	@BeforeEach
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
		PipeRunResult pipeRunResult = doPipe(new Message("""
				<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
				<SOAP-ENV:Body>
				<ns0:OnCompletedTransferNotify_Action xmlns:ns0="http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/OnCompletedTransferNotify/1">
				<ns0:TransferFlowId>FlowId</ns0:TransferFlowId>
				<ns0:UserData>FlowId-Instance</ns0:UserData>
				<ns0:ClientFilename>/filename.xml.zip</ns0:ClientFilename>
				<ns0:ServerFilename>/opt/data/FXF/instancename/flowid/in/filename.xml.zip</ns0:ServerFilename>
				<ns0:LocalTransactionID>R629100615</ns0:LocalTransactionID>
				</ns0:OnCompletedTransferNotify_Action>
				</SOAP-ENV:Body>
				</SOAP-ENV:Envelope>
				"""));

		String expected = fxfDirectory+File.separator +"FlXwId"+File.separator +"in"+File.separator +"filename.xml.zip";
		assertEquals(expected, pipeRunResult.getResult().asString());

		String clientFileName = session.getString(pipe.getClientFilenameSessionKey());
		assertEquals("/filename.xml.zip", clientFileName);
	}

	@Test
	public void testUnwrapClientFilenameWithoutSlash() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(new Message("""
				<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
				<SOAP-ENV:Body>
				<ns0:OnCompletedTransferNotify_Action xmlns:ns0="http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/OnCompletedTransferNotify/1">
				<ns0:TransferFlowId>FlowId</ns0:TransferFlowId>
				<ns0:UserData>FlowId-Instance</ns0:UserData>
				<ns0:ClientFilename>filename.xml.zip</ns0:ClientFilename>
				<ns0:ServerFilename>/opt/data/FXF/instancename/flowid/in/filename.xml.zip</ns0:ServerFilename>
				<ns0:LocalTransactionID>R629100615</ns0:LocalTransactionID>
				</ns0:OnCompletedTransferNotify_Action>
				</SOAP-ENV:Body>
				</SOAP-ENV:Envelope>
				"""));

		String expected = fxfDirectory+File.separator +"FlXwId"+File.separator +"in"+File.separator +"filename.xml.zip";
		assertEquals(expected, pipeRunResult.getResult().asString());

		String clientFileName = session.getString(pipe.getClientFilenameSessionKey());
		assertEquals("filename.xml.zip", clientFileName);
	}

	@Test
	public void testClientFileNameContainsPath() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(new Message("""
				<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
				<SOAP-ENV:Body>
				<ns0:OnCompletedTransferNotify_Action xmlns:ns0="http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/OnCompletedTransferNotify/1">
				<ns0:TransferFlowId>FlowId</ns0:TransferFlowId>
				<ns0:UserData>FlowId-Instance</ns0:UserData>
				<ns0:ClientFilename>/opt/data/FXF/instancename/flowid/in/ClientFilename.xml.zip</ns0:ClientFilename>
				<ns0:ServerFilename>/opt/data/FXF/instancename/flowid/in/ServerFilename.xml.zip</ns0:ServerFilename>
				<ns0:LocalTransactionID>R629100615</ns0:LocalTransactionID>
				</ns0:OnCompletedTransferNotify_Action>
				</SOAP-ENV:Body>
				</SOAP-ENV:Envelope>
				"""));
		String expected = fxfDirectory+File.separator +"FlXwId"+File.separator +"in"+File.separator +"ClientFilename.xml.zip";
		assertEquals(expected,pipeRunResult.getResult().asString());
	}

	@Test
	public void testUseServerFilename() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.setUseServerFilename(true);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(new Message("""
				<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
				<SOAP-ENV:Body>
				<ns0:OnCompletedTransferNotify_Action xmlns:ns0="http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/OnCompletedTransferNotify/1">
				<ns0:TransferFlowId>FlowId</ns0:TransferFlowId>
				<ns0:UserData>FlowId-Instance</ns0:UserData>
				<ns0:ClientFilename>/opt/data/FXF/instancename/flowid/in/ClientFilename.xml.zip</ns0:ClientFilename>
				<ns0:ServerFilename>/opt/data/FXF/instancename/flowid/in/ServerFilename.xml.zip</ns0:ServerFilename>
				<ns0:LocalTransactionID>R629100615</ns0:LocalTransactionID>
				</ns0:OnCompletedTransferNotify_Action>
				</SOAP-ENV:Body>
				</SOAP-ENV:Envelope>
				"""));
		String expected = fxfDirectory+File.separator +"FlXwId"+File.separator +"in"+File.separator +"ServerFilename.xml.zip";
		assertEquals(expected,pipeRunResult.getResult().asString());
	}

	@Test
	public void testClientFilenameContainsWindowsPath() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.configure();

		PipeRunResult pipeRunResult = doPipe(new Message("""
				<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
				<SOAP-ENV:Body>
				<ns0:OnCompletedTransferNotify_Action xmlns:ns0="http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/OnCompletedTransferNotify/1">
				<ns0:TransferFlowId>FlowId</ns0:TransferFlowId>
				<ns0:UserData>FlowId-Instance</ns0:UserData>
				<ns0:ClientFilename>C:\\data\\test\\ClientFilename.xml.zip</ns0:ClientFilename>
				<ns0:ServerFilename>/opt/data/FXF/instancename/flowid/in/ServerFilename.xml.zip</ns0:ServerFilename>
				<ns0:LocalTransactionID>R629100615</ns0:LocalTransactionID>
				</ns0:OnCompletedTransferNotify_Action>
				</SOAP-ENV:Body>
				</SOAP-ENV:Envelope>
				"""));
		String expected = fxfDirectory+File.separator +"FlXwId"+File.separator +"in"+File.separator + "ClientFilename.xml.zip";
		assertEquals(expected, pipeRunResult.getResult().asString());
	}
}
