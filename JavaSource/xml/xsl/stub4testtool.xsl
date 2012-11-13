<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" />
	<!--
		This XSLT adjusts the IBIS configuration as follows:
		- disable all receiver elements, except those with childs JdbcQueryListener, DirectoryListener, JavaListener and WebServiceListener
		- add a default receiver (name="testtool-[adapter name]") with a child JavaListener (serviceName="testtool-[adapter name]") to each adapter (and copy all attributes (except transactionAttribute), errorStorage and messageLog from disabled receiver when present)
		- disable all listener elements which have a parent pipe
		- stub all sender elements, which have a parent pipe, by an IbisJavaSender (serviceName="testtool-[pipe name]"), except the ResultSet2FileSender, DirectQuerySender, FixedQuerySender, XmlQuerySender, DelaySender, EchoSender, IbisLocalSender, LogSender, ParallelSenders, SenderSeries, SenderWrapper, XsltSender, CommandSender, FixedResultSender and FileSender
		- disable all elements sapSystems
		- disable all elements jmsRealm which have an attribute queueConnectionFactoryName (if combined with the attribute datasourceName a new jmsRealm for this datasourceName is created)
		- add the attribute returnFixedDate with value true to all pipe elements PutSystemDateInSession
		- replace the value '{now,...,...}' of the attribute pattern in all param elements with the value '{fixeddate,...,...}'
		- stub the pipe element FtpFileRetrieverPipe by a pipe element GenericMessageSendingPipe (and copy the attributes name, storeResultInSessionKey, getInputFromSessionKey and getInputFromFixedValue) with a child Ibis4JavaSender (serviceName="testtool-[pipe name]")
		- add the attribute timeOutOnResult with value '[timeout]' and attribute exceptionOnResult with value '[error]' to all pipe elements GenericMessageSendingPipe and ForEachChildElementPipe
	-->
	<xsl:template match="/">
		<xsl:apply-templates select="*|@*|comment()|processing-instruction()" />
	</xsl:template>
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:choose>
			<xsl:when test="name()='receiver'">
				<xsl:choose>
					<xsl:when test="listener[@className='nl.nn.adapterframework.jdbc.JdbcQueryListener']">
						<xsl:call-template name="copy" />
					</xsl:when>
					<xsl:when test="listener[@className='nl.nn.adapterframework.receivers.DirectoryListener']">
						<xsl:call-template name="copy" />
					</xsl:when>
					<xsl:when test="listener[@className='nl.nn.adapterframework.receivers.JavaListener']">
						<xsl:call-template name="copy" />
					</xsl:when>
					<xsl:when test="listener[@className='nl.nn.adapterframework.http.WebServiceListener']">
						<xsl:call-template name="copy" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="disable" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="name()='pipeline'">
				<xsl:element name="receiver">
					<xsl:attribute name="className">nl.nn.adapterframework.receivers.GenericReceiver</xsl:attribute>
					<xsl:attribute name="name">
						<xsl:value-of select="concat('testtool-',parent::*[name()='adapter']/@name)" />
					</xsl:attribute>
					<xsl:apply-templates select="parent::*[name()='adapter']/receiver/@*[name()!='transactionAttribute']" />
					<xsl:element name="listener">
						<xsl:attribute name="className">nl.nn.adapterframework.receivers.JavaListener</xsl:attribute>
						<xsl:attribute name="serviceName">
							<xsl:value-of select="concat('testtool-',parent::*[name()='adapter']/@name)" />
						</xsl:attribute>
						<xsl:if test="parent::*[name()='adapter']/errorMessageFormatter">
							<xsl:attribute name="throwException">false</xsl:attribute>
						</xsl:if>
					</xsl:element>
					<xsl:for-each select="parent::*[name()='adapter']/receiver/errorStorage">
						<xsl:if test="position()=1">
							<xsl:copy-of select="." />
						</xsl:if>
					</xsl:for-each>
					<xsl:for-each select="parent::*[name()='adapter']/receiver/messageLog">
						<xsl:if test="position()=1">
							<xsl:copy-of select="." />
						</xsl:if>
					</xsl:for-each>
				</xsl:element>
				<xsl:call-template name="copy" />
			</xsl:when>
			<xsl:when test="name()='listener'">
				<xsl:choose>
					<xsl:when test="parent::*[name()='pipe']">
						<xsl:call-template name="disable" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="copy" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="name()='sender'">
				<xsl:choose>
					<xsl:when test="parent::*[name()='pipe']">
						<xsl:variable name="pipeName" select="parent::*[name()='pipe']/@name" />
						<xsl:choose>
							<xsl:when test="@className='nl.nn.adapterframework.jdbc.ResultSet2FileSender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.jdbc.DirectQuerySender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.jdbc.FixedQuerySender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.jdbc.XmlQuerySender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.DelaySender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.EchoSender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.IbisLocalSender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.LogSender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.ParallelSenders'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.SenderSeries'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.SenderWrapper'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.XsltSender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.CommandSender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.FixedResultSender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.FileSender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:otherwise>
								<xsl:element name="sender">
									<xsl:if test="string-length(@name)&gt;0">
										<xsl:attribute name="name">
											<xsl:value-of select="@name" />
										</xsl:attribute>
									</xsl:if>
									<xsl:attribute name="className">nl.nn.adapterframework.senders.IbisJavaSender</xsl:attribute>
									<xsl:attribute name="serviceName">
										<xsl:value-of select="concat('testtool-',$pipeName)" />
									</xsl:attribute>
								</xsl:element>
								<xsl:call-template name="disable" />
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="copy" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="name()='sapSystems'">
				<xsl:call-template name="disable" />
			</xsl:when>
			<xsl:when test="name()='jmsRealm' and @queueConnectionFactoryName">
				<xsl:call-template name="disable" />
				<xsl:if test="@datasourceName">
					<xsl:element name="jmsRealm">
						<xsl:attribute name="realmName">
							<xsl:value-of select="@realmName" />
						</xsl:attribute>
						<xsl:attribute name="datasourceName">
							<xsl:value-of select="@datasourceName" />
						</xsl:attribute>
					</xsl:element>
				</xsl:if>
			</xsl:when>
			<xsl:when test="name()='pipe' and @className='nl.nn.adapterframework.pipes.PutSystemDateInSession'">
				<xsl:element name="pipe">
					<xsl:apply-templates select="@*" />
					<xsl:attribute name="returnFixedDate">true</xsl:attribute>
					<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
				</xsl:element>
			</xsl:when>
			<xsl:when test="name()='pipe' and @className='nl.nn.adapterframework.ftp.FtpFileRetrieverPipe'">
				<xsl:element name="pipe">
					<xsl:attribute name="name">
						<xsl:value-of select="@name"/>
					</xsl:attribute>
					<xsl:apply-templates select="@*[name()='storeResultInSessionKey' or name()='getInputFromSessionKey' or name()='getInputFromFixedValue']" />
					<xsl:attribute name="className">nl.nn.adapterframework.pipes.GenericMessageSendingPipe</xsl:attribute>
					<xsl:element name="sender">
						<xsl:attribute name="className">nl.nn.adapterframework.senders.IbisJavaSender</xsl:attribute>
						<xsl:attribute name="serviceName">
							<xsl:value-of select="concat('testtool-',@name)" />
						</xsl:attribute>
					</xsl:element>
					<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
				</xsl:element>
				<xsl:call-template name="disable" />
			</xsl:when>
			<xsl:when test="name()='pipe' and (@className='nl.nn.adapterframework.pipes.GenericMessageSendingPipe' or @className='nl.nn.adapterframework.pipes.ForEachChildElementPipe')">
				<xsl:element name="pipe">
					<xsl:apply-templates select="@*" />
					<xsl:attribute name="timeOutOnResult">[timeout]</xsl:attribute>
					<xsl:attribute name="exceptionOnResult">[error]</xsl:attribute>
					<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
				</xsl:element>
			</xsl:when>
			<xsl:when test="name()='param' and starts-with(@pattern,'{now,')">
				<xsl:element name="param">
					<xsl:apply-templates select="@*[name()='pattern'=false()]" />
					<xsl:attribute name="pattern">
						<xsl:value-of select="concat('{fixedDate,',substring-after(@pattern,'{now,'))" />
					</xsl:attribute>
					<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="copy" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="disable">
		<xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
		<xsl:copy>
			<xsl:apply-templates select="*|@*|processing-instruction()|text()" />
		</xsl:copy>
		<xsl:text disable-output-escaping="yes">--&gt;</xsl:text>
	</xsl:template>
	<xsl:template name="copy">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
