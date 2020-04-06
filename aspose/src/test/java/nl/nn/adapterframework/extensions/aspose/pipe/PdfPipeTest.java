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
package nl.nn.adapterframework.extensions.aspose.pipe;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.util.Misc;

import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.extensions.aspose.pipe.PdfPipe;

public class PdfPipeTest extends PipeTestBase<PdfPipe> {

	@Mock
	private IPipeLineSession session;

	@Override
	public PdfPipe createPipe() {
		return new PdfPipe();
	}

	@Test(expected = ConfigurationException.class)
	public void noPdfOutputLocation() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setPdfOutputLocation("");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void wrongPdfOutputLocation() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setPdfOutputLocation("not a valid location");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void noAction() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setAction("");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void wrongAction() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setAction("test123");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void noLicense() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setLicense("");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void wrongLicense() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setLicense("test123");
		pipe.configure();
	}

}