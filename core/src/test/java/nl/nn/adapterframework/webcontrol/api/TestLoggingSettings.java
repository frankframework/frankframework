/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Test;

import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class TestLoggingSettings extends ApiTestBase<ShowLogging> {

	@Override
	public ShowLogging createJaxRsResource() {
		return new ShowLogging();
	}

	@Test
	public void updateLogIntermediaryResults() {
		boolean logIntermediary = AppConstants.getInstance().getBoolean("log.logIntermediaryResults", true);

		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/server/logging", "{\"logIntermediaryResults\":\""+!logIntermediary+"\"}"); //Set it to false when true, or to true when false, either way, change the value!
		assertEquals(204, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

		Boolean logIntermediaryResult = Boolean.valueOf(AppConstants.getInstance().getResolvedProperty("log.logIntermediaryResults"));
		assertNotNull(logIntermediaryResult);
		assertEquals(!logIntermediary, logIntermediaryResult);

		AppConstants.getInstance().setProperty("log.logIntermediaryResults", true);
	}

	@Test
	public void updateMaxLength() {
		assertEquals("maxLength should default to -1 (off)", -1, IbisMaskingLayout.getMaxLength());

		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/server/logging", "{\"maxMessageLength\":\"50\"}");
		assertEquals(204, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

		assertEquals("maxLength should have been updated to 50", 50, IbisMaskingLayout.getMaxLength());

		IbisMaskingLayout.setMaxLength(-1); //Reset max length
	}

	@Test
	public void updateTesttoolEnabled() {
		boolean testtoolEnabled = AppConstants.getInstance().getBoolean("testtool.enabled", true);
		assertTrue(testtoolEnabled);

		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/server/logging", "{\"enableDebugger\":\"false\"}");
		assertEquals(204, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

		Boolean testtoolEnabledResult = Boolean.valueOf(AppConstants.getInstance().getResolvedProperty("testtool.enabled"));
		assertNotNull(testtoolEnabledResult);
		assertEquals(!testtoolEnabled, testtoolEnabledResult);
	}

	@Test
	public void updateLogLevel() {
		Logger rootLogger = LogUtil.getRootLogger();
		Level initialLevel = rootLogger.getLevel();
		Level otherLevel = initialLevel == Level.INFO ? Level.ERROR : Level.INFO;

		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/server/logging", "{\"loglevel\":\""+otherLevel+"\"}");
		assertEquals(204, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

		assertEquals("loglevel should have been changed to "+otherLevel, otherLevel, rootLogger.getLevel());

		Configurator.setLevel(rootLogger.getName(), initialLevel); //Reset log level
	}
}
