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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISecurityHandler;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;

class GetPrincipalPipeTest extends PipeTestBase<GetPrincipalPipe> {
	private final String PRINCIPAL_NAME = "TST9";
	private final String NOT_FOUND_FORWARD_NAME = "notFound";
	private final String NOT_FOUND_FORWARD_PATH = "doSomething";
	private ISecurityHandler securityHandler;
	private Principal principal = mock(Principal.class);

	@BeforeEach
	public void populateSession() {
		principal = mock(Principal.class);
		securityHandler = mock(ISecurityHandler.class);
		session.setSecurityHandler(securityHandler);
	}

	@Override
	public GetPrincipalPipe createPipe() throws ConfigurationException {
		return new GetPrincipalPipe();
	}

	@Test
	void missingNotFoundForwardConfigure() {
		// Given
		pipe.setNotFoundForwardName(NOT_FOUND_FORWARD_NAME);

		// Expect/When
		assertThrows(ConfigurationException.class, () -> pipe.configure(), "notInRoleForwardName [notFound] not found");
	}

	@Test
	void notFoundForwardConfigure() {
		// Given
		PipeForward notFound = new PipeForward(NOT_FOUND_FORWARD_NAME, NOT_FOUND_FORWARD_PATH);
		pipe.setNotFoundForwardName(NOT_FOUND_FORWARD_NAME);
		pipe.addForward(notFound);

		// Expect/When
		assertDoesNotThrow(() -> pipe.configure());
	}

	@Test
	void getPrincipalDoPipe() throws Exception {
		// Given
		when(principal.getName()).thenReturn(PRINCIPAL_NAME);
		when(securityHandler.getPrincipal()).thenReturn(principal);

		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result = prr.getResult().asString();
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals(PRINCIPAL_NAME, result);
	}

	@Test
	void getNullPrincipalNameDoPipe() throws Exception {
		// Given
		when(principal.getName()).thenReturn(null);
		when(securityHandler.getPrincipal()).thenReturn(principal);

		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result = prr.getResult().asString();
		assertEquals("success", prr.getPipeForward().getName());
		assertNull(result);
	}

	@Test
	void getEmptyStringPrincipalNameDoPipe() throws Exception {
		// Given
		when(principal.getName()).thenReturn("");
		when(securityHandler.getPrincipal()).thenReturn(principal);

		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result = prr.getResult().asString();
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("", result);
	}

	@Test
	void getNullPrincipalDoPipe() throws Exception {
		// Given
		when(securityHandler.getPrincipal()).thenReturn(null);

		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result = prr.getResult().asString();
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("", result);
	}

	@Test
	void getNullPrincipalWithNotFoundForwardDoPipe() throws Exception {
		// Given
		when(securityHandler.getPrincipal()).thenReturn(null);

		PipeForward notFound = new PipeForward(NOT_FOUND_FORWARD_NAME, NOT_FOUND_FORWARD_PATH);
		pipe.setNotFoundForwardName(NOT_FOUND_FORWARD_NAME);
		pipe.addForward(notFound);
		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result = prr.getResult().asString();
		assertEquals(NOT_FOUND_FORWARD_NAME, prr.getPipeForward().getName());
		assertEquals(NOT_FOUND_FORWARD_PATH, prr.getPipeForward().getPath());
		assertEquals("", result);
	}

	@Test
	void getNullPrincipalNameWithNotFoundForwardDoPipe() throws Exception {
		// Given
		when(principal.getName()).thenReturn(null);
		when(securityHandler.getPrincipal()).thenReturn(principal);

		PipeForward notFound = new PipeForward(NOT_FOUND_FORWARD_NAME, NOT_FOUND_FORWARD_PATH);
		pipe.setNotFoundForwardName(NOT_FOUND_FORWARD_NAME);
		pipe.addForward(notFound);
		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result = prr.getResult().asString();
		assertEquals(NOT_FOUND_FORWARD_NAME, prr.getPipeForward().getName());
		assertEquals(NOT_FOUND_FORWARD_PATH, prr.getPipeForward().getPath());
		assertNull(result);
	}

	@Test
	void getEmptyStringPrincipalWithNotFoundForwardNameDoPipe() throws Exception {
		// Given
		when(principal.getName()).thenReturn("");
		when(securityHandler.getPrincipal()).thenReturn(principal);

		PipeForward notFound = new PipeForward(NOT_FOUND_FORWARD_NAME, NOT_FOUND_FORWARD_PATH);
		pipe.setNotFoundForwardName(NOT_FOUND_FORWARD_NAME);
		pipe.addForward(notFound);
		pipe.configure();
		pipe.start();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		String result = prr.getResult().asString();
		assertEquals(NOT_FOUND_FORWARD_NAME, prr.getPipeForward().getName());
		assertEquals(NOT_FOUND_FORWARD_PATH, prr.getPipeForward().getPath());
		assertEquals("", result);
	}

	@Test
	void getPrincipalNameThrowsException() throws Exception {
		// Given
		when(principal.getName()).thenThrow(new NotImplementedException());
		when(securityHandler.getPrincipal()).thenReturn(principal);

		pipe.configure();
		pipe.start();

		// Expect/When
		assertThrows(PipeRunException.class, () -> doPipe(pipe, "", session),
				"Pipe [" + pipe.getName() + "] got exception getting name from principal: (NotImplementedException)");
	}
}
