<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" indent="no"/>
	<xsl:variable name="tableName" select="browseJdbcTableExecuteREQ/tableName"/>
	<xsl:variable name="numberOfRowsOnly" select="browseJdbcTableExecuteREQ/numberOfRowsOnly"/>
	<xsl:variable name="where" select="browseJdbcTableExecuteREQ/where"/>
	<xsl:variable name="order" select="browseJdbcTableExecuteREQ/order"/>
	<xsl:variable name="rownumMin" select="number(browseJdbcTableExecuteREQ/rownumMin)"/>
	<xsl:variable name="rownumMax" select="number(browseJdbcTableExecuteREQ/rownumMax)"/>
	<xsl:template match="browseJdbcTableExecuteREQ">
		<xsl:choose>
			<xsl:when test="$numberOfRowsOnly='true'">
				<xsl:choose>
					<xsl:when test="string-length($order)&gt;0">
						<xsl:text>SELECT </xsl:text>
						<xsl:value-of select="$order"/>
						<xsl:text>, COUNT(*) AS ROWCOUNT FROM </xsl:text>
						<xsl:value-of select="$tableName"/>
						
						<xsl:if test="string-length($where)&gt;0">
							<xsl:text> WHERE </xsl:text>			
								<xsl:value-of select="$where"/>
						</xsl:if>
												
						<xsl:text> GROUP BY </xsl:text>
						<xsl:value-of select="$order"/>
						<xsl:text> ORDER BY </xsl:text>
						<xsl:value-of select="$order"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>SELECT COUNT(*) AS ROWCOUNT FROM </xsl:text>
						<xsl:value-of select="$tableName"/>
						<xsl:if test="string-length($where)&gt;0">
							<xsl:text> WHERE </xsl:text>			
								<xsl:value-of select="$where"/>
						</xsl:if>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="$rownumMin&gt;0 or $rownumMax&gt;0">
					<xsl:text>SELECT * FROM (</xsl:text>					
				</xsl:if>
				<xsl:choose>
					<xsl:when test="string-length($order)&gt;0">
						<xsl:text>SELECT ROW_NUMBER() OVER (ORDER BY </xsl:text>
						<xsl:value-of select="$order"/>
						<xsl:text>) AS ROWNUMBER, </xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>SELECT ROWNUM AS ROWNUMBER, </xsl:text>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:choose>
					<xsl:when test="$rownumMax!=$rownumMin and result/fielddefinition/field[@type='BLOB' or @type='CLOB']">
						<xsl:for-each select="result/fielddefinition/field">
							<xsl:if test="@type='BLOB' or @type='CLOB'">
								<xsl:text>LENGTH(</xsl:text>
							</xsl:if>
							<xsl:value-of select="$tableName"/>
							<xsl:text>.</xsl:text>
							<xsl:value-of select="@name"/>
							<xsl:if test="@type='BLOB' or @type='CLOB'">
								<xsl:text>) AS &quot;LENGTH </xsl:text>
								<xsl:value-of select="@name"/>
								<xsl:text>&quot;</xsl:text>
							</xsl:if>
							<xsl:if test="position()!=last()">
								<xsl:text>, </xsl:text>
							</xsl:if>
						</xsl:for-each>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$tableName"/>
						<xsl:text>.*</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:text> FROM </xsl:text>
				<xsl:value-of select="$tableName"/>
				
					<xsl:if test="string-length($where)&gt;0">
						<xsl:text> WHERE </xsl:text>			
							<xsl:value-of select="$where"/>
					</xsl:if>
				
				<xsl:if test="$rownumMin&gt;0 or $rownumMax&gt;0">
					<xsl:text>)</xsl:text>
					<xsl:choose>
						<xsl:when test="$rownumMin&gt;0 and $rownumMax&gt;0">
							<xsl:text> WHERE ROWNUMBER BETWEEN </xsl:text>
							<xsl:value-of select="$rownumMin"/>
							<xsl:text> AND </xsl:text>
							<xsl:value-of select="$rownumMax"/>
						</xsl:when>
						<xsl:when test="$rownumMin&gt;0">
							<xsl:text> WHERE ROWNUMBER &gt;= </xsl:text>
							<xsl:value-of select="$rownumMin"/>
						</xsl:when>
						<xsl:when test="$rownumMax&gt;0">
							<xsl:text> WHERE ROWNUMBER &lt;= </xsl:text>
							<xsl:value-of select="$rownumMax"/>
						</xsl:when>
					</xsl:choose>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>