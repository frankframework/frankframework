/*
   Copyright 2024 WeAreFrank

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
package org.frankframework.pipes;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetPrincipalPipeTest extends PipeTestBase<GetPrincipalPipe> {
	private PipeLineSession session;
	private final String PRINCIPAL_NAME = "TST9";
	private final String NOT_FOUND_FORWARD_NAME = "notFound";
	private final String NOT_FOUND_FORWARD_PATH = "doSomething";

	@BeforeEach
	public void populateSession() {
		session = mock(PipeLineSession.class);
	}

	@Override
	public GetPrincipalPipe createPipe() throws ConfigurationException {
		return new GetPrincipalPipe();
	}

	@Test
	public void missingNotFoundForwardConfigure(){
		// Given
		pipe.setNotFoundForwardName(NOT_FOUND_FORWARD_NAME);

		// Expect
		ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
			// When
			pipe.configure();
		});

		assertEquals(exception.getMessage(), "notInRoleForwardName [notFound] not found");
	}

	@Test
	public void notFoundForwardConfigure() throws ConfigurationException {
		// Given
		PipeForward notFound = new PipeForward(NOT_FOUND_FORWARD_NAME, NOT_FOUND_FORWARD_PATH);
		pipe.setNotFoundForwardName(NOT_FOUND_FORWARD_NAME);
		pipe.registerForward(notFound);

		// Expect
		assertDoesNotThrow(() -> {
			// When
			pipe.configure();
		});
	}

	@Test
	public void getPrincipalDoPipe() throws Exception {
		// Given
		Principal principal = mock(Principal.class);
		when(principal.getName()).thenReturn(PRINCIPAL_NAME);
		when(session.getPrincipal()).thenReturn(principal);

		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result=prr.getResult().asString();
		assertEquals(PRINCIPAL_NAME, result);
	}

	@Test
	public void getNullPrincipalDoPipe() throws Exception {
		// Given
		when(session.getPrincipal()).thenReturn(null);

		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result=prr.getResult().asString();
		assertEquals(result, "");
	}

	@Test
	public void getNullPrincipalWithNotFoundForwardDoPipe() throws Exception {
		// Given
		when(session.getPrincipal()).thenReturn(null);

		PipeForward notFound = new PipeForward(NOT_FOUND_FORWARD_NAME, NOT_FOUND_FORWARD_PATH);
		pipe.setNotFoundForwardName(NOT_FOUND_FORWARD_NAME);
		pipe.registerForward(notFound);
		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result=prr.getResult().asString();
		assertEquals(prr.getPipeForward().getName(), NOT_FOUND_FORWARD_NAME);
		assertEquals(prr.getPipeForward().getPath(), NOT_FOUND_FORWARD_PATH);
		assertEquals(result, "");
	}
}
