<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="text" indent="no" />
	<!--
		This XSLT transforms the entire Frank!Framework configuration to a flowchart in dot format:
		- every adapter in the flow is represented by a box
		- every listener (in a receiver) is represented by an ellipse; excepting an interal JavaListener which is represented by a point
		- the following listeners are recognized: JAVA (interal/external) (JavaListener), FILE (DirectoryListener), FXF (FxfListener), JDBC (JdbcQueryListener), JMS (JmsListener), SAP (SapListener), TIBCO (EsbJmsListener) and WEB (WebServiceListener)
		- for every listener (in a receiver) a line (with arrowhead) is drawn between the listener and the adapter
		- every sender (in a pipe) is represented by an ellipse; by class and adapter only one sender is included in the flow
		- the following senders are recognized: JAVA (internal) (IbisLocalSender), JAVA (external) (IbisJavaSender), FXF (FxfSender), JDBC (JdbcQueryListener/DirectQuerySender/XmlQuerySender), JMS (JmsSender), LDAP (LdapSender), LOG (LogSender), SAP (SapSender), TIBCO (EsbJmsSender) and WEB (WebServiceSender)
		- for every sender (in a pipe) a line (with arrowhead) is drawn between the adapter and the sender
		- every job (in a scheduler) is represented by a octagon
		- if a job has function 'sendMessage' then a line (with arrowhead) is drawn between the job and concerned listener
		- if a job hasn't function 'sendMessage' then a line (with arrowhead) is drawn between the job and  the function represented by plaintext
	-->
	<xsl:variable name="space" select="' '" />
	<xsl:template match="/">
		<xsl:text>digraph</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>{</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:text>node [shape=ellipse]</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:apply-templates select="//adapter" />
		<xsl:apply-templates select="//scheduler/job" />
		<xsl:text>}</xsl:text>
	</xsl:template>
	<xsl:template match="adapter">
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="@name" />
		<xsl:text>&quot; [shape=box]</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:apply-templates select="receiver/listener">
			<xsl:with-param name="adapterName" select="@name" />
		</xsl:apply-templates>
		<xsl:apply-templates select="pipeline/pipe//sender">
			<xsl:with-param name="adapterName" select="@name" />
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="listener">
		<xsl:param name="adapterName" />
		<xsl:variable name="class">
			<xsl:call-template name="lastIndexOf">
				<xsl:with-param name="string" select="@className" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$class='JavaListener'">
				<xsl:if test="string-length(@name)&gt;0">
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="concat('JAVA_INT_',@name)" />
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:text>[shape=point,label=</xsl:text>
					<xsl:text>&quot;</xsl:text>
					<xsl:text>JAVA\n(internal)</xsl:text>
					<xsl:text>&quot;</xsl:text>
					<xsl:text>]</xsl:text>
					<xsl:text>&#10;</xsl:text>
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="concat('JAVA_INT_',@name)" />
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:text>-&gt;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$adapterName" />
					<xsl:text>&quot;</xsl:text>
					<xsl:text>&#10;</xsl:text>
				</xsl:if>
				<xsl:if test="string-length(@serviceName)&gt;0">
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="concat('JAVA_EXT_',@name)" />
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:text>[label=</xsl:text>
					<xsl:text>&quot;</xsl:text>
					<xsl:text>JAVA\n(external)</xsl:text>
					<xsl:text>&quot;</xsl:text>
					<xsl:text>]</xsl:text>
					<xsl:text>&#10;</xsl:text>
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="concat('JAVA_EXT_',@name)" />
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:text>-&gt;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$adapterName" />
					<xsl:text>&quot;</xsl:text>
					<xsl:text>&#10;</xsl:text>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="$class='DirectoryListener'">
						<xsl:text>FILE</xsl:text>
					</xsl:when>
					<xsl:when test="$class='FxfListener'">
						<xsl:text>FXF</xsl:text>
					</xsl:when>
					<xsl:when test="$class='JdbcQueryListener'">
						<xsl:text>JDBC</xsl:text>
					</xsl:when>
					<xsl:when test="$class='JmsListener'">
						<xsl:text>JMS</xsl:text>
					</xsl:when>
					<xsl:when test="$class='SapListener'">
						<xsl:text>SAP</xsl:text>
					</xsl:when>
					<xsl:when test="$class='EsbJmsListener'">
						<xsl:text>TIBCO</xsl:text>
					</xsl:when>
					<xsl:when test="$class='WebServiceListener'">
						<xsl:text>WEB</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$class" />
					</xsl:otherwise>
				</xsl:choose>
				<xsl:value-of select="$space" />
				<xsl:text>-&gt;</xsl:text>
				<xsl:value-of select="$space" />
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="$adapterName" />
				<xsl:text>&quot;</xsl:text>
				<xsl:text>&#10;</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="sender">
		<xsl:param name="adapterName" />
		<xsl:variable name="className" select="@className" />
		<xsl:variable name="class">
			<xsl:call-template name="lastIndexOf">
				<xsl:with-param name="string" select="$className" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$class='IbisLocalSender' and string-length(@javaListener)&gt;0">
				<xsl:variable name="javaListener" select="@javaListener" />
				<xsl:if test="count(ancestor::*[name()='pipe']/preceding-sibling::*//sender[@className=$className and @javaListener=$javaListener])=0">
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$adapterName" />
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:text>-&gt;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="concat('JAVA_INT_',@javaListener)" />
					<xsl:text>&quot;</xsl:text>
					<xsl:text>&#10;</xsl:text>
				</xsl:if>
			</xsl:when>
			<xsl:when test="$class='FixedQuerySender' or $class='DirectQuerySender' or $class='XmlQuerySender'">
				<xsl:if test="count(ancestor::*[name()='pipe']/preceding-sibling::*//sender[contains(@className,'QuerySender')])=0">
					<xsl:variable name="id" select="generate-id(.)" />
					<xsl:value-of select="$id" />
					<xsl:value-of select="$space" />
					<xsl:text>[label=</xsl:text>
					<xsl:text>JDBC</xsl:text>
					<xsl:text>]</xsl:text>
					<xsl:text>&#10;</xsl:text>
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$adapterName" />
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:text>-&gt;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:value-of select="$id" />
					<xsl:text>&#10;</xsl:text>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="count(ancestor::*[name()='pipe']/preceding-sibling::*//sender[@className=$className])=0">
					<xsl:variable name="id" select="generate-id(.)" />
					<xsl:value-of select="$id" />
					<xsl:value-of select="$space" />
					<xsl:text>[label=</xsl:text>
					<xsl:choose>
						<xsl:when test="$class='FxfSender'">
							<xsl:text>FXF</xsl:text>
						</xsl:when>
						<xsl:when test="$class='IbisJavaSender'">
							<xsl:text>&quot;</xsl:text>
							<xsl:text>JAVA\n(external)</xsl:text>
							<xsl:text>&quot;</xsl:text>
						</xsl:when>
						<xsl:when test="$class='FixedQuerySender' or $class='DirectQuerySender' or $class='XmlQuerySender'">
							<xsl:text>JDBC</xsl:text>
						</xsl:when>
						<xsl:when test="$class='JmsSender'">
							<xsl:text>JMS</xsl:text>
						</xsl:when>
						<xsl:when test="$class='LdapSender'">
							<xsl:text>LDAP</xsl:text>
						</xsl:when>
						<xsl:when test="$class='LogSender'">
							<xsl:text>LOG</xsl:text>
						</xsl:when>
						<xsl:when test="$class='SapSender'">
							<xsl:text>SAP</xsl:text>
						</xsl:when>
						<xsl:when test="$class='EsbJmsSender'">
							<xsl:text>TIBCO</xsl:text>
						</xsl:when>
						<xsl:when test="$class='WebServiceSender'">
							<xsl:text>WEB</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$class" />
						</xsl:otherwise>
					</xsl:choose>
					<xsl:text>]</xsl:text>
					<xsl:text>&#10;</xsl:text>
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$adapterName" />
					<xsl:text>&quot;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:text>-&gt;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:value-of select="$id" />
					<xsl:text>&#10;</xsl:text>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="job">
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="concat('JOB_',@name)" />
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>[shape=octagon,label=</xsl:text>
		<xsl:text>&quot;</xsl:text>
		<xsl:text>JOB</xsl:text>
		<xsl:text>&quot;</xsl:text>
		<xsl:text>]</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:choose>
			<xsl:when test="@function='sendMessage'">
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="concat('JOB_',@name)" />
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="$space" />
				<xsl:text>-&gt;</xsl:text>
				<xsl:value-of select="$space" />
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="concat('JAVA_INT_',@receiverName)" />
				<xsl:text>&quot;</xsl:text>
				<xsl:text>&#10;</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="id" select="generate-id(.)" />
				<xsl:value-of select="$id" />
				<xsl:value-of select="$space" />
				<xsl:text>[shape=plaintext,label=</xsl:text>
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="@function" />
				<xsl:text>&quot;</xsl:text>
				<xsl:text>]</xsl:text>
				<xsl:text>&#10;</xsl:text>
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="concat('JOB_',@name)" />
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="$space" />
				<xsl:text>-&gt;</xsl:text>
				<xsl:value-of select="$space" />
				<xsl:value-of select="$id" />
				<xsl:text>&#10;</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="lastIndexOf">
		<xsl:param name="string" />
		<xsl:variable name="char">.</xsl:variable>
		<xsl:choose>
			<xsl:when test="contains($string, $char)">
				<xsl:call-template name="lastIndexOf">
					<xsl:with-param name="string" select="substring-after($string, $char)" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$string" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
