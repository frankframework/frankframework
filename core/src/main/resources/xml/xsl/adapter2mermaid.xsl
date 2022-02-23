<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0">
	<xsl:output method="text" indent="yes" omit-xml-declaration="yes"/>

	<xsl:variable name="errorForwards" select="('exception','failure','fail','timeout','illegalResult','presumedTimeout','interrupt','parserError','outputParserError','outputFailure')"/>
	<xsl:variable name="adapterCount" select="count(//adapter)"/>

	<xsl:template match="/">
		<!-- Create the Mermaid graph in 2 steps
			- First preprocess adapters, putting pipes in the correct order, explicitly adding implicit forwards and preprocess input and output validators and wrappers
			- Then convert the adapter to mermaid code-->
		<xsl:variable name="preproccessedConfiguration">
			<xsl:apply-templates select="*" mode="preprocess"/>
		</xsl:variable>

		<xsl:text>graph&#10;</xsl:text>
		<xsl:text> classDef default fill:#fff,stroke:#1a9496,stroke-width:2px;&#10;</xsl:text>
		<xsl:apply-templates select="$preproccessedConfiguration" mode="convertElements"/>
		<xsl:apply-templates select="$preproccessedConfiguration//forward" mode="convertForwards"/>

		<!-- The code below gives back the preprocessed configuration
		This is for testing purposes, to test also change outputtype(line 3) to xml instead of text-->
		<!-- <xsl:copy-of select="$preproccessedConfiguration"/> -->
	</xsl:template>

	<xsl:template match="*" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="defaultCopyActions"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@*|comment()" mode="preprocess">
		<xsl:copy/>
	</xsl:template>

	<xsl:template match="adapter" mode="preprocess">
		<xsl:copy>
			<xsl:attribute name="elementID" select="generate-id()"/>
			<xsl:apply-templates select="@*" mode="#current"/>
			<xsl:variable name="firstElementID">
				<xsl:choose>
					<xsl:when test="exists(pipeline/inputValidator)">
						<xsl:value-of select="generate-id(pipeline/inputValidator)"/>
					</xsl:when>
					<xsl:when test="exists(pipeline/inputWrapper)">
						<xsl:value-of select="generate-id(pipeline/inputWrapper)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="generate-id((pipeline/pipe[@name=current()/pipeline/@firstPipe],pipeline/pipe[1])[1])"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:apply-templates select="receiver" mode="preprocess">
				<xsl:with-param name="targetID" select="$firstElementID"/>
			</xsl:apply-templates>
			<xsl:apply-templates select="pipeline" mode="preprocess">
				<xsl:with-param name="firstElementID" select="$firstElementID"/>
			</xsl:apply-templates>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="receiver" mode="preprocess">
		<xsl:param name="targetID"/>
		<xsl:copy>
			<xsl:attribute name="targetID" select="$targetID"/>
			<xsl:call-template name="defaultCopyActions"/>
			<xsl:element name="forward">
				<xsl:attribute name="name" select="'success'"/>
				<xsl:attribute name="path" select="$targetID"/>
				<xsl:attribute name="targetID" select="$targetID"/>
			</xsl:element>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipeline" mode="preprocess">
		<xsl:param name="firstElementID"/>
		<!-- Modify the pipeline in the following ways:
			- Add a unique ID on all elements, and add that ID to the forwards pointing to that element
			- Add implicit forwards and global-forwards explicitly to inputValidator -and Wrapper and all pipes
			- For each exit that is used by the pipeline, make an outputWrapper -and Validator if they originally existed
			- Recursively go through pipeline to determine things like errorHandling.
			- Create all exits that are used by the pipeline
		-->
		<xsl:copy>
			<xsl:apply-templates select="@*" mode="#current"/>
			<xsl:variable name="firstPipe" select="(pipe[@name=current()/@firstPipe],pipe[1])[1]"/>

			<xsl:variable name="elementsWithExplicitForwards">
				<xsl:apply-templates select="inputValidator" mode="#current">
					<xsl:with-param name="firstPipe" select="$firstPipe"/>
				</xsl:apply-templates>

				<xsl:apply-templates select="inputWrapper" mode="#current">
					<xsl:with-param name="firstPipe" select="$firstPipe"/>
				</xsl:apply-templates>

				<xsl:apply-templates select="pipe" mode="#current"/>
			</xsl:variable>

			<xsl:variable name="pipelineWithExplicitForwards">
				<xsl:copy-of select="$elementsWithExplicitForwards/*"/>
				<!-- Create outputwrappers and outputvalidators per exit -->
				<xsl:for-each select=".//exit[@path = $elementsWithExplicitForwards//forward/@path]">
					<xsl:apply-templates select="ancestor::pipeline/outputWrapper" mode="#current">
						<xsl:with-param name="exit" select="."/>
					</xsl:apply-templates>
					<xsl:variable name="exit" select="."/>
					<xsl:for-each select="ancestor::pipeline/(outputValidator,inputValidator[@responseRoot])[1]">
						<xsl:call-template name="preprocessOutputValidator">
							<xsl:with-param name="exit" select="$exit"/>
						</xsl:call-template>
					</xsl:for-each>
				</xsl:for-each>
			</xsl:variable>

			<xsl:apply-templates select="$pipelineWithExplicitForwards/*[@elementID=$firstElementID]" mode="sort">
				<xsl:with-param name="originalPipes">
					<xsl:copy-of select="$pipelineWithExplicitForwards/*"/>
				</xsl:with-param>
			</xsl:apply-templates>

			<xsl:apply-templates select=".//exit" mode="#current"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipeline/inputValidator" mode="preprocess">
		<xsl:param name="firstPipe"/>

		<xsl:copy>
			<xsl:call-template name="defaultPipeCopyActions"/>
			<!-- Add success forward -->
			<xsl:call-template name="createForward">
				<xsl:with-param name="name" select="'success'"/>
				<xsl:with-param name="path" select="$firstPipe/@name"/>
			</xsl:call-template>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipeline/inputWrapper" mode="preprocess">
		<xsl:param name="firstPipe"/>

		<xsl:copy>
			<xsl:call-template name="defaultCopyActions"/>
			<!-- Add success forward -->
			<xsl:call-template name="createForward">
				<xsl:with-param name="name" select="'success'"/>
				<xsl:with-param name="path" select="$firstPipe/@name"/>
			</xsl:call-template>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipeline/outputWrapper" mode="preprocess">
		<xsl:param name="exit"/>

		<xsl:copy>
			<xsl:attribute name="elementID" select="concat(generate-id(), '-', $exit/@path)"/>
			<xsl:apply-templates select="*|@*" mode="#current"/>
			<!-- Add success forward -->
			<xsl:call-template name="createForward">
				<xsl:with-param name="name" select="'success'"/>
				<xsl:with-param name="path" select="$exit/@path"/>
			</xsl:call-template>
		</xsl:copy>
	</xsl:template>

	<xsl:template name="preprocessOutputValidator">
		<xsl:param name="exit"/>

		<xsl:element name="outputValidator">
			<xsl:attribute name="elementID" select="concat(generate-id(), '-', $exit/@path)"/>
			<xsl:apply-templates select="*[name() != 'forward']|@*" mode="#current"/>
			<xsl:apply-templates select="forward|../global-forwards/forward[not(@name = current()/forward/@name)]" mode="#current">
				<xsl:with-param name="parentName" select="'outputValidator'"/>
			</xsl:apply-templates>
			<!-- Add success forward -->
			<xsl:element name="forward">
				<xsl:attribute name="name" select="'success'"/>
				<xsl:attribute name="customText" select="$exit/@path" />
				<xsl:attribute name="path" select="$exit/@path" />
				<xsl:attribute name="targetID" select="generate-id($exit)"/>
			</xsl:element>
		</xsl:element>
	</xsl:template>

	<xsl:template match="pipe" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="defaultPipeCopyActions"/>

			<xsl:for-each select="descendant::sender[@className='nl.nn.adapterframework.senders.IbisLocalSender']">
				<xsl:call-template name="IbisLocalSender"/>
			</xsl:for-each>
			<!-- Add success forward if not present -->
			<xsl:call-template name="createForwardIfNecessary">
				<xsl:with-param name="forwards" select="forward"/>
				<xsl:with-param name="name" select="'success'"/>
				<xsl:with-param name="path" select="(following-sibling::pipe/@name,..//exit[@state='success']/@path,'EXIT')[1]"/>
			</xsl:call-template>
		</xsl:copy>
	</xsl:template>

	<!-- The following pipes do not have a success forward, so it should not be added by default -->
	<xsl:template match="pipe[@className=('nl.nn.adapterframework.pipes.CompareStringPipe',
										 'nl.nn.adapterframework.pipes.CompareIntegerPipe')]" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="defaultPipeCopyActions"/>
		</xsl:copy>
	</xsl:template>

	<!-- The XmlIf can have different forward names which then might directly point to a next pipe/exit -->
	<xsl:template match="pipe[@className='nl.nn.adapterframework.pipes.XmlIf']" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="switchPipeCopyActions"/>

			<xsl:variable name="thenForwardName" select="if (exists(@thenForwardName)) then (@thenForwardName) else ('then')"/>
			<xsl:variable name="elseForwardName" select="if (exists(@elseForwardName)) then (@elseForwardName) else ('else')"/>
			<xsl:variable name="forwardNames" select="($thenForwardName,$elseForwardName)"/>
			<xsl:variable name="forwards">
				<xsl:apply-templates select="forward[@name = $forwardNames]" mode="#current"/>
				<xsl:apply-templates select="../global-forwards/forward[not(@name = current()/forward/@name) and @name = $forwardNames]" mode="#current"/>
			</xsl:variable>

			<xsl:copy-of select="$forwards"/>

			<xsl:call-template name="createForwardIfNecessary">
				<xsl:with-param name="forwards" select="$forwards/forward"/>
				<xsl:with-param name="name" select="'then'"/>
				<xsl:with-param name="path" select="$thenForwardName"/>
			</xsl:call-template>
			<xsl:call-template name="createForwardIfNecessary">
				<xsl:with-param name="forwards" select="$forwards/forward"/>
				<xsl:with-param name="name" select="'else'"/>
				<xsl:with-param name="path" select="$elseForwardName"/>
			</xsl:call-template>
		</xsl:copy>
	</xsl:template>

	<!-- The XmlSwitchPipe can have no forwards, in that case do nothing. Users shouldn't let this happen -->
	<xsl:template match="pipe[@className='nl.nn.adapterframework.pipes.XmlSwitch']" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="switchPipeCopyActions"/>

			<xsl:variable name="forwards">
				<xsl:apply-templates select="forward" mode="#current"/>
				<xsl:apply-templates select="../global-forwards/forward[not(@name = current()/forward/@name)]" mode="#current"/>
			</xsl:variable>

			<xsl:copy-of select="$forwards"/>

			<xsl:call-template name="createForwardIfNecessary">
				<xsl:with-param name="forwards" select="$forwards/forward"/>
				<xsl:with-param name="name" select="'notFoundForwardName'"/>
				<xsl:with-param name="path" select="@notFoundForwardName"/>
			</xsl:call-template>
			<xsl:call-template name="createForwardIfNecessary">
				<xsl:with-param name="forwards" select="$forwards/forward"/>
				<xsl:with-param name="name" select="'emptyForwardName'"/>
				<xsl:with-param name="path" select="@emptyForwardName"/>
			</xsl:call-template>
		</xsl:copy>
	</xsl:template>

	<!-- For local senders, add a 'forward' to the sub-adapter -->
	<xsl:template name="IbisLocalSender">
		<xsl:copy>
			<xsl:call-template name="defaultCopyActions"/>
		</xsl:copy>

		<xsl:variable name="targetReceiver" select="ancestor::*/adapter/receiver[listener[@className='nl.nn.adapterframework.receivers.JavaListener' and @name=current()/@javaListener]]"/>
		<xsl:if test="exists($targetReceiver)">
			<xsl:element name="forward">
				<xsl:attribute name="name" select="'differentAdapter'"/>
				<xsl:attribute name="path" select="$targetReceiver/parent::adapter/@name"/>
				<xsl:attribute name="targetID" select="generate-id($targetReceiver)"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>

	<xsl:template match="forward" mode="preprocess">
		<xsl:param name="pipeline" select="ancestor::pipeline"/>
		<xsl:param name="parentName" select="parent::*/name()"/>
		<xsl:copy>
			<xsl:variable name="target" select="$pipeline/(pipe[@name=current()/@path]|.//exit[@path=current()/@path])[1]"/>

			<xsl:attribute name="testParentName" select="$parentName"/>
			<!-- When the forward is an exit, link it to the corresponding outputWrapper or outputValidator -->
			<xsl:choose>
				<xsl:when test="name($target)='exit'">
					<xsl:attribute name="customText" select="$target/@path"/>
					<xsl:choose>
						<xsl:when test="not($parentName = ('outputWrapper','outputValidator')) and exists($pipeline/outputWrapper)">
							<xsl:attribute name="targetID" select="concat(generate-id($pipeline/outputWrapper), '-', $target/@path)"/>
						</xsl:when>
						<xsl:when test="$parentName != 'outputValidator' and exists(($pipeline/outputValidator,$pipeline/inputValidator[@responseRoot]))">
							<xsl:attribute name="targetID" select="concat(generate-id($pipeline/(outputValidator,inputValidator[@responseRoot])[1]), '-', $target/@path)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="targetID" select="generate-id($target)"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="$parentName = 'inputValidator' and @name = 'success' and exists($pipeline/inputWrapper)">
							<xsl:attribute name="targetID" select="generate-id($pipeline/inputWrapper)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="targetID" select="generate-id($target)"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:apply-templates select="*|@*" mode="#current"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="*" mode="sort">
		<xsl:param name="finalProcessing" as="xs:boolean" select="true()"/>
		<xsl:param name="errorHandling" as="xs:boolean" select="false()"/>
		<xsl:param name="originalPipes"/>
		<xsl:param name="iterationDepth" as="xs:integer" select="0"/>

		<xsl:variable name="newOriginalPipes">
			<xsl:copy-of select="$originalPipes/*[@elementID != current()/@elementID]"/>
		</xsl:variable>

		<xsl:variable name="processedPipes">
			<xsl:copy>
				<xsl:attribute name="errorHandling" select="$errorHandling"/>
				<xsl:attribute name="iterationDepth" select="$iterationDepth"/>
				<xsl:copy-of select="@*|*[name() != 'forward']"/>
				<xsl:for-each select="forward">
					<xsl:copy>
						<xsl:attribute name="errorHandling" select="$errorHandling or @name = $errorForwards"/>
						<xsl:copy-of select="@*|*"/>
					</xsl:copy>
				</xsl:for-each>
			</xsl:copy>

			<xsl:for-each select="forward">
				<xsl:variable name="errorHandling" select="$errorHandling or @name = $errorForwards"/>
				<xsl:apply-templates select="$originalPipes/*[@elementID=current()/@targetID]" mode="#current">
					<xsl:with-param name="finalProcessing" select="false()"/>
					<xsl:with-param name="errorHandling" select="$errorHandling"/>
					<xsl:with-param name="originalPipes">
						<xsl:copy-of select="$newOriginalPipes"/>
					</xsl:with-param>
					<xsl:with-param name="iterationDepth" select="$iterationDepth+1"/>
				</xsl:apply-templates>
			</xsl:for-each>
		</xsl:variable>

		<!-- When processing the pipes via forwards, loops might result in pipes being duplicated, keep only 1 of each pipe with a preference for errorHandling=false-->
		<xsl:choose>
			<xsl:when test="$finalProcessing">
				<xsl:for-each-group select="$processedPipes/*" group-by="@elementID">
					<xsl:copy-of select="(current-group()[not(xs:boolean(@errorHandling))],current-group()[xs:boolean(@errorHandling)])[1]"/>
				</xsl:for-each-group>
			</xsl:when>
			<xsl:otherwise><xsl:copy-of select="$processedPipes"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="defaultCopyActions">
		<xsl:attribute name="elementID" select="generate-id()"/>
		<xsl:apply-templates select="*|@*" mode="#current"/>
	</xsl:template>

	<xsl:template name="defaultPipeCopyActions">
		<xsl:call-template name="defaultCopyActions"/>
		<xsl:apply-templates select="../global-forwards/forward[not(@name = current()/forward/@name)]" mode="#current"/>
	</xsl:template>

	<xsl:template name="switchPipeCopyActions">
		<xsl:attribute name="elementID" select="generate-id()"/>
		<xsl:apply-templates select="*[local-name() != 'forward']|@*" mode="#current"/>
	</xsl:template>

	<xsl:template name="createForwardIfNecessary">
		<xsl:param name="forwards"/>
		<xsl:param name="name" select="''"/>
		<xsl:param name="path" select="''"/>
		<xsl:if test="$name != '' and $path != '' and empty($forwards[@name=$name])">
			<xsl:call-template name="createForward">
				<xsl:with-param name="name" select="$name"/>
				<xsl:with-param name="path" select="$path"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

	<xsl:template name="createForward">
		<xsl:param name="path"/>
		<xsl:param name="name" select="$path"/>

		<xsl:variable name="forward">
			<xsl:element name="forward">
				<xsl:attribute name="name" select="$name"/>
				<xsl:attribute name="path" select="$path"/>
			</xsl:element>
		</xsl:variable>
		<xsl:apply-templates select="$forward/forward" mode="#current">
			<!--Scope is the variable which has no parent, so explicitly pass pipeline parameter-->
			<xsl:with-param name="pipeline" select="ancestor::pipeline"/>
			<xsl:with-param name="parentName" select="name()"/>
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template match="*|@*|text()" mode="convertElements">
		<xsl:apply-templates select="*|@*|text()" mode="#current"/>
	</xsl:template>

	<xsl:template match="adapter" mode="convertElements">
		<xsl:if test="number($adapterCount) gt 1">
			<xsl:text>	style </xsl:text>
			<xsl:value-of select="@name"/>
			<xsl:text> fill:#0000,stroke:#1a9496,stroke-width:2px&#10;</xsl:text>
			<xsl:text>	subgraph </xsl:text>
			<xsl:value-of select="@name"/>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:apply-templates select="*|@*|text()" mode="convertElements"/>
		<xsl:if test="number($adapterCount) gt 1">
			<xsl:text>	end&#10;</xsl:text>
		</xsl:if>
	</xsl:template>

	<xsl:template match="receiver" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="shapeStart" select="'{{'"/>
			<xsl:with-param name="shapeEnd" select="'}}'"/>
			<xsl:with-param name="text" select="'Receiver'"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="inputValidator" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="shapeStart" select="'(['"/>
			<xsl:with-param name="shapeEnd" select="'])'"/>
			<xsl:with-param name="text" select="'InputValidator'"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="inputWrapper" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="shapeStart" select="'(['"/>
			<xsl:with-param name="shapeEnd" select="'])'"/>
			<xsl:with-param name="text" select="'InputWrapper'"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="outputWrapper" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="shapeStart" select="'(['"/>
			<xsl:with-param name="shapeEnd" select="'])'"/>
			<xsl:with-param name="text" select="'OutputWrapper'"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="outputValidator" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="shapeStart" select="'(['"/>
			<xsl:with-param name="shapeEnd" select="'])'"/>
			<xsl:with-param name="text" select="'OutputValidator'"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="pipe[@className=
										('nl.nn.adapterframework.pipes.XmlSwitch'
										,'nl.nn.adapterframework.pipes.CompareIntegerPipe'
										,'nl.nn.adapterframework.pipes.CompareStringPipe'
										,'nl.nn.adapterframework.pipes.FilenameSwitch'
										,'nl.nn.adapterframework.pipes.XmlIf')]" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="shapeStart" select="'{'"/>
			<xsl:with-param name="shapeEnd" select="'}'"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="pipe" mode="convertElements">
		<xsl:call-template name="createMermaidElement"/>
	</xsl:template>

	<xsl:template match="exit" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="shapeStart" select="'{{'"/>
			<xsl:with-param name="shapeEnd" select="'}}'"/>
			<xsl:with-param name="text" select="@state"/>
			<xsl:with-param name="subText" select="(@code,'')[1]"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="createMermaidElement">
		<xsl:param name="shapeStart" select="'('"/>
		<xsl:param name="shapeEnd" select="')'"/>
		<xsl:param name="text" select="xs:string((@name,name())[1])"/>
		<xsl:param name="subText" select="tokenize((sender/@className,listener/@className,@className)[1], '\.')[last()]"/>

		<xsl:if test="xs:boolean(@errorHandling)">
			<xsl:text>	style </xsl:text>
			<xsl:value-of select="@elementID"/>
			<xsl:text> stroke-dasharray: 4 4</xsl:text>
			<xsl:text>&#10;</xsl:text>
		</xsl:if>
		<xsl:text>	</xsl:text>
		<xsl:value-of select="@elementID"/>
		<xsl:value-of select="$shapeStart"/>
		<xsl:value-of select="$text"/>
		<xsl:if test="$subText != ''">
			<xsl:text>&amp;lt;br></xsl:text>
			<xsl:value-of select="$subText"/>
		</xsl:if>
		<xsl:value-of select="$shapeEnd"/>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>

	<xsl:template match="forward" mode="convertForwards">
		<xsl:text>	</xsl:text>
		<xsl:value-of select="parent::*/@elementID"/>
		<xsl:value-of select="if (xs:boolean(@errorHandling)) then (' -. ') else (' --> |')"/>
		<xsl:value-of select="@name"/>
		<xsl:if test="exists(@customText)">
			<xsl:text>&amp;lt;br></xsl:text>
			<xsl:value-of select="@customText"/>
		</xsl:if>
		<xsl:value-of select="if (xs:boolean(@errorHandling)) then (' .-> ') else ('| ')"/>
		<xsl:value-of select="@targetID"/>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
