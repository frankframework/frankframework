/*
   Copyright 2018-2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.senders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.pipes.XmlValidator;

public abstract class SenderTestBase<S extends ISender> extends Mockito {

	protected Log log = LogFactory.getLog(this.getClass());
	protected S sender;

	@Mock
	protected IPipeLineSession session;

	public abstract S createSender() throws Exception;

	@Before
	public void setUp() throws Exception {
		session = new PipeLineSessionBase();
		String messageId = "testmessageac13ecb1--30fe9225_16caa708707_-7fb1";
		String technicalCorrelationId = "testmessageac13ecb1--30fe9225_16caa708707_-7fb2";
		session.put(IPipeLineSession.messageIdKey, messageId);
		session.put(IPipeLineSession.technicalCorrelationIdKey, technicalCorrelationId);
		sender = createSender();
	}

	@After
	public void tearDown() throws SenderException {
		if (sender != null) {
			sender.close();
			sender = null;
		}
	}

	protected String readLines(Reader reader) throws IOException {
		BufferedReader buf = new BufferedReader(reader);
		StringBuilder string = new StringBuilder();
		String line = buf.readLine();
		while (line != null) {
			string.append(line);
			line = buf.readLine();
			if (line != null) {
				string.append("\n");
			}
		}
		return string.toString();
	}

	protected String getFile(String file) throws IOException {
		return readLines(new InputStreamReader(XmlValidator.class.getResourceAsStream(file)));
	}
}
