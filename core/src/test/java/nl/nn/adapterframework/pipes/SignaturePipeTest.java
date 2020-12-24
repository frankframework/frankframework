package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;

public class SignaturePipeTest extends PipeTestBase<SignaturePipe> {

	private String testMessage = "xyz";
	private String testSignature = "JBKjNltZoFlQTsBgstpnIB4itBxzAohRXGpIWuIQh51F64P4WdT+R/55v+cHrPsQ2B49GhROeFUyy7kafOKTfMTjm7DQ5yT/srImFTlZZZbHbvQns2NWBE8DoQKt6SOYowDNIJY5qDV+82k6xY2BcTcZoiAPB53F3rEkfzz/QkxcFiCKvtg2voG1WyVkyoue10404UXIkSXv0ySYnRBRugdPO1DKyUwL6FS5tP2p8toBVzeRT6rMkEwuU3A5riQpdnEOi0ckeFvSNU3Cdgdah4HWd+48gXzBE6Uwu/BMOrD/5mRUnS0wmPn7dajkjHNC2r9+C1jxlFy3NIim1rS2iA==";


	@Override
	public SignaturePipe createPipe() {
		return new SignaturePipe();
	}

	@Test
	public void testSign() throws Exception {
		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(testMessage));
		
		assertEquals(testSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void testSignPem() throws Exception {
		pipe.setKeystore("/Signature/privateKey.key");
		pipe.setKeystoreType("pem");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(testMessage));
		
		assertEquals(testSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void testVerifyOK() throws Exception {
		pipe.setAction("verify");
		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");
		
		Parameter param = new Parameter();
		param.setName("signature");
		param.setValue(testSignature);
		pipe.addParameter(param);
		
		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.registerForward(failure);
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(testMessage));

		assertEquals(testMessage, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void testVerifyNotOK() throws Exception {
		pipe.setAction("verify");
		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");
		
		Parameter param = new Parameter();
		param.setName("signature");
		param.setValue(testSignature);
		pipe.addParameter(param);
		
		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.registerForward(failure);
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message("otherMessage"));

		assertEquals("failure", prr.getPipeForward().getName());
	}

	@Test
	public void testVerifyOKPEM() throws Exception {
		pipe.setAction("verify");
		pipe.setKeystore("/Signature/certificate.crt");
		pipe.setKeystoreType("pem");
		
		Parameter param = new Parameter();
		param.setName("signature");
		param.setValue(testSignature);
		pipe.addParameter(param);
		
		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.registerForward(failure);
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(testMessage));

		assertEquals(testMessage, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

}
