<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:stub="http://frankframework.org/stub">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<!-- Parameter disableValidators has been used to test the impact of validators on memory usage -->
	<xsl:param name="disableValidators"/>
	<!--
		This XSLT adjusts the Frank!Framework configuration as follows:
		- disable all receiver elements, except those with childs JdbcQueryListener, DirectoryListener, JavaListener, WebServiceListener and RestListener
		- add a default receiver (name="testtool-[adapter name]") with a child JavaListener (serviceName="testtool-[adapter name]") to each adapter (and copy all attributes (except transactionAttribute=Mandatory, this is replaced with Required), errorStorage and messageLog from disabled receiver when present)
		- disable all listener elements which have a parent pipe
		- stub all sender elements, which have a parent pipe, by an IbisJavaSender (serviceName="testtool-[pipe name]"), except the ResultSet2FileSender, DirectQuerySender, FixedQuerySender, XmlQuerySender, DelaySender, EchoSender, IbisLocalSender, LogSender, ParallelSenders, SenderSeries, SenderWrapper, XsltSender, CommandSender, FixedResultSender, FileSender, JavascriptSender, MessageStoreSender and ZipWriterSender
		- disable all elements sapSystems
		- disable all elements jmsRealm which have an attribute queueConnectionFactoryName (if combined with the attribute datasourceName a new jmsRealm for this datasourceName is created)
		- add the attribute returnFixedDate with value true to all pipe elements PutSystemDateInSession
		- replace the value '{now,...,...}' of the attribute pattern in all param elements with the value '{fixeddate,...,...}'
		- add the attribute useFixedValues with value true to all pipe, inputWrapper and outputWrapper elements SoapWrapperPipe
		- stub the pipe element GetPrincipalPipe by a pipe element FixedResultPipe with attribute returnString set to tst9
		- stub the pipe element IsUserInRolePipe by a pipe element EchoPipe
		- stub the pipe element UUIDGeneratorPipe by a pipe element FixedResultPipe with attribute returnString set to 1234567890123456789012345678901 if type='numeric' and 0a4544b6-37489ec0_15ad0f006ae_-7ff3 otherwise
		- stub the pipe element FtpFileRetrieverPipe, LdapFindMemberPipe, LdapFindGroupMembershipsPipe and SendTibcoMessage by a pipe element SenderPipe (and copy the attributes name, storeResultInSessionKey, getInputFromSessionKey and getInputFromFixedValue) with a child Ibis4JavaSender (serviceName="testtool-[pipe name]")
		- add the attribute timeOutOnResult with value '[timeout]' and attribute exceptionOnResult with value '[error]' to all pipe elements SenderPipe and ForEachChildElementPipe
		- add, if not available, the parameter destination with value 'P2P.Infrastructure.Ibis4TestTool.Stub.Request/Action' to all pipe and inputWrapper elements SoapWrapperPipe with attribute direction=wrap
		- add, if not available, the parameter destination with value 'P2P.Infrastructure.Ibis4TestTool.Stub.Response' to all outputWrapper elements SoapWrapperPipe with attribute direction=wrap
	-->
	<xsl:template match="/">
		<xsl:apply-templates select="*|comment()|processing-instruction()" />
	</xsl:template>

	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:call-template name="copy" />
	</xsl:template>

	<xsl:template match="adapter">
		<xsl:element name="adapter">
			<xsl:apply-templates select="@*" />
			<xsl:call-template name="stubAdapterReceiver"/>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>

	<!-- All receivers are disabled except those with listeners in the list below -->
	<xsl:template match="receiver[listener[@className='org.frankframework.jdbc.JdbcQueryListener'
										or @className='org.frankframework.jdbc.JdbcTableListener'
										or @className='org.frankframework.receivers.DirectoryListener'
										or @className='org.frankframework.receivers.JavaListener'
										or @className='org.frankframework.http.WebServiceListener'
										or @className='org.frankframework.http.RestListener'
										or @className='org.frankframework.jdbc.MessageStoreListener'
										or @className='org.frankframework.http.rest.ApiListener']]">
		<xsl:call-template name="copy" />
		<xsl:call-template name="stubReceiver"/>
	</xsl:template>

	<xsl:template match="receiver">
		<xsl:call-template name="disable" />
		<xsl:call-template name="stubReceiver"/>
	</xsl:template>

	<xsl:template name="stubAdapterReceiver">
		<xsl:variable name="receiverName" select="concat('testtool-',@name)"/>
		<xsl:variable name="baseReceiver" select="receiver[1]"/>
		<xsl:element name="receiver">
			<xsl:attribute name="name">
				<xsl:value-of select="$receiverName" />
			</xsl:attribute>
			<xsl:apply-templates select="$baseReceiver/@transactionAttribute" mode="stub"/>
			<xsl:apply-templates select="$baseReceiver/@*[local-name()!='transactionAttribute' and local-name()!='name']" />
			<xsl:element name="listener">
				<xsl:attribute name="className">org.frankframework.receivers.JavaListener</xsl:attribute>
				<xsl:attribute name="serviceName">
					<xsl:value-of select="$receiverName" />
				</xsl:attribute>
				<xsl:if test="errorMessageFormatter">
					<xsl:attribute name="throwException">false</xsl:attribute>
				</xsl:if>
			</xsl:element>
			<xsl:call-template name="stubNameForStorage">
				<xsl:with-param name="store" select="$baseReceiver/errorStorage[@className='org.frankframework.jdbc.JdbcTransactionalStorage' or @className='org.frankframework.jdbc.DummyTransactionalStorage']"/>
			</xsl:call-template>
			<xsl:copy-of select="errorSender[@className='org.frankframework.senders.IbisLocalSender']"/>
			<xsl:call-template name="stubNameForStorage">
				<xsl:with-param name="store" select="$baseReceiver/messageLog[@className='org.frankframework.jdbc.JdbcTransactionalStorage' or @className='org.frankframework.jdbc.DummyTransactionalStorage']"/>
			</xsl:call-template>
		</xsl:element>
	</xsl:template>

	<xsl:template name="stubReceiver">
		<xsl:variable name="receiverName" select="string-join(('testtool',(parent::adapter/@name,xs:string(count(preceding-sibling::receiver)+1))),'-')"/>

		<xsl:element name="receiver">
			<xsl:attribute name="name">
				<xsl:value-of select="$receiverName" />
			</xsl:attribute>
			<xsl:apply-templates select="@transactionAttribute" mode="stub"/>
			<xsl:apply-templates select="@*[name()!='transactionAttribute' and name()!='name']" />
			<xsl:element name="listener">
				<xsl:attribute name="className">org.frankframework.receivers.JavaListener</xsl:attribute>
				<xsl:attribute name="serviceName">
					<xsl:value-of select="$receiverName" />
				</xsl:attribute>
				<xsl:if test="parent::adapter/errorMessageFormatter">
					<xsl:attribute name="throwException">false</xsl:attribute>
				</xsl:if>
			</xsl:element>
			<xsl:call-template name="stubNameForStorage">
				<xsl:with-param name="store" select="errorStorage[@className='org.frankframework.jdbc.JdbcTransactionalStorage' or @className='org.frankframework.jdbc.DummyTransactionalStorage']"/>
			</xsl:call-template>
			<xsl:copy-of select="errorSender[@className='org.frankframework.senders.IbisLocalSender']"/>
			<xsl:call-template name="stubNameForStorage">
				<xsl:with-param name="store" select="messageLog[@className='org.frankframework.jdbc.JdbcTransactionalStorage' or @className='org.frankframework.jdbc.DummyTransactionalStorage']"/>
			</xsl:call-template>
		</xsl:element>
	</xsl:template>

	<xsl:template name="stubNameForStorage">
		<xsl:param name="store"/>

		<xsl:if test="not(empty($store))">
			<xsl:variable name="elementName" select="name($store)"/>
			<xsl:element name="{$elementName}">
				<xsl:for-each select="$store/@*">
					<xsl:choose>
						<xsl:when test="name()='slotId'">
							<xsl:attribute name="{name()}"><xsl:value-of select="concat('stubbed-',.)"/></xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:for-each>
			</xsl:element>
		</xsl:if>
	</xsl:template>

	<xsl:template match="receiver/@transactionAttribute" mode="stub">
		<xsl:attribute name="transactionAttribute">
			<xsl:value-of select="if (.='Mandatory') then 'Required' else ."/>
		</xsl:attribute>
	</xsl:template>

	<!-- All senders are stubbed except those in the list below -->
	<xsl:template match="sender[   @className='org.frankframework.jdbc.ResultSet2FileSender'
								or @className='org.frankframework.jdbc.DirectQuerySender'
								or @className='org.frankframework.jdbc.FixedQuerySender'
								or @className='org.frankframework.jdbc.XmlQuerySender'
								or @className='org.frankframework.senders.DelaySender'
								or @className='org.frankframework.senders.EchoSender'
								or @className='org.frankframework.senders.IbisLocalSender'
								or @className='org.frankframework.senders.LogSender'
								or @className='org.frankframework.senders.ParallelSenders'
								or @className='org.frankframework.senders.SenderSeries'
								or @className='org.frankframework.senders.SenderWrapper'
								or @className='org.frankframework.senders.XsltSender'
								or @className='org.frankframework.senders.CommandSender'
								or @className='org.frankframework.senders.FixedResultSender'
								or @className='org.frankframework.senders.JavascriptSender'
								or @className='org.frankframework.jdbc.MessageStoreSender'
								or @className='org.frankframework.senders.ReloadSender'
								or @className='org.frankframework.compression.ZipWriterSender'
								or @className='org.frankframework.senders.LocalFileSystemSender']">
		<xsl:call-template name="copy" />
	</xsl:template>

	<xsl:template match="sender">
		<xsl:call-template name="disable" />

		<xsl:element name="sender">
			<xsl:if test="string-length(@name)&gt;0">
				<xsl:attribute name="name">
					<xsl:value-of select="@name" />
				</xsl:attribute>
			</xsl:if>
			<xsl:attribute name="className">org.frankframework.senders.IbisJavaSender</xsl:attribute>
			<xsl:attribute name="serviceName">
				<xsl:choose>
					<!-- For backwards compatibility, the servicename based on the parent pipe name is the first option -->
					<xsl:when test="parent::pipe">
						<xsl:value-of select="concat('testtool-',parent::pipe/@name)" />
					</xsl:when>
					<xsl:when test="@name">
						<xsl:value-of select="concat('testtool-',@name)" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:variable name="senderNames">
							<xsl:element name="senderNames">
								<xsl:call-template name="determineStubSenderNames"/>
							</xsl:element>
						</xsl:variable>
						<xsl:value-of select="string-join(('testtool',$senderNames/senderNames/stubName),'-')" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:if test="string-length(@multipartResponse)&gt;0">
				<xsl:attribute name="multipartResponse">
					<xsl:value-of select="@multipartResponse" />
				</xsl:attribute>
			</xsl:if>
		</xsl:element>
	</xsl:template>

	<xsl:template name="determineStubSenderNames">
		<xsl:choose>
			<xsl:when test="@name">
				<xsl:element name="stubName">
					<xsl:value-of select="@name"/>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:for-each select="parent::*">
					<xsl:call-template name="determineStubSenderNames"/>
				</xsl:for-each>
				<xsl:element name="stubName">
					<xsl:value-of select="string-join((tokenize(@className,'\.')[last()],xs:string(count(preceding-sibling::sender)+1)),'-')"/>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="pipe/listener">
		<xsl:call-template name="disable" />
	</xsl:template>

	<xsl:template match="listener">
		<xsl:call-template name="copy" />
	</xsl:template>

	<xsl:template match="sapSystems">
		<xsl:call-template name="disable" />
	</xsl:template>

	<xsl:template match="jmsRealm[@queueConnectionFactoryName]">
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
	</xsl:template>

	<xsl:template match="pipe[@className='org.frankframework.pipes.PutSystemDateInSession']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="returnFixedDate">true</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="pipe[@className='org.frankframework.extensions.esb.EsbSoapWrapperPipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="useFixedValues">true</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
			<xsl:if test="(@direction='wrap' or string-length(@direction)=0) and string-length(param[@name='destination']/@value)=0">
				<xsl:element name="param">
					<xsl:attribute name="name">destination</xsl:attribute>
					<xsl:attribute name="value">P2P.Infrastructure.Ibis4TestTool.Stub.Request</xsl:attribute>
				</xsl:element>
			</xsl:if>
		</xsl:element>
	</xsl:template>

	<xsl:template match="inputWrapper[@className='org.frankframework.extensions.esb.EsbSoapWrapperPipe']">
		<xsl:element name="inputWrapper">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="useFixedValues">true</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
			<xsl:if test="(@direction='wrap' or string-length(@direction)=0) and string-length(param[@name='destination']/@value)=0">
				<xsl:element name="param">
					<xsl:attribute name="name">destination</xsl:attribute>
					<xsl:choose>
						<xsl:when test="parent::*/outputWrapper">
							<xsl:attribute name="value">P2P.Infrastructure.Ibis4TestTool.Stub.Request</xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="value">P2P.Infrastructure.Ibis4TestTool.Stub.Action</xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>
			</xsl:if>
		</xsl:element>
	</xsl:template>

	<xsl:template match="outputWrapper[@className='org.frankframework.extensions.esb.EsbSoapWrapperPipe']">
		<xsl:element name="outputWrapper">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="useFixedValues">true</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
			<xsl:if test="(@direction='wrap' or string-length(@direction)=0) and string-length(param[@name='destination']/@value)=0">
				<xsl:element name="param">
					<xsl:attribute name="name">destination</xsl:attribute>
					<xsl:attribute name="value">P2P.Infrastructure.Ibis4TestTool.Stub.Response</xsl:attribute>
				</xsl:element>
			</xsl:if>
		</xsl:element>
	</xsl:template>

	<xsl:template match="pipe[@className='org.frankframework.pipes.GetPrincipalPipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="className">org.frankframework.pipes.EchoPipe</xsl:attribute>
			<xsl:attribute name="getInputFromFixedValue">tst9</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="pipe[@className='org.frankframework.pipes.IsUserInRolePipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="className">org.frankframework.pipes.EchoPipe</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="pipe[@className='org.frankframework.pipes.UUIDGeneratorPipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*[name()!='type']" />
			<xsl:attribute name="className">org.frankframework.pipes.EchoPipe</xsl:attribute>
			<xsl:choose>
				<xsl:when test="@type='numeric'">
					<xsl:attribute name="getInputFromFixedValue">1234567890123456789012345678901</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="getInputFromFixedValue">0a4544b6-37489ec0_15ad0f006ae_-7ff3</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="pipe[ @className='org.frankframework.pipes.Samba2Pipe' ]">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@name|@action|@storeResultInSessionKey|@getInputFromSessionKey|@getInputFromFixedValue" />
			<xsl:attribute name="className">org.frankframework.pipes.LocalFileSystemPipe</xsl:attribute>
			<xsl:apply-templates
				select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="pipe[ @className='org.frankframework.ftp.FtpFileRetrieverPipe'
							or @className='org.frankframework.extensions.tibco.SendTibcoMessage'
							or @className='org.frankframework.ldap.LdapFindMemberPipe'
							or @className='org.frankframework.ldap.LdapFindGroupMembershipsPipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@name|@storeResultInSessionKey|@getInputFromSessionKey|@getInputFromFixedValue" />
			<xsl:attribute name="className">org.frankframework.pipes.SenderPipe</xsl:attribute>
			<xsl:element name="sender">
				<xsl:attribute name="className">org.frankframework.senders.IbisJavaSender</xsl:attribute>
				<xsl:attribute name="serviceName">
					<xsl:value-of select="concat('testtool-',@name)" />
				</xsl:attribute>
			</xsl:element>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="pipe[ @className='org.frankframework.pipes.SenderPipe'
							or @className='org.frankframework.pipes.ForEachChildElementPipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="timeoutOnResult">[timeout]</xsl:attribute>
			<xsl:attribute name="exceptionOnResult">[error]</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="param/@pattern[contains(.,'{now,') or contains(.,'{now}')]">
		<xsl:attribute name="pattern"><xsl:value-of select="replace(.,'\{now','{fixedDate')"/></xsl:attribute>
	</xsl:template>

	<xsl:template match="pipe/*[local-name()='errorStorage' or local-name()='messageLog'][@className!='org.frankframework.jdbc.JdbcTransactionalStorage'
																					  and @className!='org.frankframework.jdbc.DummyTransactionalStorage']">
		<xsl:call-template name="disable" />
	</xsl:template>

	<xsl:template match="inputValidator[$disableValidators]|outputValidator[$disableValidators]">
		<xsl:call-template name="disable" />
	</xsl:template>

	<xsl:template name="copy">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|comment()|processing-instruction()|text()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template name="disable">
		<xsl:comment>
			<xsl:copy>
				<xsl:apply-templates select="." mode="escape" />
			</xsl:copy>
		</xsl:comment>
	</xsl:template>

	<!-- Escape xml tag opening(<) and closing(>) signs so that xsl:comment can process the copy of the xml. Processes elements and attributes only -->
	<xsl:template match="*" mode="escape">
		<xsl:variable name="apos">'</xsl:variable>
		<!-- Start element -->
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()" />

		<!-- Attributes -->
		<xsl:for-each select="@*">
			<xsl:value-of select="concat(' ', name(), '=', $apos, ., $apos)"/>
		</xsl:for-each>

		<!-- End opening tag -->
		<xsl:text>&gt;</xsl:text>

		<!-- Children -->
		<xsl:apply-templates select="node()" mode="escape" />

		<!-- End element -->
		<xsl:text>&lt;/</xsl:text>
		<xsl:value-of select="name()" />
		<xsl:text>&gt;</xsl:text>
	</xsl:template>

	<!-- Disable stubbing if set. -->
	<!-- This uses priority="1" to ensure it has higher priority than matching templates with default priority, which would otherwise result in error XTRE0540 (ambiguous rule match)-->
	<xsl:template match="*[lower-case(@stub:disableStub)=('true','!false')]" priority="1">
		<xsl:copy-of select="."/>
	</xsl:template>

	<!-- disableStub can be defined on the listener, in that case also do not stub the receiver -->
	<xsl:template match="receiver[lower-case(listener/@stub:disableStub)=('true','!false')]" priority="1">
		<xsl:copy-of select="."/>
	</xsl:template>
</xsl:stylesheet>
