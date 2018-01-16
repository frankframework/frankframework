/*
   Copyright 2018 Nationale-Nederlanden

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
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;

public class HashPipeTest extends PipeTestBase<HashPipe> {

	@Mock
	private IPipeLineSession session;

	@Override
	public HashPipe createPipe() {
		return new HashPipe();
	}

	@Test
	public void wrongAlgorithm() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		exception.expect(ConfigurationException.class);

		pipe.setSecret("Potato");
		pipe.setAlgorithm("dummy");
		pipe.configure();
	}

	@Test
	public void noSecret() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		exception.expect(PipeRunException.class);

		pipe.configure();
		pipe.start();
		pipe.doPipe("I will fail!", session);
	}

	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = pipe.doPipe("hash me plz", session);
		String hash = (String) prr.getResult();
		assertEquals("KZAvcWh5wSTeoBWty9MHZl+L4ApUjbWnJNaVq6xftAo=", hash);
	}

	@Test
	public void md5() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.setAlgorithm("HmacMD5");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = pipe.doPipe("hash me plz", session);
		String hash = (String) prr.getResult();
		assertEquals("TwGD5U8BwKoLn8u/F+4R/g==", hash);
	}

	@Test
	public void sha512() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setSecret("Potato");
		pipe.setAlgorithm("HmacSHA512");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = pipe.doPipe("hash me plz", session);
		String hash = (String) prr.getResult();
		assertEquals("56V9GhAPU9NPP76zJ5KVLrfMaCherC8JcY16PTPEO3W+yxNnoXwmLS+Ic61J3gqZyeUfc0VZzzgg23WqesXm2g==", hash);
	}
}
