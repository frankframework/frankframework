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

import org.apache.commons.lang3.NotImplementedException;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
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

		assertEquals("notInRoleForwardName [notFound] not found", exception.getMessage());
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
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals(PRINCIPAL_NAME, result);
	}

	@Test
	public void getNullPrincipalNameDoPipe() throws Exception {
		// Given
		Principal principal = mock(Principal.class);
		when(principal.getName()).thenReturn(null);
		when(session.getPrincipal()).thenReturn(principal);

		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result=prr.getResult().asString();
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals(null, result);
	}

	@Test
	public void getEmptyStringPrincipalNameDoPipe() throws Exception {
		// Given
		Principal principal = mock(Principal.class);
		when(principal.getName()).thenReturn("");
		when(session.getPrincipal()).thenReturn(principal);

		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result=prr.getResult().asString();
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("", result);
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
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals( null, result);
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
		assertEquals(NOT_FOUND_FORWARD_NAME, prr.getPipeForward().getName());
		assertEquals(NOT_FOUND_FORWARD_PATH, prr.getPipeForward().getPath());
		assertEquals(null, result);
	}

	@Test
	public void getNullPrincipalNameWithNotFoundForwardDoPipe() throws Exception {
		// Given
		Principal principal = mock(Principal.class);
		when(principal.getName()).thenReturn(null);
		when(session.getPrincipal()).thenReturn(principal);

		PipeForward notFound = new PipeForward(NOT_FOUND_FORWARD_NAME, NOT_FOUND_FORWARD_PATH);
		pipe.setNotFoundForwardName(NOT_FOUND_FORWARD_NAME);
		pipe.registerForward(notFound);
		pipe.configure();
		pipe.start();


		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result=prr.getResult().asString();
		assertEquals(NOT_FOUND_FORWARD_NAME, prr.getPipeForward().getName());
		assertEquals(NOT_FOUND_FORWARD_PATH, prr.getPipeForward().getPath());
		assertEquals(null, result);
	}

	@Test
	public void getEmptyStringPrincipalWithNotFoundForwardNameDoPipe() throws Exception {
		// Given
		Principal principal = mock(Principal.class);
		when(principal.getName()).thenReturn("");
		when(session.getPrincipal()).thenReturn(principal);

		PipeForward notFound = new PipeForward(NOT_FOUND_FORWARD_NAME, NOT_FOUND_FORWARD_PATH);
		pipe.setNotFoundForwardName(NOT_FOUND_FORWARD_NAME);
		pipe.registerForward(notFound);
		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result=prr.getResult().asString();
		assertEquals(NOT_FOUND_FORWARD_NAME, prr.getPipeForward().getName());
		assertEquals(NOT_FOUND_FORWARD_PATH, prr.getPipeForward().getPath());
		assertEquals("", result);
	}

	@Test
	public void getPrincipalNameThrowsException() throws Exception {
		// Given
		Principal principal = mock(Principal.class);
		when(principal.getName()).thenThrow(new NotImplementedException());
		when(session.getPrincipal()).thenReturn(principal);

		pipe.configure();
		pipe.start();

		// Expect
		PipeRunException exception = assertThrows(PipeRunException.class, () -> {
			doPipe(pipe, "", session);
		});

		assertEquals("Pipe ["+pipe.getName()+"] got exception getting name from principal: ((NotImplementedException)", exception.getMessage());
	}
}
