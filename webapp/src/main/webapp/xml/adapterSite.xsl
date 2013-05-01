

<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:html="http://WEB-INF/struts-html.tld"
	>
	
<xsl:output method="html"
		omit-xml-declaration="yes"
		indent="yes"/>
		
<xsl:include href="tableStyler.xsl"/>
<xsl:include href="blockStyler.xsl"/>



	<xsl:template match="page">
		<html>
		<head>
			<link rel="shortcut icon" href="favicon.ico"></link>
			<link href="ie4.css" type="text/css" rel="stylesheet"></link>
			<link href="body.css" type="text/css" rel="stylesheet"></link>
			<SCRIPT LANGUAGE="JavaScript" SRC="./js/functions.js"></SCRIPT>
			<title><xsl:value-of select="//applicationConstants/properties/property[@name='instance.name']"/>@<xsl:value-of select="//requestInfo/servletRequest/serverName"/><xsl:if test="//requestInfo/servletRequest/serverName!=//machineName">(<xsl:value-of select="//machineName"/>)</xsl:if>:<xsl:value-of select="@title"/></title>
			<script type="text/javascript">
				var serverDate = new Date();
				var sd = "<xsl:value-of select="//processMetrics/properties/property[@name='currentTime']"/>";
				serverDate.setFullYear(sd.substring(0,4));
				serverDate.setMonth(sd.substring(5,7)-1);
				serverDate.setDate(sd.substring(8,10));
				serverDate.setHours(sd.substring(11,13));
				serverDate.setMinutes(sd.substring(14,16));
				serverDate.setSeconds(sd.substring(17,19));
				serverDate.setMilliseconds(sd.substring(20));
				var diff = 0;

				function setDiffTime() {
					diffTime = clientDate.getTime() - serverDate.getTime();
					diff = 1;
				}

				function runClock() {
					clientDate = new Date();
					if (diff==0) {
						setDiffTime(clientDate);
					}
					serverDate.setTime(clientDate.getTime() - diffTime);
					var year = serverDate.getFullYear();
					var month = serverDate.getMonth()+1;
					if (month &lt;= 9) {
						month = "0" + month;
					}
					var day = serverDate.getDate();
					if (day &lt;= 9) {
						day = "0" + day;
					}
					var hours = serverDate.getHours();
					if (hours &lt;= 9) {
						hours = "0" + hours;
					}
					var minutes = serverDate.getMinutes();
					if (minutes &lt;= 9) {
						minutes = "0" + minutes;
					}
					var seconds = serverDate.getSeconds();
					if (seconds &lt;= 9) {
						seconds = "0" + seconds;
					}
					var dateString = "current date and time: " + year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds;
					document.getElementById("clock").innerHTML = dateString;
					setTimeout("runClock()",1000);
				}

				function confirmLink(message, url) {
					if(confirm(message)) location.href = url;
				}
			</script>
		</head>
		<body onload="runClock()">
			<xsl:attribute name="class">
				<xsl:variable name="stubConfig">
					<xsl:value-of select="//applicationConstants/properties/property[@name='stub4testtool.configuration']"/>
				</xsl:variable>
				<xsl:variable name="otapStage">
					<xsl:value-of select="//applicationConstants/properties/property[@name='otap.stage']"/>
				</xsl:variable>
				<xsl:variable name="otapSide">
					<xsl:value-of select="//applicationConstants/properties/property[@name='otap.side']"/>
				</xsl:variable>
				<xsl:choose>
					<xsl:when test="$stubConfig='true'">stub</xsl:when>
					<xsl:when test="$otapStage='DEV'">
						<xsl:choose>
							<xsl:when test="$otapSide='AS'">dev_as</xsl:when>
							<xsl:when test="$otapSide='EP'">dev_ep</xsl:when>
							<xsl:otherwise>dev</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:when test="$otapStage='TST'">
						<xsl:choose>
							<xsl:when test="$otapSide='AS'">tst_as</xsl:when>
							<xsl:when test="$otapSide='EP'">tst_ep</xsl:when>
							<xsl:otherwise>tst</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:when test="$otapStage='ACC'">
						<xsl:choose>
							<xsl:when test="$otapSide='AS'">acc_as</xsl:when>
							<xsl:when test="$otapSide='EP'">acc_ep</xsl:when>
							<xsl:otherwise>acc</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:when test="$otapStage='PRD'">
						<xsl:choose>
							<xsl:when test="$otapSide='AS'">prd_as</xsl:when>
							<xsl:when test="$otapSide='EP'">prd_ep</xsl:when>
							<xsl:otherwise>prd</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<table width="100%" class="page" >
			<tr><td colspan="3">
					<table width="100%"><tr>
					<td>
						<h1><xsl:value-of select="@title"/></h1>
					</td>
					<td align="right">
						<div class="appName">
						<xsl:value-of select="//applicationConstants/properties/property[@name='application.name']"/><xsl:value-of select="' '"/>
						<xsl:value-of select="//applicationConstants/properties/property[@name='application.version']"/>:<xsl:value-of select="' '"/>
						<xsl:value-of select="//applicationConstants/properties/property[@name='instance.name']"/><xsl:value-of select="' '"/>
						<xsl:variable name="release" select="//applicationConstants/properties/property[@name='instance.release']"/>
						<xsl:if test="string-length($release)&gt;0">
							<xsl:value-of select="$release"/><xsl:value-of select="' '"/>
						</xsl:if>
						<xsl:value-of select="//applicationConstants/properties/property[@name='instance.version']"/><xsl:value-of select="' '"/>
						<xsl:value-of select="//applicationConstants/properties/property[@name='instance.build_id']"/><xsl:value-of select="', '"/>
						buildscript <xsl:value-of select="//applicationConstants/properties/property[@name='build.xml.version']"/><xsl:value-of select="', '"/>
						size: <xsl:value-of select="//applicationConstants/properties/property[@name='instance.size']"/>
						</div>
						<div>
						running on <xsl:value-of select="//machineName"/> using <xsl:value-of select="//requestInfo/servletRequest/serverInfo"/>
						</div>
						heap size: <xsl:value-of select="//processMetrics/properties/property[@name='heapSize']"/>,
						total JVM memory: <xsl:value-of select="//processMetrics/properties/property[@name='totalMemory']"/>
						<div id="clock"/>
					</td>
					</tr></table>
				</td>

			</tr>
			<tr><td colspan="3">
				<xsl:apply-templates select="//menuBar"/>
			</td></tr>
			<tr><td colspan="3">
				<img src="images/pixel.gif" height="20px" border="0" alt=""/>
			</td></tr>
			
			<tr><td/>
				<td class="breadCrumb">
					<xsl:apply-templates select="//breadCrumb"/>
				</td>
				<td/>
			</tr>
			<tr><td width="50px"></td>
			<td class="pagePanel">
				<xsl:apply-templates select="//errors"/>
				<xsl:apply-templates select="//messages"/>
			<xsl:apply-templates />		
			</td>
			<td width="50px"></td>
			</tr>
			</table>
		</body>
		</html>
	</xsl:template>

	<xsl:template match="errors">
		<xsl:if test="error">
			<b>Errors occured</b>
		
			<xsl:for-each select="error">
			<li class="clsError"> <xsl:value-of select="."/></li>
			</xsl:for-each>
			<br/><br/>
		</xsl:if>
	</xsl:template>
	<xsl:template match="messages">
		
		<xsl:if test="message">
			<b>Messages:</b>
		
			<xsl:for-each select="message">
			<li><xsl:value-of select="."/></li>
			</xsl:for-each>
			<br/><br/>
		</xsl:if>
	</xsl:template>
	<xsl:template match="breadCrumb">
		<xsl:for-each select="breadCrumbItem">
			<a href="{@href}"><xsl:value-of select="."/></a>
			<xsl:if test="position()!=last()">&#160;&gt;&#160;</xsl:if>
		</xsl:for-each>
	</xsl:template>

	<!-- omit requestInfo in transformation -->
	<xsl:template match="requestInfo"/>
	
	<xsl:template match="abstractPage">
		
		<xsl:apply-templates select="page"/>
	</xsl:template>

	<xsl:template match="form">
		<xsl:choose>
		<xsl:when test="@focus">
			<xsl:apply-templates select="text()|@*|./*"/>
			<xsl:copy>
				<script language="JavaScript" type="text/javascript">
					if (document.forms["{@name}"].elements["{@focus}"].type != "hidden") 
					document.forms["{@name}"].elements["{@focus}"].focus();
					</script>
			</xsl:copy>
		</xsl:when>
		<xsl:otherwise>
				<!-- select first non-hidden field -->
				<xsl:variable name="firstField">
					<xsl:value-of select="descendant::input[@type!='hidden']/@name"/>
				</xsl:variable>
				<xsl:copy>
				<xsl:apply-templates select="text()|@*|./*"/>
				</xsl:copy>
				<xsl:if test="string-length($firstField)>0">
				<script language="JavaScript" type="text/javascript">
				 if (document.forms["<xsl:value-of select="@name"/>"].elements["<xsl:value-of select="$firstField"/>"].type != "hidden") 
				document.forms["<xsl:value-of select="@name"/>"].elements["<xsl:value-of select="$firstField"/>"].focus();
				</script>
				</xsl:if>


		</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- render an input element -->
	<xsl:template match="input">
		<xsl:choose>
		<xsl:when test="@type='hidden' or @type='HIDDEN'">
			<xsl:copy>
							<xsl:apply-templates select="@*|*|text()"/>
			</xsl:copy>
		</xsl:when>
		<xsl:when test="@type='submit' or 
		 			    @type='reset' or 
						@type='cancel' or
						@type='button'">
			<xsl:copy>
				<xsl:attribute name="class"><xsl:value-of select="@type"/></xsl:attribute>
				<xsl:if test="string-length(@name)=0">
					<xsl:attribute name="name"><xsl:value-of select="@value"/></xsl:attribute>
				</xsl:if>
				<xsl:attribute name="onmouseover">changeBg(this,true);</xsl:attribute>
				<xsl:attribute name="onmouseout">changeBg(this,false);</xsl:attribute>
				<xsl:apply-templates select="@*|*|text()"/>
			</xsl:copy>
		</xsl:when>
		<xsl:when test="@type='checkbox' or
						@type='radio'">
			<xsl:copy>
				<xsl:attribute name="class"><xsl:value-of select="@type"/></xsl:attribute>
				<xsl:apply-templates select="@*|*|text()"/>
			</xsl:copy>
				
		</xsl:when>
		<xsl:otherwise>
			<xsl:copy>
				<xsl:attribute name="class"><xsl:value-of select="@type"/></xsl:attribute>
				<xsl:apply-templates select="@*|*|text()"/>
			</xsl:copy>
		</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="textarea">
		<xsl:copy>
			<xsl:attribute name="class">normal</xsl:attribute>
			<xsl:apply-templates select="@*|*|text()"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="select">
		<xsl:copy>
			<xsl:attribute name="class">normal</xsl:attribute>
			<xsl:apply-templates select="@*|*|text()"/>
		</xsl:copy>
	</xsl:template>
	
	
	<xsl:template match="booleanImage">
		<xsl:choose>
		<xsl:when test="@value='true' or @value='True'">
			<img src="images/check.gif" border="0"/>	
		</xsl:when>
		<xsl:when test="@value='false' or @value='False'">
			<img src="images/no.gif" border="0"/>	
		</xsl:when>
		</xsl:choose>

	</xsl:template>
	
	<xsl:template match="imagelinkMenu">

		<xsl:for-each select="imagelink">
			<xsl:apply-templates select="."/>
		</xsl:for-each>
		
	</xsl:template>
	
	<xsl:template match="imagelink">
		<!-- determine significant parent -->
		<xsl:variable name="parent">
			<xsl:value-of select="ancestor::tr/@ref"/>
		</xsl:variable>
		<xsl:element name="a">
			<!-- define class -->
			<xsl:attribute name="class">
			<xsl:choose>
				<xsl:when test="../../../menuBar">utilmenu</xsl:when>
				<xsl:when test="$parent='triggerRow'">triggeractionmenu</xsl:when>
				<xsl:when test="$parent='jobRow'">jobactionmenu</xsl:when>
				<xsl:otherwise>unknown</xsl:otherwise>
			</xsl:choose>
			</xsl:attribute>
	
			<!-- create href -->
			
			<xsl:attribute name="href">
						<xsl:value-of select="@href"/>
						<xsl:for-each select="parameter">
							<xsl:choose>
								<xsl:when test="position()=1">?</xsl:when>
								<xsl:otherwise>&amp;</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="@name"/>=<xsl:value-of select="."/>
						</xsl:for-each>
			</xsl:attribute>
			<xsl:if test="@newwindow">
				<xsl:attribute name="target">_blank</xsl:attribute>
			</xsl:if>
	
			<xsl:call-template name="buildIcon" />
		</xsl:element>
	</xsl:template>

	<xsl:template name="determineImage">
		<xsl:param name="type"/>
		<xsl:choose>
			<xsl:when test="@type='scheduler'"			>images/clock.gif</xsl:when>
			<xsl:when test="@type='pause'"				>images/pause.gif</xsl:when>
			<xsl:when test="@type='delete'"				>images/delete.gif</xsl:when>
			<xsl:when test="@type='wsdl'"				>images/wsdl.gif</xsl:when>
			<xsl:when test="@type='service'"			>images/service.gif</xsl:when>
			<xsl:when test="@type='logging'"			>images/logging.gif</xsl:when>
			<xsl:when test="@type='testPipeLine'"		>images/testPipeLine.gif</xsl:when>
			<xsl:when test="@type='browse'"				>images/browse.gif</xsl:when>
			<xsl:when test="@type='browseErrorStore'"	>images/browseErrorStore.gif</xsl:when>
			<xsl:when test="@type='browseMessageLog'"	>images/browseMessageLog.gif</xsl:when>
			<xsl:when test="@type='browsejms'"			>images/browsejms.gif</xsl:when>
			<xsl:when test="@type='message'"			>images/message.gif</xsl:when>
			<xsl:when test="@type='resend'"				>images/resend.gif</xsl:when>
			<xsl:when test="@type='jms-message'"		>images/jmsmessage.gif</xsl:when>
			<xsl:when test="@type='ifsa-message'"		>images/interface.gif</xsl:when>
			<xsl:when test="@type='properties'"			>images/config.gif</xsl:when>
			<xsl:when test="@type='security'"			>images/security.gif</xsl:when>
			<xsl:when test="@type='execquery'"			>images/execquery.gif</xsl:when>
			<xsl:when test="@type='browsetable'"		>images/table.gif</xsl:when>
			<xsl:when test="@type='dump'"				>images/dump.gif</xsl:when>
			<xsl:when test="@type='edit'"				>images/edit.gif</xsl:when>
			<xsl:when test="@type='help'"				>images/help.gif</xsl:when>
			<xsl:when test="@type='testtool'"			>images/testtool.gif</xsl:when>
			<xsl:when test="@type='showsummary'"		>images/ibisstore.gif</xsl:when>

			<xsl:when test="@type='configurationStatus'">images/configurationStatus.gif</xsl:when>

			<xsl:when test="@type='monitoring'"			>images/monitoring.gif</xsl:when>
			<xsl:when test="@type='configuration'"		>images/configuration.gif</xsl:when>
			<xsl:when test="@type='adapterStatistics'"	>images/statistics.gif</xsl:when>
			<xsl:when test="@type='tracingConfiguration'">images/trc.gif</xsl:when>


			<xsl:when test="@type='showashtml'"			>images/showashtml.gif</xsl:when>
			<xsl:when test="@type='showastext'"			>images/showastext.gif</xsl:when>


			<xsl:when test="@type='start'"				>images/start.gif</xsl:when>
			<xsl:when test="@type='pause'"				>images/pause.gif</xsl:when>
			<xsl:when test="@type='stop'"				>images/stop.gif</xsl:when>
			<xsl:when test="@type='flow'"				>images/flow.gif</xsl:when>
		</xsl:choose>
	</xsl:template>

	
	<xsl:template name="buildIcon">
		<xsl:variable name="src">
			<xsl:call-template name="determineImage"> 
				<xsl:with-param name="type" select="@type"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$src!=''">
				<xsl:element name="img">
					<xsl:attribute name="src"><xsl:value-of select="$src"/></xsl:attribute>
					<xsl:attribute name="align">middle</xsl:attribute>
					<xsl:attribute name="hspace">2</xsl:attribute>
					<xsl:attribute name="vspace">2</xsl:attribute>
					<xsl:attribute name="border">0</xsl:attribute>
					<xsl:choose>
						<xsl:when test="@alt">
							<xsl:attribute name="alt"><xsl:value-of select="@alt"/></xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="alt"></xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>					
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="@text"/>
					<xsl:when test="text"/>
					<xsl:otherwise>
						<xsl:value-of select="@alt"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="@text"><xsl:value-of select="@text"/></xsl:if>
		<xsl:if test="text"><xsl:value-of select="text"/></xsl:if>
	</xsl:template>

	<xsl:template match="menuBar">
		<div class="tabBar">
			<table width="100%"><tr><td>
			<xsl:apply-templates select="imagelinkMenu"/>
			</td></tr></table>
		</div>
	</xsl:template>
	
	<xsl:template match="*|@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	

	

	
</xsl:stylesheet>

