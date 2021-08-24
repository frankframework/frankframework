<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:exslt="http://exslt.org/common" xmlns:frank="https://wearefrank.nl/" exclude-result-prefixes="exslt" version="2.0">
	<xsl:output method="xml" indent="no"/>

	<xsl:param name="adapterToId">
		<xsl:for-each select="//adapter">
			<adapter name="{@name}" id="{generate-id(.)}"/>
		</xsl:for-each>
	</xsl:param>
	<xsl:param name="all" select="."/>

	<xsl:template match="/">
		<xsl:text>graph</xsl:text>
		<xsl:text>&#10;</xsl:text>

		<xsl:variable name="initialResult">
			<xsl:apply-templates select="*"/>
		</xsl:variable>

		<xsl:copy-of select="$initialResult"/>

		<xsl:for-each-group select="$initialResult/element" group-by="substring-before(identification/text(), '|')">
			<xsl:variable name="adapterName" select="$adapterToId/adapter[@id = current-grouping-key()]/@name"/>
<!--			<groupingKeyTest><xsl:value-of select="current-grouping-key()"/></groupingKeyTest>-->
			<xsl:text>style </xsl:text>
			<xsl:value-of select="$adapterName"/>
			<xsl:text disable-output-escaping="yes"> fill:#0000,stroke:#AAAAFF,stroke-width:2px&#10;	subgraph </xsl:text>
			<xsl:value-of select="$adapterName"/>
			<xsl:text disable-output-escaping="yes">&#10;</xsl:text>
			<xsl:for-each-group select="current-group()" group-by="identification/text()">
				<xsl:choose>
					<xsl:when test="current-group()[@isValidationFlow=false()]">
						<xsl:value-of select="current-group()[@isValidationFlow=false()][1]/value" disable-output-escaping="yes"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="value" disable-output-escaping="yes"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each-group>
			<xsl:text>	end</xsl:text>
			<xsl:text disable-output-escaping="yes">&#10;</xsl:text>
		</xsl:for-each-group>
	</xsl:template>

	<xsl:template match="adapter">
		<xsl:variable name="adapterName" select="@name"/>
		<xsl:variable name="id" select="$adapterToId/adapter[@name = $adapterName]/@id"/>
		<xsl:variable name="inputValidatorId" select="generate-id(pipeline/inputValidator)"/>
		<xsl:variable name="outputValidatorId" select="generate-id(pipeline/outputValidator)"/>
		<xsl:variable name="firstPipe">
			<xsl:choose>
				<xsl:when test="pipeline/inputValidator">
					<xsl:text>inputValidator</xsl:text>
					<xsl:value-of select="$inputValidatorId"/>
				</xsl:when>
				<xsl:when test="pipeline/@firstPipe">
					<xsl:value-of select="pipeline/@firstPipe"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="pipeline/pipe[1]/@name"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
<!--		<adapterName><xsl:value-of select="$adapterName"/></adapterName>-->
<!--		<id><xsl:value-of select="$id"/></id>-->
<!--		<inputValidatorId><xsl:value-of select="$inputValidatorId"/></inputValidatorId>-->
<!--		<outputValidatorId><xsl:value-of select="$outputValidatorId"/></outputValidatorId>-->
<!--		<firstPipe><xsl:value-of select="$firstPipe"/></firstPipe>-->

		<xsl:apply-templates select="receiver">
			<xsl:with-param name="pipeline">
				<xsl:copy-of select="pipeline"/>
			</xsl:with-param>
			<xsl:with-param name="id" select="$id"/>
			<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
			<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
			<xsl:with-param name="firstPipe" select="$firstPipe"/>
		</xsl:apply-templates>

	</xsl:template>

	<xsl:template match="receiver">
		<xsl:param name="pipeline"/>
		<xsl:param name="id"/>
		<xsl:param name="inputValidatorId"/>
		<xsl:param name="outputValidatorId"/>
		<xsl:param name="firstPipe"/>

		<xsl:variable name="internalName">
			<xsl:text>ReceiveR</xsl:text>
			<xsl:value-of select="@name"/>
			<xsl:value-of select="$id"/>
		</xsl:variable>

		<element>
			<identification>
				<xsl:value-of select="concat($id, '|receiver|', $internalName)"/>
			</identification>
			<value>
				<xsl:text>	</xsl:text>
				<xsl:value-of select="$internalName"/>
				<xsl:text>{{Receiver</xsl:text>
				<xsl:text disable-output-escaping="yes">&lt;br/></xsl:text>
				<xsl:value-of select="@name"/>
				<xsl:text>}}</xsl:text>
				<xsl:text>&#10;</xsl:text>
			</value>
		</element>

		<xsl:variable name="newFlowHistory">
			<pipe>
				<xsl:value-of select="$internalName"/>
			</pipe>
		</xsl:variable>

		<!--This is where we actually forward to where we want to go. We don't use the forward template for this,
		 because we may have to forward to the pipelines inputvalidator, which the forward template can't handle. Maybe add this later, like for the outputValidator-->
		<xsl:choose>
			<!--If the firstPipe param is the same as the inputValidatorId concatenated after 'inputValidator', then we know we have to start with the inputValidator-->
			<xsl:when test="$firstPipe = concat('inputValidator', $inputValidatorId)">
				<xsl:call-template name="forward">
					<xsl:with-param name="pipeline" select="$pipeline"/>
					<xsl:with-param name="id" select="$id"/>
					<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
					<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
					<xsl:with-param name="flowHistory" select="$newFlowHistory"/>
					<xsl:with-param name="origin" select="concat('ReceiveR', @name)"/>
					<xsl:with-param name="isValidationFlow" select="false()"/>
					<xsl:with-param name="text" select="' '"/>
					<xsl:with-param name="destination" select="concat('inputValidator', $inputValidatorId)"/>
					<xsl:with-param name="specialForward" select="'inputValidator'"/>
					<xsl:with-param name="specialForwardValue"><!--This param is used to let the inputvalidator know where the success (or other) forwards go to when it doesn't defined them its self-->
<!--						<specialForwardValue>-->
							<forward name="success">
								<xsl:attribute name="path">
									<xsl:choose>
										<xsl:when test="$pipeline//pipeline/@firstPipe">
											<xsl:value-of select="$pipeline//pipeline/@firstPipe"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="$pipeline//pipe[1]/@name"/>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:attribute>
							</forward>
<!--						</specialForwardValue>-->
					</xsl:with-param>
				</xsl:call-template>

<!--				<xsl:apply-templates select="$pipeline//pipeline/inputValidator">-->
<!--					<xsl:with-param name="pipeline" select="$pipeline"/>-->
<!--					<xsl:with-param name="id" select="$id"/>-->
<!--					<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>-->
<!--					<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>-->
<!--					<xsl:with-param name="flowHistory" select="$flowHistory"/>-->
<!--					<xsl:with-param name="forwardTo"> &lt;!&ndash;This param is used to let the inputvalidator know where the success forward has to go since it doesn't defined it itsself&ndash;&gt;-->
<!--						<forward name="success">-->
<!--							<xsl:attribute name="path">-->
<!--								<xsl:choose>-->
<!--									<xsl:when test="$pipeline//pipeline/@firstPipe">-->
<!--										<xsl:value-of select="$pipeline//pipeline/@firstPipe"/>-->
<!--									</xsl:when>-->
<!--									<xsl:otherwise>-->
<!--										<xsl:value-of select="$pipeline//pipe[1]/@name"/>-->
<!--									</xsl:otherwise>-->
<!--								</xsl:choose>-->
<!--							</xsl:attribute>-->
<!--						</forward>-->
<!--					</xsl:with-param>-->
<!--				</xsl:apply-templates>-->
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="forward">
					<xsl:with-param name="pipeline" select="$pipeline"/>
					<xsl:with-param name="id" select="$id"/>
					<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
					<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
					<xsl:with-param name="flowHistory" select="$newFlowHistory"/>
					<xsl:with-param name="origin" select="concat('ReceiveR', @name)"/>
					<xsl:with-param name="isValidationFlow" select="false()"/>
					<xsl:with-param name="text" select="' '"/>
					<xsl:with-param name="destination" select="$firstPipe"/>
				</xsl:call-template>



<!--				<xsl:apply-templates select="$pipeline//pipe[@name = $firstPipe]">-->
<!--					<xsl:with-param name="pipeline">-->
<!--						<xsl:copy-of select="."/>-->
<!--					</xsl:with-param>-->
<!--					<xsl:with-param name="id" select="$id"/>-->
<!--					<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>-->
<!--					<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>-->
<!--					<xsl:with-param name="flowHistory" select="$flowHistory"/>-->
<!--					<xsl:with-param name="isValidationFlow" select="false()"/>-->
<!--				</xsl:apply-templates>-->
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--	<xsl:template match="pipeline">-->
<!--		<xsl:param name="id"/>-->
<!--		<xsl:param name="inputValidatorId"/>-->
<!--		<xsl:param name="outputValidatorId"/>-->
<!--		<xsl:param name="firstPipe"/>-->
<!--		<xsl:variable name="flowHistory"/>-->
<!--		<xsl:choose>-->
<!--			&lt;!&ndash;If the firstPipe param is the same as the inputValidatorId concatenated after 'inputValidator', then we know we have to start with the inputValidator&ndash;&gt;-->
<!--			<xsl:when test="$firstPipe = concat('inputValidator', $inputValidatorId)">-->
<!--				<xsl:apply-templates select="inputValidator">-->
<!--					<xsl:with-param name="pipeline">-->
<!--						<xsl:copy-of select="."/>-->
<!--					</xsl:with-param>-->
<!--					<xsl:with-param name="id" select="$id"/>-->
<!--					<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>-->
<!--					<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>-->
<!--&lt;!&ndash;					<xsl:with-param name="flowHistory" select="$flowHistory"/>&ndash;&gt;-->
<!--					<xsl:with-param name="forwardTo"> &lt;!&ndash;This param is used to let the inputvalidator know where the success forward has to go&ndash;&gt;-->
<!--							<forward name="success">-->
<!--								<xsl:attribute name="path">-->
<!--									<xsl:choose>-->
<!--										<xsl:when test="@firstPipe">-->
<!--											<xsl:value-of select="@firstPipe"/>-->
<!--										</xsl:when>-->
<!--										<xsl:otherwise>-->
<!--											<xsl:value-of select="pipe[1]/@name"/>-->
<!--										</xsl:otherwise>-->
<!--									</xsl:choose>-->
<!--								</xsl:attribute>-->
<!--							</forward>-->
<!--					</xsl:with-param>-->
<!--				</xsl:apply-templates>-->
<!--			</xsl:when>-->
<!--			<xsl:otherwise>-->
<!--				<xsl:apply-templates select="pipe[@name = $firstPipe]">-->
<!--					<xsl:with-param name="pipeline">-->
<!--						<xsl:copy-of select="."/>-->
<!--					</xsl:with-param>-->
<!--					<xsl:with-param name="id" select="$id"/>-->
<!--					<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>-->
<!--					<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>-->
<!--					<xsl:with-param name="flowHistory" select="$flowHistory"/>-->
<!--					<xsl:with-param name="isValidationFlow" select="false()"/>-->
<!--				</xsl:apply-templates>-->
<!--			</xsl:otherwise>-->
<!--		</xsl:choose>-->
<!--	</xsl:template>-->

	<xsl:template match="pipe">
		<xsl:param name="pipeline"/>
		<xsl:param name="id"/>
		<xsl:param name="inputValidatorId"/>
		<xsl:param name="outputValidatorId"/>
		<xsl:param name="flowHistory"/>
		<xsl:param name="isValidationFlow"/>

		<xsl:variable name="internalName">
			<xsl:value-of select="@name"/>
			<xsl:value-of select="$id"/>
		</xsl:variable>

<!--		<pipeTest><xsl:copy-of select="ancestor::pipeline"/></pipeTest>-->

		<!--If the internal name is not present in the flowHistory, proceed. Otherwise, don't proceed to prevent an infinite loop-->
		<xsl:if test="count($flowHistory/pipe[text() = $internalName]) = 0">
			<element>
				<identification>
					<xsl:value-of select="concat($id, '|pipe|', $internalName)"/>
				</identification>
				<value>
					<xsl:text>	</xsl:text>
					<xsl:value-of select="$internalName"/>
					<xsl:text>(</xsl:text>
					<xsl:value-of select="@name"/>
					<xsl:text disable-output-escaping="yes">&lt;br/></xsl:text>
					<xsl:choose>
						<xsl:when test="@className = 'nl.nn.adapterframework.pipes.GenericMessageSendingPipe'">
							<xsl:call-template name="afterLastIndexOf">
								<xsl:with-param name="string" select="sender/@className"/>
							</xsl:call-template>
						</xsl:when>
						<xsl:otherwise>
							<xsl:call-template name="afterLastIndexOf">
								<xsl:with-param name="string" select="@className"/>
							</xsl:call-template>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:text>)</xsl:text>
					<xsl:text>&#10;</xsl:text>
				</value>
			</element>

			<xsl:variable name="newFlowHistory">
				<xsl:if test="$flowHistory">
					<xsl:copy-of select="$flowHistory/*"/>
				</xsl:if>
				<pipe>
					<xsl:value-of select="$internalName"/>
				</pipe>
			</xsl:variable>

			<xsl:variable name="forwards">
				<xsl:apply-templates select="." mode="determinePipeForwards">
					<xsl:with-param name="pipeline" select="$pipeline"/>
					<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
				</xsl:apply-templates>
			</xsl:variable>
<!--			<forwardsTest><xsl:copy-of select="$forwards"/></forwardsTest>-->
			<xsl:apply-templates select="$forwards/*">
				<xsl:with-param name="pipeline" select="$pipeline"/>
				<xsl:with-param name="id" select="$id"/>
				<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
				<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
				<xsl:with-param name="flowHistory" select="$newFlowHistory"/>
				<xsl:with-param name="origin" select="@name"/>
				<xsl:with-param name="isValidationFlow" select="$isValidationFlow"/>
			</xsl:apply-templates>
		</xsl:if>
	</xsl:template>


<!--Validators-->
	<xsl:template match="inputValidator">
		<xsl:param name="pipeline"/>
		<xsl:param name="id"/>
		<xsl:param name="inputValidatorId"/>
		<xsl:param name="outputValidatorId"/>
		<xsl:param name="flowHistory"/>
		<xsl:param name="isValidationFlow"/>
		<xsl:param name="forwardTo"/>

		<xsl:variable name="internalName">
			<xsl:text>inputValidator</xsl:text>
			<xsl:value-of select="$inputValidatorId"/>
			<xsl:value-of select="$id"/>
		</xsl:variable>

		<element>
			<identification>
				<xsl:value-of select="concat($id, '|inputValidator|', $internalName)"/>
			</identification>
			<value>
				<xsl:text>	</xsl:text>
				<xsl:value-of select="$internalName"/>
				<xsl:text disable-output-escaping="yes">([inputValidator&lt;br/></xsl:text>
				<xsl:call-template name="afterLastIndexOf">
					<xsl:with-param name="string" select="@className"/>
				</xsl:call-template>
				<xsl:text>])</xsl:text>
				<xsl:text>&#10;</xsl:text>
				<xsl:text>	style </xsl:text>
				<xsl:value-of select="$internalName"/>
				<xsl:text> stroke-dasharray: 1 3</xsl:text>
				<xsl:text>&#10;</xsl:text>
			</value>
		</element>


		<xsl:variable name="newFlowHistory">
			<xsl:if test="$flowHistory">
				<xsl:copy-of select="$flowHistory/*"/>
			</xsl:if>
			<pipe>
				<xsl:value-of select="$internalName"/>
			</pipe>
		</xsl:variable>

		<xsl:apply-templates select="forward">
			<xsl:with-param name="pipeline" select="$pipeline"/>
			<xsl:with-param name="id" select="$id"/>
			<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
			<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
			<xsl:with-param name="flowHistory" select="$newFlowHistory"/>
			<xsl:with-param name="origin" select="concat('inputValidator', $inputValidatorId)"/>
			<xsl:with-param name="isValidationFlow" select="true()"/> <!--The failure/error forwards are validation errors and are therefore validation flow-->
		</xsl:apply-templates>
		<xsl:apply-templates select="$forwardTo/forward">
			<xsl:with-param name="pipeline" select="$pipeline"/>
			<xsl:with-param name="id" select="$id"/>
			<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
			<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
			<xsl:with-param name="flowHistory" select="$newFlowHistory"/>
			<xsl:with-param name="origin" select="concat('inputValidator', $inputValidatorId)"/>
			<xsl:with-param name="isValidationFlow" select="false()"/> <!--The failure/error forwards are validation errors and are therefore validation flow-->
		</xsl:apply-templates>
<!--		<xsl:call-template name="forward">-->
<!--			<xsl:with-param name="id" select="$id"/>-->
<!--			<xsl:with-param name="flowHistory" select="$newFlowHistory"/>-->
<!--			<xsl:with-param name="origin" select="'inputValidator'"/>-->
<!--			<xsl:with-param name="text" select="'success'"/>-->
<!--			<xsl:with-param name="destination" select="$forwardTo"/>-->
<!--		</xsl:call-template>-->
	</xsl:template>

	<xsl:template match="outputValidator">
		<xsl:param name="pipeline"/>
		<xsl:param name="id"/>
		<xsl:param name="inputValidatorId"/>
		<xsl:param name="outputValidatorId"/>
		<xsl:param name="flowHistory"/>
		<xsl:param name="isValidationFlow"/>
		<xsl:param name="forwardTo"/>

		<xsl:variable name="internalName">
			<xsl:text>outputValidator</xsl:text>
			<xsl:value-of select="$outputValidatorId"/>
			<xsl:value-of select="$id"/>
		</xsl:variable>

		<!--If the internal name is not present in the flowHistory, proceed. Otherwise, don't proceed to prevent an infinite loop-->
		<xsl:if test="count($flowHistory/pipe[text() = $internalName]) = 0">
			<element>
				<identification>
					<xsl:value-of select="concat($id, '|outputValidator|', $internalName)"/>
				</identification>
				<value>
					<xsl:text>	</xsl:text>
					<xsl:value-of select="$internalName"/>
					<xsl:text disable-output-escaping="yes">([outputValidator&lt;br/></xsl:text>
					<xsl:call-template name="afterLastIndexOf">
						<xsl:with-param name="string" select="@className"/>
					</xsl:call-template>
					<xsl:text>])</xsl:text>
					<xsl:text>&#10;</xsl:text>
					<xsl:text>	style </xsl:text>
					<xsl:value-of select="$internalName"/>
					<xsl:text> stroke-dasharray: 1 3</xsl:text>
					<xsl:text>&#10;</xsl:text>
				</value>
			</element>

			<xsl:variable name="newFlowHistory">
				<xsl:if test="$flowHistory">
					<xsl:copy-of select="$flowHistory/*"/>
				</xsl:if>
				<pipe>
					<xsl:value-of select="$internalName"/>
				</pipe>
			</xsl:variable>

			<xsl:apply-templates select="forward">
				<xsl:with-param name="pipeline" select="$pipeline"/>
				<xsl:with-param name="id" select="$id"/>
				<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
				<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
				<xsl:with-param name="flowHistory" select="$newFlowHistory"/>
				<xsl:with-param name="origin" select="concat('outputValidator', $outputValidatorId)"/>
				<xsl:with-param name="isValidationFlow" select="true()"/>
			</xsl:apply-templates>
			<xsl:apply-templates select="$forwardTo/forward">
				<xsl:with-param name="pipeline" select="$pipeline"/>
				<xsl:with-param name="id" select="$id"/>
				<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
				<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
				<xsl:with-param name="flowHistory" select="$newFlowHistory"/>
				<xsl:with-param name="origin" select="concat('outputValidator', $outputValidatorId)"/>
				<xsl:with-param name="isValidationFlow" select="false()"/> <!--The failure/error forwards are validation errors and are therefore validation flow-->
			</xsl:apply-templates>
			<!--Create forwards to each of the exits-->
			<xsl:for-each select="$pipeline//exit">
				<xsl:call-template name="forward">
					<xsl:with-param name="pipeline" select="$pipeline"/>
					<xsl:with-param name="id" select="$id"/>
					<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
					<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
					<xsl:with-param name="flowHistory" select="$newFlowHistory"/>
					<xsl:with-param name="origin" select="concat('outputValidator', $outputValidatorId)"/>
					<xsl:with-param name="isValidationFlow" select="false()"/>
					<xsl:with-param name="text" select="@path"/>
					<xsl:with-param name="destination" select="@path"/>
				</xsl:call-template>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>


<!--Forward stuff-->
	<!--This template determines what a pipe forwards to-->
	<xsl:template match="pipe" mode="determinePipeForwards">
		<xsl:param name="pipeline"/>
		<xsl:param name="outputValidatorId"/>
<!--		<determinePipeForwardsTest><xsl:copy-of select="ancestor::pipeline"/></determinePipeForwardsTest>-->
		<xsl:variable name="pos" select="position()"/>
		<xsl:variable name="initialForwards1">
			<xsl:copy-of select="forward"/>
			<xsl:choose>
				<xsl:when test="@className='nl.nn.adapterframework.pipes.XmlSwitch'
								or@className='nl.nn.adapterframework.pipes.CompareIntegerPipe'
								or@className='nl.nn.adapterframework.pipes.CompareStringPipe'
								or@className='nl.nn.adapterframework.pipes.FilenameSwitch'
								or@className='nl.nn.adapterframework.pipes.XmlIf'">

					<xsl:variable name="notFoundForwardName" select="@notFoundForwardName"/>
					<xsl:if test="string-length($notFoundForwardName)&gt;0 and (forward/@name=$notFoundForwardName)=false()">
						<forward name="notFoundForwardName" path="{$notFoundForwardName}"/>
					</xsl:if>
					<xsl:variable name="emptyForwardName" select="@emptyForwardName"/>
					<xsl:if test="string-length($emptyForwardName)&gt;0 and (forward/@name=$emptyForwardName)=false()">
						<forward name="emptyForwardName" path="{$emptyForwardName}"/>
					</xsl:if>
					<xsl:variable name="thenForwardName" select="@thenForwardName"/>
					<xsl:if test="string-length($thenForwardName)&gt;0 and (forward/@name=$thenForwardName)=false()">
						<forward name="thenForwardName" path="{$thenForwardName}"/>
					</xsl:if>
					<xsl:variable name="elseForwardName" select="@elseForwardName"/>
					<xsl:if test="string-length($elseForwardName)>0 and (forward/@name=$elseForwardName)=false()">
						<forward name="elseForwardName" path="{$elseForwardName}"/>
					</xsl:if>

<!--					<xsl:if test="string-length($notFoundForwardName) = 0 and-->
<!--									string-length($emptyForwardName) = 0 and-->
<!--									string-length($thenForwardName) = 0 and-->
<!--									string-length($elseForwardName) = 0">-->
<!--						-->
<!--					</xsl:if>-->
				</xsl:when>
				<xsl:otherwise>
					<xsl:if test="not(forward[@name='success'])">
						<forward name="success">
							<xsl:attribute name="path">
								<xsl:choose>
									<xsl:when test="count($pipeline//pipeline/pipe)&gt;$pos">
										<xsl:value-of select="$pipeline//pipeline/pipe[$pos+1]/@name"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$pipeline//exit[@state='success']/@path"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
						</forward>
					</xsl:if>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

<!--		<forwardTest01>-->
<!--			<xsl:copy-of select="$initialForwards1"/>-->
<!--		</forwardTest01>-->

		<xsl:variable name="initialForwards2">
			<xsl:copy-of select="$initialForwards1/*"/>
			<xsl:copy-of select="$pipeline/global-forwards/forward"/>
			<xsl:if test="count($initialForwards1/*) = 0 and @className = 'nl.nn.adapterframework.pipes.XmlSwitch'">
<!--				<isXmlSwitchWithNoForwards/>-->
				<xsl:variable name="originPipeName" select="@name"/>
				<xsl:for-each select="$pipeline//pipe[@name != $originPipeName]">
					<xsl:variable name="pipeName" select="@name"/>
<!--					<pipe name="{$pipeName}">-->
<!--						<test01><xsl:value-of select="-->
<!--										if($pipeline//pipeline/@firstPipe) then (-->
<!--											$pipeline//pipeline/@firstPipe != $pipeName-->
<!--										) else (-->
<!--											$pipeline//pipeline/pipe[1]/@name != $pipeName-->
<!--										)"/></test01>-->
<!--						<test02><xsl:value-of select="count($pipeline//pipe/forward[@path = $pipeName]) = 0"/></test02>-->
<!--						<test03><xsl:value-of select="count($pipeline//inputValidator/forward[@path = $pipeName]) = 0"/></test03>-->
<!--						<test04><xsl:value-of select="count($pipeline//outputValidator/forward[@path = $pipeName]) = 0"/></test04>-->
<!--						<test05><xsl:value-of select="count($pipeline//pipe[@notFoundForwardName = $pipeName]) = 0"/></test05>-->
<!--						<test06><xsl:value-of select="count($pipeline//pipe[@emptyForwardName = $pipeName]) = 0"/></test06>-->
<!--						<test07><xsl:value-of select="count($pipeline//pipe[@thenForwardName = $pipeName]) = 0"/></test07>-->
<!--						<test08><xsl:value-of select="count($pipeline//pipe[@elseForwardName = $pipeName]) = 0"/></test08>-->
						<xsl:if test="
										(if($pipeline//pipeline/@firstPipe) then (
											$pipeline//pipeline/@firstPipe != $pipeName
										) else (
											$pipeline//pipeline/pipe[1]/@name != $pipeName
										)) and
										count($pipeline//pipe/forward[@path = $pipeName]) = 0 and
										count($pipeline//inputValidator/forward[@path = $pipeName]) = 0 and
										count($pipeline//outputValidator/forward[@path = $pipeName]) = 0 and
										count($pipeline//pipe[@notFoundForwardName = $pipeName]) = 0 and
										count($pipeline//pipe[@emptyForwardName = $pipeName]) = 0 and
										count($pipeline//pipe[@thenForwardName = $pipeName]) = 0 and
										count($pipeline//pipe[@elseForwardName = $pipeName]) = 0">
							<forward name="unconfirmed-forward" path="{$pipeName}"/>
						</xsl:if>
<!--					</pipe>-->
				</xsl:for-each>
			</xsl:if>
		</xsl:variable>

<!--		<forwardTest02>-->
<!--			<xsl:copy-of select="$initialForwards1"/>-->
<!--		</forwardTest02>-->

		<!--Here we account for outputValidators. If there is one, we'll want to forward to it instead of to an exit-->
		<xsl:for-each select="$initialForwards2/*">
			<xsl:variable name="destination" select="@path"/>
<!--			<testPipeline><xsl:copy-of select="$pipeline"/></testPipeline>-->
			<xsl:choose>
<!--				<xsl:when test="true()"></xsl:when>-->
				<!--If this forward goes to a pipe-->
				<xsl:when test="$pipeline//pipeline/pipe[@name = $destination]">
					<xsl:copy-of select="."/>
				</xsl:when>
				<!--If this forward goes to an exit-->
				<xsl:otherwise>
					<xsl:choose>
						<!--If there is an outputValidator, redirect the flow to that instead-->
						<xsl:when test="$pipeline//pipeline/outputValidator"> <!--If there is an outputValidator-->
							<forward
								name="{concat(@name, '&lt;br/&gt;', $destination)}"
								path="{concat('outputValidator', $outputValidatorId)}"
								specialForward="outputValidator">
								<!--Here we define where the outputValidator forwards to. In this case, we're working with the pipeline outputValidator,
								and it can forward to all the exits, so we make forwards for each exit in the pipeline-->
								<specialForwardValue>
									<xsl:for-each select="$pipeline//exit">
										<forward name="{@path}" path="{@path}"/>
									</xsl:for-each>
								</specialForwardValue>
							</forward>
						</xsl:when>
						<!--Otherwise, forward to the exit-->
						<xsl:otherwise>
							<xsl:copy-of select="."/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
		<xsl:if test="sender[@className = 'nl.nn.adapterframework.senders.IbisLocalSender']">
			<xsl:variable name="javaListener" select="sender/@javaListener"/>
<!--			<forward name="|     TEST     |" path="{$javaListener}"/>-->
<!--			<forward name="|     TEST     |" path="{$all//adapter[receiver[@className = 'nl.nn.adapterframework.receivers.GenericReceiver']/-->
<!--					listener[@className = 'nl.nn.adapterframework.receivers.JavaListener' and @name = $javaListener]]/@name}"/>-->
			<xsl:for-each select="
				$all//adapter/receiver[@className = 'nl.nn.adapterframework.receivers.GenericReceiver']/
					listener[@className = 'nl.nn.adapterframework.receivers.JavaListener' and @name = $javaListener]">
				<xsl:variable name="adapterName" select="ancestor::adapter/@name"/>
				<forward name=" " path="ReceiveR{@name}" specialForward="differentAdapter" specialForwardValue="{$adapterToId/adapter[@name = $adapterName]/@id}"/>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

	<xsl:template match="forward">
		<xsl:param name="pipeline"/>
		<xsl:param name="id"/>
		<xsl:param name="inputValidatorId"/>
		<xsl:param name="outputValidatorId"/>
		<xsl:param name="flowHistory"/>
		<xsl:param name="origin"/>
		<xsl:param name="isValidationFlow"/>
<!--		<forwardTest1><xsl:copy-of select="ancestor::pipeline"/></forwardTest1>-->
		<xsl:call-template name="forward">
			<xsl:with-param name="pipeline" select="$pipeline"/>
			<xsl:with-param name="id" select="$id"/>
			<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
			<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
			<xsl:with-param name="flowHistory" select="$flowHistory"/>
			<xsl:with-param name="origin" select="$origin"/>
			<xsl:with-param name="isValidationFlow" select="$isValidationFlow"/>
			<xsl:with-param name="text" select="@name"/>
			<xsl:with-param name="destination" select="@path"/>
			<xsl:with-param name="specialForward" select="if(string-length(@specialForward) != 0) then (@specialForward) else ('')"/>
			<xsl:with-param name="specialForwardValue" select="if(specialForwardValue) then (specialForwardValue) else (@specialForwardValue)"/>
<!--			<xsl:with-param name="goesToOtherAdapter" select="if(string-length(@goesToOtherAdapter) != 0) then (@goesToOtherAdapter) else ('')"/>-->
<!--			<xsl:with-param name="goesToOtherAdapter" select="@goesToOtherAdapter"/>-->
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="forward">
		<xsl:param name="pipeline"/>
		<xsl:param name="id"/>
		<xsl:param name="inputValidatorId"/>
		<xsl:param name="outputValidatorId"/>
		<xsl:param name="flowHistory"/>
		<xsl:param name="origin"/>
		<xsl:param name="text"/>
		<xsl:param name="destination"/>
		<xsl:param name="isValidationFlow"/>
		<xsl:param name="specialForward" select="''"/><!--Used to specify special forwards such as input and output wrappers and validators.-->
		<xsl:param name="specialForwardValue"></xsl:param><!--Used to supply nessecary information when this forward is a special one.-->

		<forwardTest>
			<id><xsl:value-of select="$id"/></id>
			<inputValidatorId><xsl:value-of select="$inputValidatorId"/></inputValidatorId>
			<outputValidatorId><xsl:value-of select="$outputValidatorId"/></outputValidatorId>
			<flowHistory><xsl:copy-of select="$flowHistory"/></flowHistory>
			<origin><xsl:value-of select="$origin"/></origin>
			<text><xsl:value-of select="$text" disable-output-escaping="yes"/></text>
			<destination><xsl:value-of select="$destination"/></destination>
			<isValidationFlow><xsl:value-of select="$isValidationFlow"/></isValidationFlow>
			<specialForward><xsl:copy-of select="$specialForward"/></specialForward>
			<specialForward><xsl:value-of select="$specialForward"/></specialForward>
			<specialForwardValue><xsl:copy-of select="$specialForwardValue"/></specialForwardValue>
			<specialForwardValue><xsl:value-of select="$specialForwardValue"/></specialForwardValue>
			<testSpecialForward><xsl:value-of select="$specialForward != 'differentAdapter'"/></testSpecialForward>
			<testSpecialForward><xsl:value-of select="$specialForward = 'differentAdapter'"/></testSpecialForward>
			<testSpecialForward><xsl:value-of select="string-length($specialForward)"/></testSpecialForward>
		</forwardTest>

<!--		<forwardTest2><xsl:copy-of select="ancestor::pipeline"/></forwardTest2>-->
<!--		<xsl:text>&#10;</xsl:text>-->
		<element isValidationFlow="{$isValidationFlow}">
			<identification>
				<!--If this forward goes to a different adapter, use the id supplied in $specialForwardValue, else use this adapters id.
					We do this to make sure this forward put in the correct place. When all elements have been created,
					they are first grouped by the id portion of this identification element.
					This means a forward to a different adapter will be grouped with all other elements in that adapter.
					We want to group it with that adapter to make sure the elements are placed correctly in the resulting picture-->
				<xsl:value-of select="concat(if($specialForward = 'differentAdapter') then ($specialForwardValue) else ($id), '|', $id, '|', $origin, '|', $text, '|', $destination)"/>
			</identification>
			<value>
				<xsl:text>	</xsl:text>
				<xsl:value-of select="$origin"/>
				<xsl:value-of select="$id"/>
				<xsl:choose>
					<xsl:when test="$isValidationFlow">
						<xsl:text disable-output-escaping="yes"> -. </xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text disable-output-escaping="yes"> --> |</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:value-of select="$text" disable-output-escaping="yes"/>
				<xsl:choose>
					<xsl:when test="$isValidationFlow">
						<xsl:text disable-output-escaping="yes"> .-> </xsl:text>
					</xsl:when>
					<xsl:otherwise><xsl:text>| </xsl:text></xsl:otherwise>
				</xsl:choose>
				<xsl:value-of select="$destination"/>
				<!--If this forward goes to a different adapter, use the id supplied in $specialForwardValue, else use this adapters id-->
				<xsl:value-of select="if($specialForward = 'differentAdapter') then ($specialForwardValue) else ($id)"/>
				<xsl:text>&#10;</xsl:text>
			</value>
		</element>

		<xsl:if test="$specialForward != 'differentAdapter'">
			<doesNotGoToDifferentAdapter/>
			<!--Forward to pipe-->
			<xsl:choose>
				<!--If this forward was replaced by one that goes to the outputValidator-->
				<xsl:when test="$specialForward = 'outputValidator'">
					<xsl:apply-templates select="$pipeline//pipeline/outputValidator">
						<xsl:with-param name="pipeline" select="$pipeline"/>
						<xsl:with-param name="id" select="$id"/>
						<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
						<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
						<xsl:with-param name="flowHistory" select="$flowHistory"/>
						<xsl:with-param name="isValidationFlow" select="$isValidationFlow"/>
						<xsl:with-param name="forwardTo" select="$specialForwardValue"/>
					</xsl:apply-templates>
				</xsl:when>
				<xsl:when test="$specialForward = 'inputValidator'">
					<xsl:apply-templates select="$pipeline//pipeline/inputValidator">
						<xsl:with-param name="pipeline" select="$pipeline"/>
						<xsl:with-param name="id" select="$id"/>
						<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
						<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
						<xsl:with-param name="flowHistory" select="$flowHistory"/>
						<xsl:with-param name="isValidationFlow" select="$isValidationFlow"/>
						<xsl:with-param name="forwardTo" select="$specialForwardValue"/>
					</xsl:apply-templates>
				</xsl:when>
				<xsl:when test="$specialForward = 'inputWrapper'">
					<xsl:apply-templates select="$pipeline//pipeline/inputWrapper">
						<xsl:with-param name="pipeline" select="$pipeline"/>
						<xsl:with-param name="id" select="$id"/>
						<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
						<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
						<xsl:with-param name="flowHistory" select="$flowHistory"/>
						<xsl:with-param name="isValidationFlow" select="$isValidationFlow"/>
						<xsl:with-param name="forwardTo" select="$specialForwardValue"/>
					</xsl:apply-templates>
				</xsl:when>
				<xsl:when test="$specialForward = 'outputWrapper'">
					<xsl:apply-templates select="$pipeline//pipeline/outputWrapper">
						<xsl:with-param name="pipeline" select="$pipeline"/>
						<xsl:with-param name="id" select="$id"/>
						<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
						<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
						<xsl:with-param name="flowHistory" select="$flowHistory"/>
						<xsl:with-param name="isValidationFlow" select="$isValidationFlow"/>
						<xsl:with-param name="forwardTo" select="$specialForwardValue"/>
					</xsl:apply-templates>
				</xsl:when>
				<!--If this forward goes to a pipe-->
				<xsl:when test="$pipeline//pipeline/pipe[@name = $destination]">
					<xsl:apply-templates select="$pipeline//pipeline/pipe[@name = $destination]">
						<xsl:with-param name="pipeline" select="$pipeline"/>
						<xsl:with-param name="id" select="$id"/>
						<xsl:with-param name="inputValidatorId" select="$inputValidatorId"/>
						<xsl:with-param name="outputValidatorId" select="$outputValidatorId"/>
						<xsl:with-param name="flowHistory" select="$flowHistory"/>
						<xsl:with-param name="isValidationFlow" select="$isValidationFlow"/>
					</xsl:apply-templates>
				</xsl:when>
				<!--If this forward goes to an exit-->
				<xsl:otherwise>
					<xsl:apply-templates select="$pipeline//exit[@path = $destination]">
						<xsl:with-param name="id" select="$id"/>
						<!--					<xsl:with-param name="flowHistory" select="$flowHistory"/>-->
						<!--					<xsl:with-param name="isValidationFlow" select="$isValidationFlow"/>-->
					</xsl:apply-templates>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

	<xsl:template match="exit">
		<xsl:param name="id"/>

		<xsl:variable name="internalName">
			<xsl:value-of select="@path"/>
			<xsl:value-of select="$id"/>
		</xsl:variable>
		
		<element>
			<identification>
				<xsl:value-of select="concat($id, '|exit|', $internalName)"/>
			</identification>
			<value>
				<xsl:text>	</xsl:text>
				<xsl:value-of select="$internalName"/>
				<xsl:text>{{</xsl:text>
<!--				<xsl:value-of select="@path"/>-->
<!--				<xsl:text disable-output-escaping="yes">&lt;br/></xsl:text>-->
				<xsl:value-of select="@state"/>
				<xsl:if test="@code">
					<xsl:text disable-output-escaping="yes">&lt;br/></xsl:text>
					<xsl:value-of select="@code"/>
				</xsl:if>
				<xsl:text>}}</xsl:text>
				<xsl:text>&#10;</xsl:text>
			</value>
		</element>
	</xsl:template>

	<xsl:template name="afterLastIndexOf">
		<xsl:param name="string"/>
		<xsl:variable name="char">.</xsl:variable>
		<xsl:choose>
			<xsl:when test="contains($string, $char)">
				<xsl:call-template name="afterLastIndexOf">
					<xsl:with-param name="string" select="substring-after($string, $char)"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$string"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

</xsl:stylesheet>