/*
   Copyright 2018, 2020 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class HashPipeTest extends PipeTestBase<HashPipe> {

	@Override
	public HashPipe createPipe() {
		return new HashPipe();
	}
	
	@Test
	public void wrongAlgorithm() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		exception.expect(ConfigurationException.class);
		exception.expectMessage("illegal value for algorithm [dummy], must be one of " + pipe.algorithms.toString());

		pipe.setSecret("Potato");
		pipe.setAlgorithm("dummy");
		pipe.configure();
	}

	@Test
	public void wrongBinaryToTextEncoding() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		exception.expect(ConfigurationException.class);
		exception.expectMessage("illegal value for binary to text method [dummy], must be one of " + pipe.binaryToTextEncodings.toString());

		pipe.setSecret("Potato");
		pipe.setBinaryToTextEncoding("dummy");
		pipe.configure();
	}
	
	@Test
	public void noSecret() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		exception.expect(PipeRunException.class);

		pipe.configure();
		pipe.start();
		doPipe(pipe, "I will fail!", session);
	}

	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash=prr.getResult().asString();
		assertEquals("KZAvcWh5wSTeoBWty9MHZl+L4ApUjbWnJNaVq6xftAo=", hash);
	}

	@Test
	public void md5() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.setAlgorithm("HmacMD5");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash=prr.getResult().asString();
		assertEquals("TwGD5U8BwKoLn8u/F+4R/g==", hash);
	}

	@Test
	public void sha512() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.setAlgorithm("HmacSHA512");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash=prr.getResult().asString();
		assertEquals("56V9GhAPU9NPP76zJ5KVLrfMaCherC8JcY16PTPEO3W+yxNnoXwmLS+Ic61J3gqZyeUfc0VZzzgg23WqesXm2g==", hash);
	}
	
	@Test
	public void hex() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.setBinaryToTextEncoding("Hex");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash=prr.getResult().asString();
		assertEquals("29902f716879c124dea015adcbd307665f8be00a548db5a724d695abac5fb40a", hash);
	}
	
	@Test
	public void md5hex() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.setBinaryToTextEncoding("Hex");
		pipe.setAlgorithm("HmacMD5");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash=prr.getResult().asString();
		assertEquals("4f0183e54f01c0aa0b9fcbbf17ee11fe", hash);
	}

	@Test
	public void sha512hex() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.setBinaryToTextEncoding("Hex");
		pipe.setAlgorithm("HmacSHA512");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash=prr.getResult().asString();
		assertEquals("e7a57d1a100f53d34f3fbeb32792952eb7cc68285eac2f09718d7a3d33c43b75becb1367a17c262d2f8873ad49de0a99c9e51f734559cf3820db75aa7ac5e6da", hash);
	}
	
	@Test
	public void largeMessage() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.configure();
		pipe.start();
		
		PipeRunResult prr = doPipe(pipe, TestFileUtils.getTestFile("/HashPipe/largeInput.txt"), session);
		String hash=prr.getResult().asString();
		assertEquals("M7Z60BhL72SMyCEUVesQOuvBRUcokJPyy95lSQODDZU=", hash);
	}
	
	@Test
	public void largeMessageHex() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.setBinaryToTextEncoding("Hex");
		pipe.configure();
		pipe.start();
		
		PipeRunResult prr = doPipe(pipe, TestFileUtils.getTestFile("/HashPipe/largeInput.txt"), session);
		String hash=prr.getResult().asString();
		assertEquals("33b67ad0184bef648cc8211455eb103aebc14547289093f2cbde654903830d95", hash);
	}
}
