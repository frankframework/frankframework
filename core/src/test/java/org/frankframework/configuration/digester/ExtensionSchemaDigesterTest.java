/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.configuration.digester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import org.frankframework.components.ExtensionSchema;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.util.ConfigurationUtils;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.Resource;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.PropertyLoader;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.XmlWriter;

/**
 * A module-contributed extension schema is composed into configuration validation, so its
 * custom-namespace elements are validated instead of stripped as unknown content (issue #10490).
 */
public class ExtensionSchemaDigesterTest {

	private static final String BASE_XSD = "/Digester/extensionSchema/base.xsd";
	private static final String EXTENSION_NS = "urn:test:vf-ext";
	private static final String EXTENSION_XSD = "/Digester/extensionSchema/extension.xsd";

	private static ConfigurationDigester digesterWithExtension() {
		return new ConfigurationDigester() {
			@Override
			protected List<ExtensionSchema> collectExtensionSchemas() {
				return List.of(new ExtensionSchema(EXTENSION_NS, EXTENSION_XSD));
			}
		};
	}

	private static class RecordingErrorHandler implements ErrorHandler {
		final List<SAXParseException> errors = new ArrayList<>();

		@Override
		public void warning(SAXParseException e) {
			errors.add(e);
		}

		@Override
		public void error(SAXParseException e) {
			errors.add(e);
		}

		@Override
		public void fatalError(SAXParseException e) {
			errors.add(e);
		}
	}

	@Test
	public void registeredExtensionElementIsKeptAndValidates() throws Exception {
		RecordingErrorHandler errorHandler = new RecordingErrorHandler();
		XmlWriter writer = new XmlWriter();
		ContentHandler handler = digesterWithExtension().getConfigurationCanonicalizer(writer, BASE_XSD, errorHandler);

		String config = "<Configuration><vf:CustomPipe xmlns:vf=\"" + EXTENSION_NS + "\" name=\"demo\"/></Configuration>";
		XmlUtils.parseXml(config, handler);

		assertTrue(errorHandler.errors.isEmpty(), () -> "expected no validation errors, got: " + errorHandler.errors);
		// Retained (not stripped): the element's namespace survives into the canonicalized output.
		assertTrue(writer.toString().contains(EXTENSION_NS), () -> "extension element was stripped: " + writer);
	}

	@Test
	public void registeredExtensionElementIsActuallyValidatedAgainstItsSchema() throws Exception {
		RecordingErrorHandler errorHandler = new RecordingErrorHandler();
		XmlWriter writer = new XmlWriter();
		ContentHandler handler = digesterWithExtension().getConfigurationCanonicalizer(writer, BASE_XSD, errorHandler);

		// Missing the required @name: the composed extension schema must flag it (a stripped element
		// would raise no error at all).
		String config = "<Configuration><vf:CustomPipe xmlns:vf=\"" + EXTENSION_NS + "\"/></Configuration>";
		XmlUtils.parseXml(config, handler);

		assertFalse(errorHandler.errors.isEmpty(), "expected a validation error for the missing required attribute");
	}

	@Test
	public void unregisteredNamespaceIsStillStripped() throws Exception {
		RecordingErrorHandler errorHandler = new RecordingErrorHandler();
		XmlWriter writer = new XmlWriter();
		ContentHandler handler = new ConfigurationDigester().getConfigurationCanonicalizer(writer, BASE_XSD, errorHandler);

		String config = "<Configuration><other:Thing xmlns:other=\"urn:test:unregistered\"/></Configuration>";
		XmlUtils.parseXml(config, handler);

		assertTrue(errorHandler.errors.isEmpty(), () -> "unregistered namespaced content should be stripped, not errored: " + errorHandler.errors);
		assertFalse(writer.toString().contains("Thing"), () -> "unregistered namespaced element should have been stripped: " + writer);
	}

	@Test
	public void extensionElementReachesTheDigesterAsAPipeWithItsClassName() throws Exception {
		RecordingErrorHandler errorHandler = new RecordingErrorHandler();
		XmlWriter writer = new XmlWriter();
		// writer is the terminal handler: it captures exactly what the digester is fed.
		ContentHandler handler = digesterWithExtension().getConfigurationCanonicalizer(writer, BASE_XSD, errorHandler);

		String config = "<Configuration><vf:CustomPipe xmlns:vf=\"" + EXTENSION_NS + "\" name=\"demo\"/></Configuration>";
		XmlUtils.parseXml(config, handler);

		String canonical = writer.toString();
		assertTrue(errorHandler.errors.isEmpty(), () -> "expected no validation errors, got: " + errorHandler.errors);
		// The schema's default elementRole/className were injected by validation: the element now
		// carries the class, so GenericFactory instantiates it exactly like any built-in pipe.
		assertTrue(canonical.contains(CustomExtensionPipe.class.getName()),
				() -> "className was not injected onto the extension element: " + canonical);
	}

	@Test
	public void extensionElementIsInstantiatedByTheDigester() throws Exception {
		ConfigurationDigester digester = new ConfigurationDigester() {
			@Override
			protected List<ExtensionSchema> collectExtensionSchemas() {
				return List.of(new ExtensionSchema(EXTENSION_NS, EXTENSION_XSD));
			}
		};
		digester.setConfigurationWarnings(new ConfigurationWarnings());

		Configuration configuration = new TestConfiguration();
		Resource resource = Resource.getResource("/Digester/extensionSchema/CustomPipeConfiguration.xml");
		digester.digest(configuration, resource, new PropertyLoader("Digester/ConfigurationDigesterTest.properties"));

		Adapter adapter = configuration.getRegisteredAdapter("ExtAdapter");
		assertNotNull(adapter, "adapter was not digested");
		// The namespaced <CustomPipe> resolved to the pipe role + its className and instantiated the
		// class -- the digester matches its rules on localName, so the namespace does not get in the way.
		assertInstanceOf(CustomExtensionPipe.class, adapter.getPipeLine().getPipe("myCustomPipe"));
	}

	// --- Module attributes on built-in elements (option 2 for issue #10490) ---

	private static final String ATTRIBUTE_CONFIG = "<Configuration><Adapter name=\"a\"><Pipeline>%s</Pipeline></Adapter></Configuration>";

	/** Canonicalizes against the real compatibility schema; returns the validation messages. */
	private static List<String> canonicalize(ConfigurationDigester digester, String pipe, XmlWriter writer) throws Exception {
		RecordingErrorHandler errorHandler = new RecordingErrorHandler();
		ContentHandler handler = digester.getConfigurationCanonicalizer(writer, ConfigurationUtils.FRANK_CONFIG_XSD, errorHandler);
		XmlUtils.parseXml(ATTRIBUTE_CONFIG.formatted(pipe), handler);
		return errorHandler.errors.stream().map(SAXParseException::getMessage).toList();
	}

	@Test
	public void registeredNamespacedAttributeOnBuiltInElementValidatesAndArrivesUnqualified() throws Exception {
		List<String> baseline = canonicalize(digesterWithExtension(), "<EchoPipe name=\"echo\"/>", new XmlWriter());

		XmlWriter writer = new XmlWriter();
		List<String> errors = canonicalize(digesterWithExtension(),
				"<EchoPipe xmlns:acme=\"" + EXTENSION_NS + "\" name=\"echo\" acme:auditLevel=\"full\"/>", writer);

		// The generated EchoPipe type accepts ##other attributes, so the module attribute adds no warning.
		assertEquals(baseline, errors, "the namespaced module attribute must not add validation messages");
		String canonical = writer.toString();
		assertTrue(canonical.contains("auditLevel=\"full\""), () -> "module attribute was not passed on as a plain attribute: " + canonical);
		assertFalse(canonical.contains(EXTENSION_NS), () -> "the namespace should be a validation-time marker only: " + canonical);
	}

	@Test
	public void unqualifiedCustomAttributeOnBuiltInElementIsStillReported() throws Exception {
		// The situation this feature addresses: without the namespace the schema rejects the attribute.
		List<String> baseline = canonicalize(digesterWithExtension(), "<EchoPipe name=\"echo\"/>", new XmlWriter());
		List<String> errors = canonicalize(digesterWithExtension(), "<EchoPipe name=\"echo\" auditLevel=\"full\"/>", new XmlWriter());

		assertTrue(errors.size() > baseline.size(), () -> "expected a validation message for the undeclared attribute, got: " + errors);
		assertTrue(errors.stream().anyMatch(m -> m.contains("auditLevel")), () -> "expected the message to name the attribute: " + errors);
	}

	@Test
	public void unregisteredNamespacedAttributeIsStillStripped() throws Exception {
		List<String> baseline = canonicalize(digesterWithExtension(), "<EchoPipe name=\"echo\"/>", new XmlWriter());

		XmlWriter writer = new XmlWriter();
		List<String> errors = canonicalize(digesterWithExtension(),
				"<EchoPipe xmlns:tool=\"urn:test:unregistered\" name=\"echo\" tool:auditLevel=\"full\"/>", writer);

		assertEquals(baseline, errors);
		assertFalse(writer.toString().contains("auditLevel"), () -> "an unregistered namespaced attribute should be stripped: " + writer);
	}

	@Test
	public void unqualifiedAttributeWinsOverNamespacedDuplicateAndTheConflictIsReported() throws Exception {
		XmlWriter writer = new XmlWriter();
		List<String> errors = canonicalize(digesterWithExtension(),
				"<EchoPipe xmlns:acme=\"" + EXTENSION_NS + "\" name=\"echo\" auditLevel=\"unqualified\" acme:auditLevel=\"namespaced\"/>", writer);

		assertTrue(errors.stream().anyMatch(m -> m.contains("acme:auditLevel") && m.contains("is ignored")), () -> "expected the conflict to be reported, got: " + errors);
		String canonical = writer.toString();
		assertTrue(canonical.contains("auditLevel=\"unqualified\""), () -> "the unqualified attribute should win: " + canonical);
		assertFalse(canonical.contains("namespaced"), () -> "the namespaced duplicate should be dropped: " + canonical);
	}

	@Test
	public void namespacedAttributeIsSetOnTheCustomizedBuiltInPipe() throws Exception {
		ConfigurationDigester digester = digesterWithExtension();
		ConfigurationWarnings validationWarnings = new ConfigurationWarnings();
		digester.setConfigurationWarnings(validationWarnings);

		Configuration configuration = new TestConfiguration();
		Resource resource = Resource.getResource("/Digester/extensionSchema/ExtensionAttributeConfiguration.xml");
		digester.digest(configuration, resource, new PropertyLoader("Digester/ConfigurationDigesterTest.properties"));

		Adapter adapter = configuration.getRegisteredAdapter("ExtAttrAdapter");
		assertNotNull(adapter, "adapter was not digested");
		AuditingEchoPipe pipe = assertInstanceOf(AuditingEchoPipe.class, adapter.getPipeLine().getPipe("echo"));
		assertEquals("full", pipe.getAuditLevel(), "the digester did not set the module attribute");

		// Neither the digester's attribute check nor schema validation complains about the module attribute.
		List<String> warnings = new ArrayList<>(configuration.getConfigurationWarnings().getWarnings());
		warnings.addAll(validationWarnings.getWarnings());
		assertTrue(warnings.stream().noneMatch(w -> w.contains("auditLevel")), () -> "unexpected warning about the module attribute: " + warnings);
	}

	/** Stand-in for a module's customized built-in pipe: EchoPipe plus one module-specific setter. */
	public static class AuditingEchoPipe extends EchoPipe {
		private String auditLevel;

		public void setAuditLevel(String auditLevel) {
			this.auditLevel = auditLevel;
		}

		public String getAuditLevel() {
			return auditLevel;
		}
	}

	/** Stand-in for a module's custom pipe; referenced as the className default in extension.xsd. */
	public static class CustomExtensionPipe extends FixedForwardPipe {
		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) {
			return new PipeRunResult(getSuccessForward(), message);
		}
	}
}
