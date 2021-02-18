<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0" xmlns:xs="http://www.w3.org/2001/XMLSchema">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
	<!-- Parameter disableValidators has been used to test the impact of validators on memory usage -->
	<xsl:param name="disableValidators"/>
	<!--
		This XSLT adjusts the IBIS configuration as follows:
		- disable all receiver elements, except those with childs JdbcQueryListener, DirectoryListener, JavaListener, WebServiceListener and RestListener
		- add a default receiver (name="testtool-[adapter name]") with a child JavaListener (serviceName="testtool-[adapter name]") to each adapter (and copy all attributes (except transactionAttribute), errorStorage and messageLog from disabled receiver when present)
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
		- stub the pipe element FtpFileRetrieverPipe, LdapFindMemberPipe, LdapFindGroupMembershipsPipe and SendTibcoMessage by a pipe element GenericMessageSendingPipe (and copy the attributes name, storeResultInSessionKey, getInputFromSessionKey and getInputFromFixedValue) with a child Ibis4JavaSender (serviceName="testtool-[pipe name]")
		- add the attribute timeOutOnResult with value '[timeout]' and attribute exceptionOnResult with value '[error]' to all pipe elements GenericMessageSendingPipe and ForEachChildElementPipe
		- add, if not available, the parameter destination with value 'P2P.Infrastructure.Ibis4TestTool.Stub.Request/Action' to all pipe and inputWrapper elements SoapWrapperPipe with attribute direction=wrap 
		- add, if not available, the parameter destination with value 'P2P.Infrastructure.Ibis4TestTool.Stub.Response' to all outputWrapper elements SoapWrapperPipe with attribute direction=wrap 
	-->
	<xsl:template match="/">
		<xsl:apply-templates select="*|@*|comment()|processing-instruction()" />
	</xsl:template>
	
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:call-template name="copy" />
	</xsl:template>
	
	<xsl:template match="adapter">
		<xsl:element name="adapter">
			<xsl:apply-templates select="@*" />
			<xsl:for-each select="receiver[1]">
				<xsl:call-template name="stubReceiver">
					<xsl:with-param name="isAdapterStub" select="true()"/>
				</xsl:call-template>
			</xsl:for-each>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>
	
	<!-- All receivers are disabled except those with listeners in the list below -->
	<xsl:template match="receiver[listener[@className='nl.nn.adapterframework.jdbc.JdbcQueryListener'
										or @className='nl.nn.adapterframework.jdbc.JdbcTableListener'
										or @className='nl.nn.adapterframework.receivers.DirectoryListener'
										or @className='nl.nn.adapterframework.receivers.JavaListener'
										or @className='nl.nn.adapterframework.http.WebServiceListener'
										or @className='nl.nn.adapterframework.http.RestListener'
										or @className='nl.nn.adapterframework.jdbc.MessageStoreListener'
										or @className='nl.nn.adapterframework.http.rest.ApiListener']]">
		<xsl:call-template name="copy" />
		<xsl:call-template name="stubReceiver">
			<xsl:with-param name="isAdapterStub" select="false()"/>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="receiver">
		<xsl:call-template name="disable" />
		<xsl:call-template name="stubReceiver">
			<xsl:with-param name="isAdapterStub" select="false()"/>
		</xsl:call-template>
	</xsl:template>	
	
	<xsl:template name="stubReceiver">
		<xsl:param name="isAdapterStub" as="xs:boolean"/>

		<xsl:variable name="receiverName">
			<xsl:choose>
				<xsl:when test="$isAdapterStub">
					<xsl:value-of select="parent::adapter/@name"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="string-join((parent::adapter/@name,xs:string(count(preceding-sibling::receiver)+1)),'-')"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:element name="receiver">
			<xsl:attribute name="name">
				<xsl:value-of select="concat('testtool-',$receiverName)" />
			</xsl:attribute>
			<xsl:apply-templates select="@*[name()!='transactionAttribute' and name() !='name']" />
			<xsl:element name="listener">
				<xsl:attribute name="className">nl.nn.adapterframework.receivers.JavaListener</xsl:attribute>
				<xsl:attribute name="serviceName">
					<xsl:value-of select="concat('testtool-',$receiverName)" />
				</xsl:attribute>
				<xsl:if test="parent::*[adapter]/errorMessageFormatter">
					<xsl:attribute name="throwException">false</xsl:attribute>
				</xsl:if>
			</xsl:element>
			<xsl:call-template name="stubNameForStorage">
				<xsl:with-param name="store" select="errorStorage[@className='nl.nn.adapterframework.jdbc.JdbcTransactionalStorage' or @className='nl.nn.adapterframework.jdbc.DummyTransactionalStorage']"/>
			</xsl:call-template>
			<xsl:copy-of select="errorSender[@className='nl.nn.adapterframework.senders.IbisLocalSender']"/>
			<xsl:call-template name="stubNameForStorage">
				<xsl:with-param name="store" select="messageLog[@className='nl.nn.adapterframework.jdbc.JdbcTransactionalStorage' or @className='nl.nn.adapterframework.jdbc.DummyTransactionalStorage']"/>
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
	
	<!-- All senders are stubbed except those in the list below -->
	<xsl:template match="sender[   @className='nl.nn.adapterframework.jdbc.ResultSet2FileSender'
								or @className='nl.nn.adapterframework.jdbc.DirectQuerySender'
								or @className='nl.nn.adapterframework.jdbc.FixedQuerySender'
								or @className='nl.nn.adapterframework.jdbc.XmlQuerySender'
								or @className='nl.nn.adapterframework.senders.DelaySender'
								or @className='nl.nn.adapterframework.senders.EchoSender'
								or @className='nl.nn.adapterframework.senders.IbisLocalSender'
								or @className='nl.nn.adapterframework.senders.LogSender'
								or @className='nl.nn.adapterframework.senders.ParallelSenders'
								or @className='nl.nn.adapterframework.senders.SenderSeries'
								or @className='nl.nn.adapterframework.senders.SenderWrapper'
								or @className='nl.nn.adapterframework.senders.XsltSender'
								or @className='nl.nn.adapterframework.senders.CommandSender'
								or @className='nl.nn.adapterframework.senders.FixedResultSender'
								or @className='nl.nn.adapterframework.senders.JavascriptSender'
								or @className='nl.nn.adapterframework.jdbc.MessageStoreSender'
								or @className='nl.nn.adapterframework.senders.ReloadSender'
								or @className='nl.nn.adapterframework.compression.ZipWriterSender'
								or @className='nl.nn.adapterframework.senders.LocalFileSystemSender']">
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
			<xsl:attribute name="className">nl.nn.adapterframework.senders.IbisJavaSender</xsl:attribute>
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
	
	<xsl:template match="pipe[@className='nl.nn.adapterframework.pipes.PutSystemDateInSession']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="returnFixedDate">true</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="pipe[@className='nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe']">
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
	
	<xsl:template match="inputWrapper[@className='nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe']">
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
	
	<xsl:template match="outputWrapper[@className='nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe']">
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
	
	<xsl:template match="pipe[@className='nl.nn.adapterframework.pipes.GetPrincipalPipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="className">nl.nn.adapterframework.pipes.FixedResultPipe</xsl:attribute>
			<xsl:attribute name="returnString">tst9</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="pipe[@className='nl.nn.adapterframework.pipes.IsUserInRolePipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="className">nl.nn.adapterframework.pipes.EchoPipe</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="pipe[@className='nl.nn.adapterframework.pipes.UUIDGeneratorPipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*[name()!='type']" />
			<xsl:attribute name="className">nl.nn.adapterframework.pipes.FixedResultPipe</xsl:attribute>
			<xsl:choose>
				<xsl:when test="@type='numeric'">
					<xsl:attribute name="returnString">1234567890123456789012345678901</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="returnString">0a4544b6-37489ec0_15ad0f006ae_-7ff3</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="pipe[ @className='nl.nn.adapterframework.ftp.FtpFileRetrieverPipe' 
							or @className='nl.nn.adapterframework.extensions.tibco.SendTibcoMessage' 
							or @className='nl.nn.adapterframework.ldap.LdapFindMemberPipe' 
							or @className='nl.nn.adapterframework.ldap.LdapFindGroupMembershipsPipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@name|@storeResultInSessionKey|@getInputFromSessionKey|@getInputFromFixedValue" />
			<xsl:attribute name="className">nl.nn.adapterframework.pipes.GenericMessageSendingPipe</xsl:attribute>
			<xsl:element name="sender">
				<xsl:attribute name="className">nl.nn.adapterframework.senders.IbisJavaSender</xsl:attribute>
				<xsl:attribute name="serviceName">
					<xsl:value-of select="concat('testtool-',@name)" />
				</xsl:attribute>
			</xsl:element>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="pipe[ @className='nl.nn.adapterframework.pipes.GenericMessageSendingPipe' 
							or @className='nl.nn.adapterframework.pipes.ForEachChildElementPipe']">
		<xsl:element name="pipe">
			<xsl:apply-templates select="@*" />
			<xsl:attribute name="timeOutOnResult">[timeout]</xsl:attribute>
			<xsl:attribute name="exceptionOnResult">[error]</xsl:attribute>
			<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="param/@pattern[contains(.,'{now,')]">
		<xsl:attribute name="pattern"><xsl:value-of select="replace(.,'\{now,','{fixedDate,')"/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="pipe/*[local-name()='errorStorage' or local-name()='messageLog'][@className!='nl.nn.adapterframework.jdbc.JdbcTransactionalStorage' 
																					  and @className!='nl.nn.adapterframework.jdbc.DummyTransactionalStorage']">
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
		<xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
		<xsl:copy>
			<xsl:apply-templates select="*|@*|processing-instruction()|text()" />
		</xsl:copy>
		<xsl:text disable-output-escaping="yes">--&gt;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
