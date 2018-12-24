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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

public class HeapSizePipeTest extends PipeTestBase<HeapSizePipe> {

	@Mock
	private IPipeLineSession session;
	
	@Override
	public HeapSizePipe createPipe() {
		return new HeapSizePipe();
	}

	@Test
	public void MemoryTest() throws PipeRunException, ConfigurationException {
		Object dummyObject = "dummy";
		pipe.configure();

		PipeRunResult prr = pipe.doPipe(dummyObject, session);
		String memoryMetrics = (String) prr.getResult();
		System.out.println(memoryMetrics);
		
		assertNotNull(memoryMetrics);
		
		assertEquals(true, memoryMetrics.contains("<processMetrics>") && memoryMetrics.contains("</processMetrics>"));
		assertEquals(true, memoryMetrics.contains("<properties>") && memoryMetrics.contains("</properties>"));
		assertEquals(true, memoryMetrics.contains("<property name=\"freeMemory\">"));
		assertEquals(true, memoryMetrics.contains("<property name=\"totalMemory\">"));
		assertEquals(true, memoryMetrics.contains("<property name=\"heapSize\">"));
		assertEquals(true, memoryMetrics.contains("<property name=\"currentTime\">"));
	}

}
