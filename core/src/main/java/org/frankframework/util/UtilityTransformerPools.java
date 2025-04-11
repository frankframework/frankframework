/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.util;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.frankframework.configuration.ConfigurationException;

public class UtilityTransformerPools {

	private static String makeDetectXsltVersionXslt() {
		return
		"""
		<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">\
		<xsl:output method="text"/>\
		<xsl:template match="/">\
		<xsl:value-of select="xsl:stylesheet/@version"/>\
		</xsl:template>\
		</xsl:stylesheet>\
		""";
	}

	public static TransformerPool getDetectXsltVersionTransformerPool() throws TransformerException {
		try {
			return XmlUtils.getUtilityTransformerPool(UtilityTransformerPools::makeDetectXsltVersionXslt,"DetectXsltVersion",true,false,2);
		} catch (ConfigurationException e) {
			throw new TransformerException(e);
		}
	}

	private static String makeGetXsltConfigXslt() {
		return
		"""
		<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">\
		<xsl:output method="text"/>\
		<xsl:template match="/">\
		<xsl:for-each select="/xsl:stylesheet/@*">\
		<xsl:value-of select="concat(name(),'=',.,';')"/>\
		</xsl:for-each>\
		<xsl:for-each select="/xsl:transform/@*">\
		<xsl:value-of select="concat(name(),'=',.,';')"/>\
		</xsl:for-each>\
		<xsl:for-each select="/xsl:stylesheet/xsl:output/@*">\
		<xsl:value-of select="concat('output-',name(),'=',.,';')"/>\
		</xsl:for-each>\
		disable-output-escaping=<xsl:choose>\
		<xsl:when test="//*[@disable-output-escaping='yes']">yes</xsl:when>\
		<xsl:otherwise>no</xsl:otherwise>\
		</xsl:choose>;\
		</xsl:template>\
		</xsl:stylesheet>\
		""";
	}

	public static TransformerPool getGetXsltConfigTransformerPool() throws TransformerException {
		try {
			return XmlUtils.getUtilityTransformerPool(UtilityTransformerPools::makeGetXsltConfigXslt,"detectXsltOutputType",true,false,2);
		} catch (ConfigurationException e) {
			throw new TransformerException(e);
		}
	}

	public static Map<String,String> getXsltConfig(Source source) throws TransformerException, IOException {
		TransformerPool tp = getGetXsltConfigTransformerPool();
		String metadataString = tp.transformToString(source);
		Map<String,String> result = new LinkedHashMap<>();
		for (final String s : StringUtil.split(metadataString, ";")) {
			List<String> kv = StringUtil.split(s, "=");
			String key = kv.get(0);
			String value = kv.get(1);
			result.put(key, value);
		}
		return result;
	}

	public static TransformerPool getGetRootNodeNameTransformerPool() throws ConfigurationException {
		return XmlUtils.getUtilityTransformerPool(()->XmlUtils.createXPathEvaluatorSource(XmlUtils.XPATH_GETROOTNODENAME),"GetRootNodeName",true, false, XmlUtils.DEFAULT_XSLT_VERSION);
	}

	private static String makeGetRootNamespaceXslt() {
		return
		"""
		<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">\
		<xsl:output method="text"/>\
		<xsl:template match="*">\
		<xsl:value-of select="namespace-uri()"/>\
		</xsl:template>\
		</xsl:stylesheet>\
		""";
	}

	public static TransformerPool getGetRootNamespaceTransformerPool() throws ConfigurationException {
		return XmlUtils.getUtilityTransformerPool(UtilityTransformerPools::makeGetRootNamespaceXslt,"GetRootNamespace",true,false, 2);
	}

	private static String makeAddRootNamespaceXslt(String namespace, boolean omitXmlDeclaration, boolean indent) {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" xmlns=\""+namespace+"\">"
			+ "<xsl:output method=\"xml\" indent=\""+(indent?"yes":"no")+"\" omit-xml-declaration=\""+(omitXmlDeclaration?"yes":"no")+"\"/>"
			+ "<xsl:template match=\"/*\">"
			+ "<xsl:element name=\"{local-name()}\">"
			+ "<xsl:apply-templates select=\"@* | comment() | node()\"/>"
			+ "</xsl:element>"
			+ "</xsl:template>"
			+ "<xsl:template match=\"*\">"
			+ "<xsl:choose>"
			+ "<xsl:when test=\"namespace-uri() = namespace-uri(/*)\">"
			+ "<xsl:element name=\"{local-name()}\">"
			+ "<xsl:apply-templates select=\"@* | comment() | node()\"/>"
			+ "</xsl:element>"
			+ "</xsl:when>"
			+ "<xsl:otherwise>"
			+ "<xsl:element namespace=\"{namespace-uri()}\" name=\"{local-name()}\">"
			+ "<xsl:apply-templates select=\"@* | comment() | node()\"/>"
			+ "</xsl:element>"
			+ "</xsl:otherwise>"
			+ "</xsl:choose>"
			+ "</xsl:template>"
			+ "<xsl:template match=\"@*\">"
			+ "<xsl:copy-of select=\".\"/>"
			+ "</xsl:template>"
			+ "<xsl:template match=\"comment()\">"
			+ "<xsl:copy-of select=\".\"/>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static TransformerPool getAddRootNamespaceTransformerPool(String namespace, boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return XmlUtils.getUtilityTransformerPool(()->UtilityTransformerPools.makeAddRootNamespaceXslt(namespace,omitXmlDeclaration,indent),"AddRootNamespace["+namespace+"]",omitXmlDeclaration,indent, 1);
	}

	private static String makeChangeRootXslt(String root, boolean omitXmlDeclaration, boolean indent) {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:output method=\"xml\" indent=\""+(indent?"yes":"no")+"\" omit-xml-declaration=\""+(omitXmlDeclaration?"yes":"no")+"\"/>"
			+ "<xsl:template match=\"/*\">"
			+ "<xsl:element name=\""+root+"\" namespace=\"{namespace-uri()}\">"
			+ "<xsl:for-each select=\"@*\">"
			+ "<xsl:attribute name=\"{name()}\"><xsl:value-of select=\".\"/></xsl:attribute>"
			+ "</xsl:for-each>"
			+ "<xsl:copy-of select=\"*\"/>"
			+ "</xsl:element>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static TransformerPool getChangeRootTransformerPool(String root, boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return XmlUtils.getUtilityTransformerPool(()->UtilityTransformerPools.makeChangeRootXslt(root,omitXmlDeclaration,indent),"ChangeRoot["+root+"]",omitXmlDeclaration,indent, 1);
	}

	private static String makeRemoveUnusedNamespacesXslt(boolean omitXmlDeclaration, boolean indent) {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:output method=\"xml\" indent=\""+(indent?"yes":"no")+"\" omit-xml-declaration=\""+(omitXmlDeclaration?"yes":"no")+"\"/>"
			+ "<xsl:template match=\"*\">"
			+ "<xsl:element name=\"{local-name()}\" namespace=\"{namespace-uri()}\">"
			+ "<xsl:apply-templates select=\"@* | node()\"/>"
			+ "</xsl:element>"
			+ "</xsl:template>"
			+ "<xsl:template match=\"@* | comment() | processing-instruction() | text()\">"
			+ "<xsl:copy/>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}

	public static TransformerPool getRemoveUnusedNamespacesTransformerPool(boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return XmlUtils.getUtilityTransformerPool(()->UtilityTransformerPools.makeRemoveUnusedNamespacesXslt(omitXmlDeclaration,indent),"RemoveUnusedNamespaces",omitXmlDeclaration,indent, 1);
	}

	private static String makeRemoveUnusedNamespacesXslt2(boolean omitXmlDeclaration, boolean indent) {
		return "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
				+ "<xsl:output method=\"xml\" indent=\""
				+ (indent ? "yes" : "no")
				+ "\" omit-xml-declaration=\""
				+ (omitXmlDeclaration ? "yes" : "no")
				+ "\"/>"
				+ "<xsl:variable name=\"colon\" select=\"':'\"/>"
				+ "<xsl:template match=\"*\">"
				+ "<xsl:element name=\"{local-name()}\" namespace=\"{namespace-uri()}\">"
				+ "<xsl:apply-templates select=\"@* | node()\"/>"
				+ "</xsl:element>"
				+ "</xsl:template>"
				+ "<xsl:template match=\"@*\">"
				+ "<xsl:choose>"
				+ "<xsl:when test=\"local-name()='type' and namespace-uri()='http://www.w3.org/2001/XMLSchema-instance' and contains(.,$colon)\">"
				+ "<xsl:variable name=\"prefix\" select=\"substring-before(.,$colon)\"/>"
				+ "<xsl:variable name=\"namespace\" select=\"namespace-uri-for-prefix($prefix, parent::*)\"/>"
				+ "<xsl:variable name=\"parentNamespace\" select=\"namespace-uri(parent::*)\"/>"
				+ "<xsl:choose>"
				+ "<xsl:when test=\"$namespace=$parentNamespace\">"
				+ "<xsl:attribute name=\"{name()}\" namespace=\"{namespace-uri()}\"><xsl:value-of select=\"substring-after(.,$colon)\"/></xsl:attribute>"
				+ "</xsl:when>"
				+ "<xsl:otherwise>"
				+ "<xsl:copy/>"
				+ "</xsl:otherwise>"
				+ "</xsl:choose>"
				+ "</xsl:when>"
				+ "<xsl:otherwise>"
				+ "<xsl:copy/>"
				+ "</xsl:otherwise>"
				+ "</xsl:choose>"
				+ "</xsl:template>"
				+ "<xsl:template match=\"comment() | processing-instruction() | text()\">"
				+ "<xsl:copy/>" + "</xsl:template>" + "</xsl:stylesheet>";
	}

	public static TransformerPool getRemoveUnusedNamespacesXslt2TransformerPool(boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return XmlUtils.getUtilityTransformerPool(()->makeRemoveUnusedNamespacesXslt2(omitXmlDeclaration,indent),"RemoveUnusedNamespacesXslt2",omitXmlDeclaration,indent, 2);
	}

	private static String makeCopyOfSelectXslt(String xpath, boolean omitXmlDeclaration, boolean indent) {
		return "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
				+ "<xsl:output method=\"xml\" indent=\""
				+ (indent ? "yes" : "no")
				+ "\" omit-xml-declaration=\""
				+ (omitXmlDeclaration ? "yes" : "no")
				+ "\"/>"
				+ "<xsl:strip-space elements=\"*\"/>"
				+ "<xsl:template match=\"/*\">"
				+ "<xsl:copy-of select=\""
				+ xpath + "\"/>" + "</xsl:template>" + "</xsl:stylesheet>";
	}

	public static TransformerPool getCopyOfSelectTransformerPool(String xpath, boolean omitXmlDeclaration, boolean indent) throws ConfigurationException {
		return XmlUtils.getUtilityTransformerPool(()->UtilityTransformerPools.makeCopyOfSelectXslt(xpath,omitXmlDeclaration,indent),"CopyOfSelect["+xpath+"]",omitXmlDeclaration,indent, 2);
	}

}
