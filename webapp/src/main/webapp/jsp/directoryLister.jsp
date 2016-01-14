<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/xtags-1.0" prefix="xtags" %>

<xtags:parse>
	<bean:write name="Dir2Xml" scope="request" filter="false"/>
</xtags:parse>

<br/>

<xtags:forEach select="directory">
	<xtags:variable id="sizeFormat" select="@sizeFormat"/>
	<tr><th>Name</th>
		<th>Size</th>
		<th>Date</th>
		<th>Time</th>
		<th>as</th>
	</tr>
	<xtags:forEach select="file">
		<tr alternatingRows="true">
			<xtags:variable id="canonicalName" select="@canonicalName"/>
			<td>
				<xtags:valueOf select="@name"/>
			</td>
			<td align="right">
				<xtags:if test="@directory='false'">
					<xtags:choose>
						<xtags:when test="$sizeFormat='true'">
							<xtags:valueOf select="@fSize"/>
						</xtags:when>
						<xtags:otherwise>
							<xtags:valueOf select="@size"/>
						</xtags:otherwise>					
					</xtags:choose>
				</xtags:if>
			</td>
			<td align="right">
				<xtags:valueOf select="@modificationDate"/>
			</td>
			<td align="right">
				<xtags:valueOf select="@modificationTime"/>
			</td>
			<td>
				<xtags:choose>
					<xtags:when test="@directory='true'">
						<a href="showLogging.do?directory=<%=canonicalName%>">directory</a>
					</xtags:when>
					<xtags:otherwise>
						<% if (canonicalName.indexOf("_xml.log")!=-1) { %>
							<a href="FileViewerServlet?resultType=xml&amp;fileName=<%=canonicalName%>">xml</a>
							<a href="FileViewerServlet?resultType=bin&amp;fileName=<%=canonicalName%>">bin</a>
							<a href="FileViewerServlet?resultType=html&amp;fileName=<%=canonicalName%>&amp;log4j=true">2html</a>
							<a href="FileViewerServlet?resultType=text&amp;fileName=<%=canonicalName%>&amp;log4j=true">2text</a>
						<% } else { %>
						<% if (canonicalName.indexOf("-stats_")!=-1) { %>
							<a href="FileViewerServlet?resultType=xml&amp;fileName=<%=canonicalName%>">xml</a>
							<a href="FileViewerServlet?resultType=bin&amp;fileName=<%=canonicalName%>">bin</a>
							<a href="FileViewerServlet?resultType=html&amp;fileName=<%=canonicalName%>&amp;stats=true">2html</a>
						<% } else { %>
							<a href="FileViewerServlet?resultType=html&amp;fileName=<%=canonicalName%>">html</a>
							<a href="FileViewerServlet?resultType=bin&amp;fileName=<%=canonicalName%>">bin</a>
						<% }} %>
					</xtags:otherwise>
				</xtags:choose>
			</td>
		</tr>	
	</xtags:forEach>
</xtags:forEach>
