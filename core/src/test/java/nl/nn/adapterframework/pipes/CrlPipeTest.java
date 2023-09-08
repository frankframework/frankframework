package nl.nn.adapterframework.pipes;

import static nl.nn.adapterframework.testutil.MatchUtils.assertTestFileEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class CrlPipeTest extends PipeTestBase<CrlPipe> {

	private final String CRL_FILE="/Pipes/CrlPipe/LatestCRL.crl";
	private final String CRL_AS_XML="/Pipes/CrlPipe/LatestCRL.xml";
	private final String CRL_ISSUER_CERT_FILE="/Pipes/CrlPipe/ABC-issuer.cer";
	private final String CRL_ISSUER_CERT_FILE_WRONG="/Pipes/CrlPipe/ABC2-issuer.cer";

	@Override
	public CrlPipe createPipe() {
		return new CrlPipe();
	}

	@Test
	public void testCrlViaMessage() throws Exception {
		// arrange
		String issuerKey="issuer";
		pipe.setIssuerSessionKey(issuerKey);
		configureAndStartPipe();

		Message issuer_cert = Message.asMessage(TestFileUtils.getTestFileURL(CRL_ISSUER_CERT_FILE).openStream());
		Message crl = Message.asMessage(TestFileUtils.getTestFileURL(CRL_FILE).openStream());
		session.put(issuerKey, issuer_cert);

		// act
		PipeRunResult prr = doPipe(crl);

		// assert
		assertEquals("success", prr.getPipeForward().getName());
		assertTestFileEquals(CRL_AS_XML, prr.getResult().asString());
	}

	@Test
	public void testCrlViaInputStream() throws Exception {
		// arrange
		String issuerKey="issuer";
		pipe.setIssuerSessionKey(issuerKey);
		configureAndStartPipe();

		InputStream issuer_cert = TestFileUtils.getTestFileURL(CRL_ISSUER_CERT_FILE).openStream();
		Message crl = Message.asMessage(TestFileUtils.getTestFileURL(CRL_FILE).openStream());
		session.put(issuerKey, issuer_cert);

		// act
		PipeRunResult prr = doPipe(crl);

		// assert
		assertEquals("success", prr.getPipeForward().getName());
		assertTestFileEquals(CRL_AS_XML, prr.getResult().asString());
	}

	@Test
	public void testCrlWrongIssuer() throws Exception {
		// arrange
		String issuerKey="issuer";
		pipe.setIssuerSessionKey(issuerKey);
		configureAndStartPipe();

		Message issuer_cert = Message.asMessage(TestFileUtils.getTestFileURL(CRL_ISSUER_CERT_FILE_WRONG).openStream());
		Message crl = Message.asMessage(TestFileUtils.getTestFileURL(CRL_FILE).openStream());
		session.put(issuerKey, issuer_cert);

		// act
		PipeRunResult prr = doPipe(crl);

		// assert
		assertEquals("success", prr.getPipeForward().getName()); 	// CrlPipe could return exception forward here
		assertNull(prr.getResult().asString());						// CrlPipe could return error message here
	}


}
