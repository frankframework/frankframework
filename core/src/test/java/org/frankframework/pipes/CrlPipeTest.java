package org.frankframework.pipes;

import static org.frankframework.testutil.MatchUtils.assertTestFileEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;

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

		Message issuer_cert = new Message(TestFileUtils.getTestFileURL(CRL_ISSUER_CERT_FILE).openStream());
		Message crl = new Message(TestFileUtils.getTestFileURL(CRL_FILE).openStream());
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
		Message crl = new Message(TestFileUtils.getTestFileURL(CRL_FILE).openStream());
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

		Message issuer_cert = new Message(TestFileUtils.getTestFileURL(CRL_ISSUER_CERT_FILE_WRONG).openStream());
		Message crl = new Message(TestFileUtils.getTestFileURL(CRL_FILE).openStream());
		session.put(issuerKey, issuer_cert);

		// act
		PipeRunResult prr = doPipe(crl);

		// assert
		assertEquals("success", prr.getPipeForward().getName()); 	// CrlPipe could return exception forward here
		assertNull(prr.getResult().asString());						// CrlPipe could return error message here
	}


}
