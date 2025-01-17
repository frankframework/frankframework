<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="text" indent="no" />
	<!--
		This XSLT transforms an adapter in the Frank!Framework configuration to a flowchart in dot format:
		- each flow starts with a START element and ends with an END element (both in an ellipse)
		- every pipe in the flow is represented by a box; excepting XmlSwitch, CompareIntegerPipe, CompareStringPipe, FilenameSwitch and XmlIf which are represented by a diamond
		- for every forward a line (with arrowhead) is drawn between the regarding elements; if the pipe attribute notFoundForwardName, emptyForwardName, thenForwardName or elseForwardName exists and has no corresponding forward then a forward is assumed
		- for every exit a box with rounded corners is created
		- if the pipeline has an inputValidator element then an ENTER element (in a box with rounded corners) is created (after the START element); the inputValidator is represented by a dashed box; between the ENTER element and the inputValidator a dashed line without arrowhead is drawn; for the inputValidator forwards a dashed line is used instead of the default solid line
		- if the pipeline has an outputValidator element then this one is represented by a dashed box; between every exit element and the outputValidator a dashed line without arrowhead is drawn; for the outputValidators forwards a dashed line is used instead of the default solid line
		- for every pipe input/outputValidator a dashed box is created; between the pipe element and the input/outputValidator a dashed line without arrowhead is drawn; for the input/outputValidator forwards a dashed line is used instead of the default solid line
		- if a pipe isn't found in a forward (and not in the pipeline attribute firstPipe and not in the pipe attribute notFoundForwardName, emptyForwardName, thenForwardName or elseForwardName) then the first XmlSwitch pipe before it without forwards is assumed to be its caller; between the elements a dotted line is used instead of the default solid line
		- if a pipe (not XmlSwitch, CompareIntegerPipe, CompareStringPipe or XmlIf) has no forward then the next pipe in the sequence is assumed to be the 'success' forward; for the last pipe in the sequence the exit with attribute state 'success' is assumed to be the 'success' forward
	-->
	<xsl:variable name="space" select="' '" />
	<xsl:template match="adapter">
		<xsl:text>digraph</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>{</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:text>node [shape=box]</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:apply-templates select="pipeline" />
		<xsl:text>}</xsl:text>
	</xsl:template>
	<xsl:template match="pipeline">
		<xsl:text>START [shape=ellipse]</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:choose>
			<xsl:when test="inputValidator">
				<xsl:text>ENTER [style=rounded]</xsl:text>
				<xsl:text>&#10;</xsl:text>
				<xsl:text>START -&gt; ENTER -&gt;</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>START -&gt;</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="$space" />
		<xsl:text>&quot;</xsl:text>
		<xsl:choose>
			<xsl:when test="string-length(@firstPipe) > 0">
				<xsl:value-of select="@firstPipe" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="pipe[1]/@name" />
			</xsl:otherwise>
		</xsl:choose>
		<xsl:text>&quot;</xsl:text>
		<xsl:text>;</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:apply-templates select="pipe" />
		<xsl:apply-templates select="exits/exit" />
		<xsl:if test="inputValidator">
			<xsl:call-template name="inputValidator">
				<xsl:with-param name="caller">ENTER</xsl:with-param>
			</xsl:call-template>
		</xsl:if>
		<xsl:if test="outputValidator">
			<xsl:call-template name="outputValidator" />
		</xsl:if>
	</xsl:template>
	<xsl:template match="pipe">
		<xsl:variable name="pos" select="position()" />
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="@name" />
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>[label=</xsl:text>
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="@name" />
		<xsl:text>\n(</xsl:text>
		<xsl:choose>
			<xsl:when test="@className='org.frankframework.pipes.SenderPipe'">
				<xsl:call-template name="lastIndexOf">
					<xsl:with-param name="string" select="sender/@className" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="lastIndexOf">
					<xsl:with-param name="string" select="@className" />
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:text>)</xsl:text>
		<xsl:text>&quot;</xsl:text>
		<xsl:if test="@className='org.frankframework.pipes.SenderPipe'">
			<xsl:text>, shape=parallelogram</xsl:text>
		</xsl:if>
		<xsl:text>]</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:apply-templates select="forward">
			<xsl:with-param name="pipeName" select="@name" />
		</xsl:apply-templates>
		<xsl:choose>
			<xsl:when test="@className='org.frankframework.pipes.XmlSwitch'
							or@className='org.frankframework.pipes.CompareIntegerPipe'
							or@className='org.frankframework.pipes.CompareStringPipe'
							or@className='org.frankframework.pipes.XmlIf'">
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="@name" />
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="$space" />
				<xsl:text>[shape=diamond]</xsl:text>
				<xsl:text>&#10;</xsl:text>
				<xsl:variable name="notFoundForwardName" select="@notFoundForwardName" />
				<xsl:if test="string-length($notFoundForwardName)&gt;0 and (forward/@name=$notFoundForwardName)=false()">
					<xsl:call-template name="forward">
						<xsl:with-param name="pipeName" select="@name" />
						<xsl:with-param name="forwardPath" select="$notFoundForwardName" />
						<xsl:with-param name="forwardName">notFoundForwardName</xsl:with-param>
					</xsl:call-template>
				</xsl:if>
				<xsl:variable name="emptyForwardName" select="@emptyForwardName" />
				<xsl:if test="string-length($emptyForwardName)&gt;0 and (forward/@name=$emptyForwardName)=false()">
					<xsl:call-template name="forward">
						<xsl:with-param name="pipeName" select="@name" />
						<xsl:with-param name="forwardPath" select="$emptyForwardName" />
						<xsl:with-param name="forwardName">emptyForwardName</xsl:with-param>
					</xsl:call-template>
				</xsl:if>
				<xsl:variable name="thenForwardName" select="@thenForwardName" />
				<xsl:if test="string-length($thenForwardName)&gt;0 and (forward/@name=$thenForwardName)=false()">
					<xsl:call-template name="forward">
						<xsl:with-param name="pipeName" select="@name" />
						<xsl:with-param name="forwardPath" select="$thenForwardName" />
						<xsl:with-param name="forwardName">thenForwardName</xsl:with-param>
					</xsl:call-template>
				</xsl:if>
				<xsl:variable name="elseForwardName" select="@elseForwardName" />
				<xsl:if test="string-length($elseForwardName)&gt;0 and (forward/@name=$elseForwardName)=false()">
					<xsl:call-template name="forward">
						<xsl:with-param name="pipeName" select="@name" />
						<xsl:with-param name="forwardPath" select="$elseForwardName" />
						<xsl:with-param name="forwardName">elseForwardName</xsl:with-param>
					</xsl:call-template>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:if test="not(forward[@name='success'])">
					<xsl:call-template name="forward">
						<xsl:with-param name="pipeName" select="@name" />
						<xsl:with-param name="forwardPath">
							<xsl:choose>
								<xsl:when test="count(parent::*[name()='pipeline']/pipe)&gt;$pos">
									<xsl:value-of select="parent::*[name()='pipeline']/pipe[$pos+1]/@name" />
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="parent::*[name()='pipeline']/exits/exit[@state='success']/@name" />
								</xsl:otherwise>
							</xsl:choose>
						</xsl:with-param>
						<xsl:with-param name="forwardName">success</xsl:with-param>
					</xsl:call-template>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="inputValidator">
			<xsl:call-template name="inputValidator">
				<xsl:with-param name="caller" select="@name" />
			</xsl:call-template>
		</xsl:if>
		<xsl:if test="outputValidator">
			<xsl:call-template name="outputValidator">
				<xsl:with-param name="caller" select="@name" />
			</xsl:call-template>
		</xsl:if>
		<xsl:variable name="pipeName" select="@name" />
		<xsl:if test="(parent::*[name()='pipeline']/@firstPipe=$pipeName)=false() and (parent::*[name()='pipeline']/pipe/forward/@path=$pipeName)=false() and (parent::*[name()='pipeline']/pipe/@notFoundForwardName=$pipeName)=false() and (parent::*[name()='pipeline']/pipe/@emptyForwardName=$pipeName)=false() and (parent::*[name()='pipeline']/pipe/@thenForwardName=$pipeName)=false() and (parent::*[name()='pipeline']/pipe/@elseForwardName=$pipeName)=false()">
			<xsl:for-each select="parent::*[name()='pipeline']/pipe[position()&lt;$pos and @className='org.frankframework.pipes.XmlSwitch' and forward=false()]">
				<xsl:if test="position()=last()">
					<xsl:call-template name="forward">
						<xsl:with-param name="pipeName" select="@name" />
						<xsl:with-param name="forwardPath" select="$pipeName" />
						<xsl:with-param name="forwardName">
							<xsl:value-of select="$pipeName" />
						</xsl:with-param>
						<xsl:with-param name="style">dotted</xsl:with-param>
					</xsl:call-template>
				</xsl:if>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>
	<xsl:template match="forward" name="forward">
		<xsl:param name="pipeName" />
		<xsl:param name="forwardPath" />
		<xsl:param name="forwardName" />
		<xsl:param name="style" select="''"/>
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="$pipeName" />
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>-&gt;</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>&quot;</xsl:text>
		<xsl:choose>
			<xsl:when test="string-length($forwardPath)=0">
				<xsl:value-of select="@path" />
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="$space" />
				<xsl:text>[label=&quot;</xsl:text>
				<xsl:value-of select="@name" />
				<xsl:text>&quot;</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$forwardPath" />
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="$space" />
				<xsl:text>[label=&quot;</xsl:text>
				<xsl:value-of select="$forwardName" />
				<xsl:text>&quot;</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="string-length($style)&gt;0">
			<xsl:text>,style=</xsl:text>
			<xsl:value-of select="$style" />
		</xsl:if>
		<xsl:text>];</xsl:text>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>
	<xsl:template match="exit">
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="@name" />
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>[style=rounded]</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="@name" />
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>-&gt;</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>&quot;</xsl:text>
		<xsl:text>END</xsl:text>
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>[label=&quot;</xsl:text>
		<xsl:value-of select="@state" />
		<xsl:text>&quot;];</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:text>END [shape=ellipse]</xsl:text>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>
	<xsl:template name="inputValidator">
		<xsl:param name="caller" />
		<xsl:variable name="iv" select="generate-id(inputValidator)" />
		<xsl:value-of select="$iv" />
		<xsl:value-of select="$space" />
		<xsl:text>[label=inputValidator,style=dashed]</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="$caller" />
		<xsl:text>&quot;</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:text>-&gt;</xsl:text>
		<xsl:value-of select="$space" />
		<xsl:value-of select="$iv" />
		<xsl:value-of select="$space" />
		<xsl:text>[arrowhead=none,constraint=false,style=dashed]</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:apply-templates select="inputValidator/forward">
			<xsl:with-param name="pipeName" select="$iv" />
			<xsl:with-param name="style">dashed</xsl:with-param>
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template name="outputValidator">
		<xsl:param name="caller" select="''"/>
		<xsl:variable name="ov" select="generate-id(outputValidator)" />
		<xsl:value-of select="$ov" />
		<xsl:value-of select="$space" />
		<xsl:text>[label=outputValidator,style=dashed]</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<xsl:choose>
			<xsl:when test="string-length($caller)=0">
				<xsl:for-each select="exits/exit">
					<xsl:value-of select="@name" />
					<xsl:value-of select="$space" />
					<xsl:text>-&gt;</xsl:text>
					<xsl:value-of select="$space" />
					<xsl:value-of select="$ov" />
					<xsl:value-of select="$space" />
					<xsl:text>[arrowhead=none,constraint=false,style=dashed]</xsl:text>
					<xsl:text>&#10;</xsl:text>
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="$caller" />
				<xsl:text>&quot;</xsl:text>
				<xsl:value-of select="$space" />
				<xsl:text>-&gt;</xsl:text>
				<xsl:value-of select="$space" />
				<xsl:value-of select="$ov" />
				<xsl:value-of select="$space" />
				<xsl:text>[arrowhead=none,constraint=false,style=dashed]</xsl:text>
				<xsl:text>&#10;</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:apply-templates select="outputValidator/forward">
			<xsl:with-param name="pipeName" select="$ov" />
			<xsl:with-param name="style">dashed</xsl:with-param>
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template name="lastIndexOf">
		<xsl:param name="string" />
		<xsl:variable name="char">.</xsl:variable>
		<xsl:choose>
			<xsl:when test="contains($string, $char)">
				<xsl:call-template name="lastIndexOf">
					<xsl:with-param name="string" select="substring-after($string, $char)" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$string" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
