<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

    <xsl:param name="tnumber"/>
    <xsl:param name="slotid"/>

	<xsl:template match="/">
		<manageDatabaseREQ>
            <select>
                <tableName>IBISTEMP</tableName>
                <columns>
                    <column>
                        <name>tchar</name>
                    </column>
                    <column>
                        <name>tclob</name>
                    </column>
                    <column>
                        <name>tnumber</name>
                    </column>
                    <column>
                        <name>tvarchar</name>
                    </column>
                </columns>
                <where>tnumber=<xsl:value-of select="$tnumber"/> and tchar!='1' and tchar!='8'</where> <!-- avoid 'inProcess' records -->
                <order>tkey</order>
            </select>
            <select>
                <tableName>IBISSTORE</tableName>
                <columns>
                    <column>
                        <name>type</name>
                    </column>
                    <column>
                        <name>slotid</name>
                    </column>
                </columns>
                <where>slotid='<xsl:value-of select="$slotid"/>'</where>
                <order>MESSAGEKEY</order>
            </select>
        </manageDatabaseREQ>
	</xsl:template>
</xsl:stylesheet>
