<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="text" indent="no"/>
	<xsl:variable name="dbmsName" select="browseJdbcTableExecuteREQ/dbmsName"/>
	<xsl:variable name="countColumnName" select="browseJdbcTableExecuteREQ/countColumnName"/>
	<xsl:variable name="rnum" select="browseJdbcTableExecuteREQ/rnumColumnName"/>
	<xsl:variable name="tableName" select="browseJdbcTableExecuteREQ/tableName"/>
	<xsl:variable name="numberOfRowsOnly" select="browseJdbcTableExecuteREQ/numberOfRowsOnly"/>
	<xsl:variable name="where" select="browseJdbcTableExecuteREQ/where"/>
	<xsl:variable name="order" select="browseJdbcTableExecuteREQ/order"/>
	<xsl:variable name="rownumMin" select="number(browseJdbcTableExecuteREQ/rownumMin)"/>
	<xsl:variable name="rownumMax" select="number(browseJdbcTableExecuteREQ/rownumMax)"/>
	<xsl:variable name="maxColumnSize" select="number(browseJdbcTableExecuteREQ/maxColumnSize)"/>
	<xsl:template match="browseJdbcTableExecuteREQ">
		<xsl:choose>
			<xsl:when test="$numberOfRowsOnly='true'">
				<xsl:choose>
					<xsl:when test="string-length($order)&gt;0">
						<xsl:text>SELECT </xsl:text>
						<xsl:value-of select="$order"/>
						<xsl:text>, COUNT(*) AS </xsl:text>
						<xsl:value-of select="countColumnName"/>
						<xsl:text> FROM </xsl:text>
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
						<xsl:text>SELECT COUNT(*) AS </xsl:text>
						<xsl:value-of select="countColumnName"/>
						<xsl:text> FROM </xsl:text>
						<xsl:value-of select="$tableName"/>
						<xsl:if test="string-length($where)&gt;0">
							<xsl:text> WHERE </xsl:text>			
							<xsl:value-of select="$where"/>
						</xsl:if>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="not($dbmsName = 'Oracle')">
				<xsl:text>SELECT * FROM (SELECT ROW_NUMBER() OVER (ORDER BY </xsl:text>
				<xsl:choose>
					<xsl:when test="string-length($order)&gt;0">
						<xsl:value-of select="$order"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>(SELECT 1)</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:text>) AS rnum, </xsl:text>
				<xsl:call-template name="selectAllFields"/>
				<xsl:text> FROM </xsl:text>
				<xsl:value-of select="$tableName"/>
				<xsl:if test="string-length($where)&gt;0">
					<xsl:text> WHERE </xsl:text>			
					<xsl:value-of select="$where"/>
				</xsl:if>
				<xsl:text>) AS x WHERE x.rnum BETWEEN </xsl:text>			
				<xsl:value-of select="$rownumMin"/>
				<xsl:text> AND </xsl:text>			
				<xsl:value-of select="$rownumMax"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>SELECT * FROM (SELECT /*+ FIRST_ROWS(10) */ rownum rnum, x.* FROM (SELECT </xsl:text>
				<xsl:call-template name="selectAllFields"/>
				<xsl:text> FROM </xsl:text>
				<xsl:value-of select="$tableName"/>
				<xsl:if test="string-length($where)&gt;0">
					<xsl:text> WHERE </xsl:text>			
					<xsl:value-of select="$where"/>
				</xsl:if>
				<xsl:if test="string-length($order)&gt;0">
					<xsl:text> ORDER BY </xsl:text>
					<xsl:value-of select="$order"/>
				</xsl:if>
				<xsl:text>) x WHERE rownum &lt;= </xsl:text>
				<xsl:value-of select="$rownumMax"/>
				<xsl:text>) x WHERE rnum &gt;= </xsl:text>
				<xsl:value-of select="$rownumMin"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="selectAllFields">
		<xsl:choose>
			<xsl:when test="$rownumMax!=$rownumMin and fielddefinition/field[number(@size)&gt;$maxColumnSize]">
				<xsl:for-each select="fielddefinition/field">
					<xsl:if test="not(@name=$rnum)">
						<xsl:if test="number(@size)&gt;$maxColumnSize">
							<xsl:text>LENGTH(</xsl:text>
						</xsl:if>
						<xsl:value-of select="@name"/>
						<xsl:if test="number(@size)&gt;$maxColumnSize">
							<xsl:text>) AS &quot;LENGTH </xsl:text>
							<xsl:value-of select="@name"/>
							<xsl:text>&quot;</xsl:text>
						</xsl:if>
						<xsl:if test="position()!=last()">
							<xsl:text>, </xsl:text>
						</xsl:if>
					</xsl:if>
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>*</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>