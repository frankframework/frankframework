<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
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
		- stub the pipe element GetPrincipalPipe by a pipe element FixedResult with attribute returnString set to tst9
		- stub the pipe element IsUserInRolePipe by a pipe element EchoPipe
		- stub the pipe element UUIDGeneratorPipe by a pipe element FixedResult with attribute returnString set to 1234567890123456789012345678901 if type='numeric' and 0a4544b6-37489ec0_15ad0f006ae_-7ff3 otherwise
		- stub the pipe element FtpFileRetrieverPipe, LdapFindMemberPipe, LdapFindGroupMembershipsPipe and SendTibcoMessage by a pipe element GenericMessageSendingPipe (and copy the attributes name, storeResultInSessionKey, getInputFromSessionKey and getInputFromFixedValue) with a child Ibis4JavaSender (serviceName="testtool-[pipe name]")
		- add the attribute timeOutOnResult with value '[timeout]' and attribute exceptionOnResult with value '[error]' to all pipe elements GenericMessageSendingPipe and ForEachChildElementPipe
		- add, if not available, the parameter destination with value 'P2P.Infrastructure.Ibis4TestTool.Stub.Request/Action' to all pipe and inputWrapper elements SoapWrapperPipe with attribute direction=wrap 
		- add, if not available, the parameter destination with value 'P2P.Infrastructure.Ibis4TestTool.Stub.Response' to all outputWrapper elements SoapWrapperPipe with attribute direction=wrap 
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
					<xsl:when test="listener[@className='nl.nn.adapterframework.http.RestListener']">
						<xsl:call-template name="copy" />
					</xsl:when>
					<xsl:when test="listener[@className='nl.nn.adapterframework.jdbc.MessageStoreListener']">
						<xsl:call-template name="copy" />
					</xsl:when>
					<xsl:when test="listener[@className='nl.nn.adapterframework.http.rest.ApiListener']">
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
					<xsl:for-each select="parent::*[name()='adapter']/receiver/errorStorage[@className='nl.nn.adapterframework.jdbc.JdbcTransactionalStorage' or @className='nl.nn.adapterframework.jdbc.DummyTransactionalStorage']">
						<xsl:if test="position()=1">
							<xsl:copy-of select="." />
						</xsl:if>
					</xsl:for-each>
					<xsl:for-each select="parent::*[name()='adapter']/receiver/errorSender[@className='nl.nn.adapterframework.senders.IbisLocalSender']">
						<xsl:if test="position()=1">
							<xsl:copy-of select="." />
						</xsl:if>
					</xsl:for-each>
					<xsl:for-each select="parent::*[name()='adapter']/receiver/messageLog[@className='nl.nn.adapterframework.jdbc.JdbcTransactionalStorage' or @className='nl.nn.adapterframework.jdbc.DummyTransactionalStorage']">
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
							<xsl:when test="@className='nl.nn.adapterframework.senders.JavascriptSender'">
								<xsl:call-template name="copy" /> 
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.jdbc.MessageStoreSender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.senders.ReloadSender'">
								<xsl:call-template name="copy" />
							</xsl:when>
							<xsl:when test="@className='nl.nn.adapterframework.compression.ZipWriterSender'">
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
									<xsl:if test="string-length(@multipartResponse)&gt;0">
										<xsl:attribute name="multipartResponse">
											<xsl:value-of select="@multipartResponse" />
										</xsl:attribute>
									</xsl:if>
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
			<xsl:when test="name()='pipe' and @className='nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe'">
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
			</xsl:when>
			<xsl:when test="name()='inputWrapper' and @className='nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe'">
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
			</xsl:when>
			<xsl:when test="name()='outputWrapper' and @className='nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe'">
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
			</xsl:when>
			<xsl:when test="name()='pipe' and @className='nl.nn.adapterframework.pipes.GetPrincipalPipe'">
				<xsl:element name="pipe">
					<xsl:apply-templates select="@*" />
					<xsl:attribute name="className">nl.nn.adapterframework.pipes.FixedResult</xsl:attribute>
					<xsl:attribute name="returnString">tst9</xsl:attribute>
					<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
				</xsl:element>
			</xsl:when>
			<xsl:when test="name()='pipe' and @className='nl.nn.adapterframework.pipes.IsUserInRolePipe'">
				<xsl:element name="pipe">
					<xsl:apply-templates select="@*" />
					<xsl:attribute name="className">nl.nn.adapterframework.pipes.EchoPipe</xsl:attribute>
					<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
				</xsl:element>
			</xsl:when>
			<xsl:when test="name()='pipe' and @className='nl.nn.adapterframework.pipes.UUIDGeneratorPipe'">
				<xsl:element name="pipe">
					<xsl:apply-templates select="@*[name()!='type']" />
					<xsl:attribute name="className">nl.nn.adapterframework.pipes.FixedResult</xsl:attribute>
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
			</xsl:when>
			<xsl:when test="name()='pipe' and (@className='nl.nn.adapterframework.ftp.FtpFileRetrieverPipe' or @className='nl.nn.adapterframework.extensions.tibco.SendTibcoMessage' or @className='nl.nn.adapterframework.ldap.LdapFindMemberPipe' or @className='nl.nn.adapterframework.ldap.LdapFindGroupMembershipsPipe')">
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
			<xsl:when test="name()='param' and contains(@pattern,'{now,')">
				<xsl:element name="param">
					<xsl:apply-templates select="@*[(name()='pattern')=false()]" />
					<xsl:attribute name="pattern">
						<xsl:value-of select="replace(@pattern,'\{now,','{fixedDate,')" />
					</xsl:attribute>
					<xsl:apply-templates select="*|comment()|processing-instruction()|text()" />
				</xsl:element>
			</xsl:when>
			<xsl:when test="(name()='errorStorage' or name()='messageLog') and parent::*[name()='pipe'] and (@className='nl.nn.adapterframework.jdbc.JdbcTransactionalStorage')=false() and (@className='nl.nn.adapterframework.jdbc.DummyTransactionalStorage')=false()">
				<xsl:call-template name="disable" />
			</xsl:when>
			<xsl:when test="$disableValidators=true and (name()='inputValidator' or name()='outputValidator')">
				<xsl:call-template name="disable" />
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
