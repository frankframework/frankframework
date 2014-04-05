<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:include href="functions.xsl"/>
	<xsl:include href="components.xsl"/>
	<xsl:template match="/">
		<html lang="nl-NL">
			<xsl:call-template name="header">
				<xsl:with-param name="css">showLogging.css</xsl:with-param>
				<xsl:with-param name="title">IJA_IPLnL 186 20131029-1413 - show Ibisstore Summary</xsl:with-param>
			</xsl:call-template>
			<body>
				<xsl:call-template name="menu">
					<xsl:with-param name="environment">TST</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="modal">
					<xsl:with-param name="version">IJA_IPLnL 186 20131029-1413, IAF 5.0-a27.3, buildscript 11g, size: 5.1</xsl:with-param>
					<xsl:with-param name="server">running on LPAB00000001894 using Apache Tomcat/7.0.22</xsl:with-param>
					<xsl:with-param name="heap">heap size: 89M, total JVM memory: 150M</xsl:with-param>
					<xsl:with-param name="space">free space: 68GB, total space: 74GB</xsl:with-param>
				</xsl:call-template>
				<div class="panel panel-primary">
					<div class="panel-heading">
						<h3 class="panel-title">Logfiles</h3>
					</div>
					<div class="panel-body">
						<form method="post" name="executeJdbcQueryForm" action="showIbisstoreSummary.do">

							<!--html:hidden property="action"/-->
							<table class="table table-bordered">
								<tr>
									<td>Select a jms realm</td>
									<td>
										<select name="jmsRealm">	
											<option name="executeJdbcQueryForm" property="jmsRealms">jdbc</option> 
										</select>
									</td>
								</tr>
								<tr>
									<td>
										<input value="cancel" class="submit" onmouseover="changeBg(this,true);" onmouseout="changeBg(this,false);" onclick="bCancel=true;" type="submit" name="org.apache.struts.taglib.html.CANCEL"/>
									</td>
									<td>
										<input value="send" class="submit" onmouseover="changeBg(this,true);" onmouseout="changeBg(this,false);" type="submit" name="send"/>
									</td>
								</tr>
							</table>
						</form>

						<table>
							<caption>Ibisstore Summary</caption>
							<tbody>
								<tr>
									<th class="table-header">Type</th>
									<th class="table-header">#</th>
									<th class="table-header">SlotID</th>
									<th class="table-header">#</th>
									<th class="table-header">Date</th>
									<th class="table-header">#</th>
								</tr>
								<xsl:for-each select="result">
									
									<xsl:for-each select="//rowset/row">
										<tr>
											<td><xsl:value-of select="field[@name='TYPE']"/></td>
											<td><xsl:value-of select="field[@name='SLOTID']"/></td>
											<td><xsl:value-of select="field[@name='MSGDATE']"/></td>
											<td><xsl:value-of select="field[@name='MSGCOUNT']"/></td>
										</tr>
									</xsl:for-each>
								
									
									<xsl:for-each select="//type">
										<tr>
											<td> <!-- rowspan="<xsl:value-of select="@datecount"/>"-->
												<xsl:value-of select="@id"/>
											</td>
									
									
											<td> <!-- rowspan="<xsl:value-of select="@datecount"/>"-->
												<xsl:value-of select="@msgcount"/>
											</td>
											<td> <!-- rowspan="<xsl:value-of select="slot[1]/@datecount"/>"-->
												<xsl:value-of select="slot[1]/@id"/>
												<xsl:if test="slot[1]/@adapter!=''">
													<xsl:variable name="type" select="@id"/>
													<img
														href="browser.do">
														<!-- <type="browse"
														<% if ( "E".equalsIgnoreCase(type) ) { %>
															alt="show contents of errorQueue"
														<% } else { %>
															alt="show contents of messageLog"
														<%}%>
														>-->
														<parameter name="storageType"><xsl:value-of select="@name"/></parameter>
														<parameter name="action">show</parameter>
														<parameter name="adapterName"><xsl:value-of select="slot[1]/@adapter"/></parameter>
														<parameter name="receiverName"><xsl:value-of select="slot[1]/@receiver"/></parameter>
														<parameter name="pipeName"><xsl:value-of select="slot[1]/@pipe"/></parameter>
														<parameter name="typeMask"><xsl:value-of select="@id"/></parameter>
													 </img>
													(<xsl:value-of select="slot[1]/@adapter"/> / <xsl:value-of select="slot[1]/@receiver"/>)
												</xsl:if>
											</td>
										
											<td> <!-- rowspan="<xsl:value-of select="slot[1]/@datecount"/>">-->
												<xsl:value-of select="slot[1]/@msgcount"/>
											</td>
											<td>
												<xsl:value-of select="slot[1]/date[1]/@id"/>
												<xsl:if test="slot[1]/@adapter!=''">
													<xsl:variable name="type" select="@id"/>
													<img
														href="browser.do">
														<!-- type="browse"
														<% if ( "E".equalsIgnoreCase(type) ) { %>
															alt="show contents of errorQueue"
														<% } else { %>
															alt="show contents of messageLog"
														<%}%>
														-->
														<parameter name="storageType"><xsl:value-of select="@name"/></parameter>
														<parameter name="action">show</parameter>
														<parameter name="adapterName"><xsl:value-of select="slot[1]/@adapter"/></parameter>
														<parameter name="receiverName"><xsl:value-of select="slot[1]/@receiver"/></parameter>
														<parameter name="pipeName"><xsl:value-of select="slot[1]/@pipe"/></parameter>
														<parameter name="typeMask"><xsl:value-of select="@id"/></parameter>
														<parameter name="insertedAfter"><xsl:value-of select="slot[1]/date[1]/@id"/></parameter>
														<parameter name="insertedAfterClip">on</parameter>
													 </img>
												</xsl:if>
											</td>
											<td>
												<xsl:value-of select="slot[1]/date[1]/@count"/>
											</td>
										</tr>
									
									
										
										<xsl:for-each select="slot[1]/date[position()>1]" >
											<tr>
												<td>
													<xsl:value-of select="@id"/>
													<xsl:if test="../@adapter!=''">
														<xsl:variable name="type" select="../../@id"/>
														<img
															href="browser.do">
															<!-- type="browse"
															<% if ( "E".equalsIgnoreCase(type) ) { %>
																alt="show contents of errorQueue"
															<% } else { %>
																alt="show contents of messageLog"
															<%}%>
															-->
															<parameter name="storageType"><xsl:value-of select="../../@name"/></parameter>
															<parameter name="action">show</parameter>
															<parameter name="adapterName"><xsl:value-of select="../@adapter"/></parameter>
															<parameter name="receiverName"><xsl:value-of select="../@receiver"/></parameter>
															<parameter name="pipeName"><xsl:value-of select="../@pipe"/></parameter>
															<parameter name="typeMask"><xsl:value-of select="../../@id"/></parameter>
															<parameter name="insertedAfter"><xsl:value-of select="@id"/></parameter>
															<parameter name="insertedAfterClip">on</parameter>
														 </img>
													</xsl:if>
												</td>
												<td>
													<xsl:value-of select="@count"/>
												</td>
											</tr>
										</xsl:for-each>
										
										<xsl:for-each select="slot[position()>1]" >
											<tr>
												<td><!-- rowspan="><xsl:value-of select="@datecount"/>"-->
													<xsl:value-of select="@id"/>
													<xsl:if test="@adapter!=''">
														<xsl:variable name="type" select="../@id"/>
														<img
															href="browser.do">
															<!-- type="browse"
															<% if ( "E".equalsIgnoreCase(type) ) { %>
																alt="show contents of errorQueue"
															<% } else { %>
																alt="show contents of messageLog"
															<%}%>
															-->
															<parameter name="storageType"><xsl:value-of select="../@name"/></parameter>
															<parameter name="action">show</parameter>
															<parameter name="adapterName"><xsl:value-of select="@adapter"/></parameter>
															<parameter name="receiverName"><xsl:value-of select="@receiver"/></parameter>
															<parameter name="pipeName"><xsl:value-of select="@pipe"/></parameter>
															<parameter name="typeMask"><xsl:value-of select="../@id"/></parameter>
														 </img>
														(<xsl:value-of select="@adapter"/> / <xsl:value-of select="@receiver"/>)
													</xsl:if>
												</td>
												<td><!--  rowspan="><xsl:value-of select="@datecount"/>"-->
													<xsl:value-of select="@msgcount"/>
												</td>
												<td>
													<xsl:value-of select="date[1]/@id"/>
													<xsl:if test="@adapter!=''">
														<xsl:variable name="type" select="../@id"/>
														<img
															href="browser.do">
															<!-- type="browse"
															<% if ( "E".equalsIgnoreCase(type) ) { %>
																alt="show contents of errorQueue"
															<% } else { %>
																alt="show contents of messageLog"
															<%}%>
															-->
															<parameter name="storageType"><xsl:value-of select="../@name"/></parameter>
															<parameter name="action">show</parameter>
															<parameter name="adapterName"><xsl:value-of select="@adapter"/></parameter>
															<parameter name="receiverName"><xsl:value-of select="@receiver"/></parameter>
															<parameter name="pipeName"><xsl:value-of select="@pipe"/></parameter>
															<parameter name="typeMask"><xsl:value-of select="../@id"/></parameter>
															<parameter name="insertedAfter"><xsl:value-of select="date[1]/@id"/></parameter>
															<parameter name="insertedAfterClip">on</parameter>
														</img>
													</xsl:if>
												</td>
												<td>
													<xsl:value-of select="date[1]/@count"/>
												</td>
											</tr>
											<xsl:for-each select="date[position()>1]" >
												<tr>
													<td>
														<xsl:value-of select="@id"/>
														<xsl:if test="../@adapter!=''">
															<xsl:variable name="type" select="../../@id"/>
															<img
																href="browser.do">
																<!-- type="browse"
																<% if ( "E".equalsIgnoreCase(type) ) { %>
																	alt="show contents of errorQueue"
																<% } else { %>
																	alt="show contents of messageLog"
																<%}%>
																-->
																<parameter name="storageType"><xsl:value-of select="../../@name"/></parameter>
																<parameter name="action">show</parameter>
																<parameter name="adapterName"><xsl:value-of select="../@adapter"/></parameter>
																<parameter name="receiverName"><xsl:value-of select="../@receiver"/></parameter>
																<parameter name="pipeName"><xsl:value-of select="../@pipe"/></parameter>
																<parameter name="typeMask"><xsl:value-of select="../../@id"/></parameter>
																<parameter name="insertedAfter"><xsl:value-of select="@id"/></parameter>
																<parameter name="insertedAfterClip">on</parameter>
															 </img>
														</xsl:if>
													</td>
													<td>
														<xsl:value-of select="@count"/>
													</td>
												</tr>
											</xsl:for-each>
										</xsl:for-each>
									</xsl:for-each>	
								</xsl:for-each>
							</tbody>
						</table>
					</div>
				</div>
				<script>
					var sd = "2013-11-07 10:02:27.320";
				</script>
				<xsl:call-template name="footer"/>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
