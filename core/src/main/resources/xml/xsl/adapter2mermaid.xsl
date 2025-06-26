<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0">
	<xsl:param name="frankElements"/>

	<xsl:output method="text" indent="yes" omit-xml-declaration="yes"/>

	<xsl:variable name="adapterCount" select="count(//adapter)"/>
	<xsl:variable name="errorForwards" select="('exception','failure','fail','timeout','illegalResult','presumedTimeout','interrupt','parserError','outputParserError','outputFailure')"/>

	<xsl:template match="/">
		<!-- Create the Mermaid graph in 2 steps
			- First preprocess adapters, putting pipes in the correct order, explicitly adding implicit forwards and preprocess input and output validators and wrappers
			- Then convert the adapter to mermaid code-->
		<xsl:variable name="adapterWithExits">
			<xsl:apply-templates select="*" mode="resolveExits"/>
		</xsl:variable>

		<xsl:variable name="preproccessedAdapter">
			<xsl:apply-templates select="$adapterWithExits/adapter" mode="preprocess"/>
		</xsl:variable>
		<xsl:variable name="forwards" select="$preproccessedAdapter//forward"/>

		<xsl:text>flowchart&#10;</xsl:text>
		<xsl:apply-templates select="$preproccessedAdapter" mode="convertElements"/>
		<xsl:text>	classDef normal fill:#fff,stroke-width:4px,stroke:#8bc34a;&#10;</xsl:text>
		<xsl:text>	classDef errorOutline fill:#fff,stroke-width:4px,stroke:#ec4758;&#10;</xsl:text>
		<xsl:apply-templates select="$forwards" mode="convertForwards"/>
		<xsl:variable name="forwardNums">
			<xsl:for-each select="$forwards">
				<xsl:element name="forward">
					<xsl:copy-of select="@errorHandling"/>
					<xsl:attribute name="pos" select="position() - 1"/>
				</xsl:element>
			</xsl:for-each>
		</xsl:variable>
		<xsl:for-each-group select="$forwardNums/forward" group-by="@errorHandling">
			<xsl:text>	linkStyle </xsl:text>
			<xsl:value-of select="current-group()/@pos" separator=","/>
			<xsl:choose>
				<xsl:when test="xs:boolean(current-grouping-key())">
					<xsl:text> stroke:#ec4758,stroke-width:3px,fill:none;</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text> stroke:#8bc34a,stroke-width:3px,fill:none;</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:text>&#10;</xsl:text>
		</xsl:for-each-group>
	</xsl:template>

	<xsl:template match="*" mode="resolveExits">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates select="*" mode="#current"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipeline" mode="resolveExits">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<exits>
				<xsl:apply-templates select="//exit" mode="#current"/>
				<xsl:if test="not(//exit[lower-case(@state) = 'success'])">
					<exit name="READY" state="success"/>
				</xsl:if>
			</exits>
			<xsl:copy-of select="*[not(name() = ('exit','exits'))]"/>
		</xsl:copy>
	</xsl:template>

	<!--Copy exit but replace path with name attribute-->
	<xsl:template match="exit" mode="resolveExits">
		<xsl:element name="exit">
			<xsl:attribute name="name" select="(@name,@path)[1]"/>
			<xsl:copy-of select="@*[not(name() = ('path', 'name'))]"/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="*" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="defaultCopyActions"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="attribute" mode="preprocess"></xsl:template>
	<xsl:template match="sender" mode="preprocess"></xsl:template>

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
				<xsl:attribute name="errorHandling" select="'false'"/>
			</xsl:element>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipeline" mode="preprocess">
		<xsl:param name="firstElementID"/>
		<!-- Modify the pipeline in the following ways:
			- Create all exits that are used by the pipeline
			- Add a unique ID on all elements, and add that ID to the forwards pointing to that element
			- Add implicit forwards and global-forwards explicitly to inputValidator -and Wrapper and all pipes
			- For each exit that is used by the pipeline, make an outputWrapper -and Validator if they originally existed
			- Recursively go through pipeline to determine things like errorHandling.
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
				<xsl:for-each select="./exits/exit[@name = $elementsWithExplicitForwards//forward/@path]">
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
					<xsl:apply-templates select="./exits/exit" mode="#current"/>
				</xsl:with-param>
			</xsl:apply-templates>
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
			<xsl:attribute name="elementID" select="concat(generate-id(), '-', $exit/@name)"/>
			<xsl:attribute name="name" select="'OutputWrapper'"/>
			<xsl:apply-templates select="@*|*" mode="#current"/>
			<xsl:call-template name="styleElement"/>
			<!-- Add success forward -->
			<xsl:call-template name="createForward">
				<xsl:with-param name="name" select="'success'"/>
				<xsl:with-param name="path" select="$exit/@name"/>
			</xsl:call-template>
		</xsl:copy>
	</xsl:template>

	<xsl:template name="preprocessOutputValidator">
		<xsl:param name="exit"/>

		<xsl:element name="outputValidator">
			<xsl:attribute name="elementID" select="concat(generate-id(), '-', $exit/@name)"/>
			<xsl:apply-templates select="@*|*[name() != 'forward']" mode="#current"/>
			<xsl:call-template name="styleElement"/>
			<xsl:apply-templates select="forward|../global-forwards/forward[not(@name = current()/forward/@name)]" mode="#current">
				<xsl:with-param name="parentName" select="'outputValidator'"/>
			</xsl:apply-templates>
			<!-- Add success forward -->
			<xsl:element name="forward">
				<xsl:attribute name="name" select="'success'"/>
				<xsl:attribute name="customText" select="$exit/@name" />
				<xsl:attribute name="path" select="$exit/@name" />
				<xsl:attribute name="targetID" select="generate-id($exit)"/>
			</xsl:element>
		</xsl:element>
	</xsl:template>

	<xsl:template match="pipe" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="defaultPipeCopyActions"/>

			<xsl:for-each select="descendant::sender[@className='org.frankframework.senders.IbisLocalSender']">
				<xsl:call-template name="IbisLocalSender"/>
			</xsl:for-each>
			<!-- Add success forward if not present -->
			<xsl:call-template name="createForwardIfNecessary">
				<xsl:with-param name="forwards" select="forward"/>
				<xsl:with-param name="name" select="'success'"/>
				<xsl:with-param name="path" select="(following-sibling::pipe/@name,../exits/exit[lower-case(@state)='success']/@name,'READY')[1]"/>
			</xsl:call-template>
		</xsl:copy>
	</xsl:template>

	<!-- The following pipes do not have a success forward, so it should not be added by default -->
	<xsl:template match="pipe[@className=('org.frankframework.pipes.CompareStringPipe',
										 'org.frankframework.pipes.CompareIntegerPipe')]" mode="preprocess">
		<xsl:copy>
			<xsl:call-template name="defaultPipeCopyActions"/>
		</xsl:copy>
	</xsl:template>

	<!-- The XmlIf can have different forward names which then might directly point to a next pipe/exit -->
	<xsl:template match="pipe[@className='org.frankframework.pipes.XmlIf']" mode="preprocess">
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

	<!-- The SwitchPipe can have no forwards, in that case do nothing. Users shouldn't let this happen -->
	<xsl:template match="pipe[@className='org.frankframework.pipes.XmlSwitch' or @className='org.frankframework.pipes.SwitchPipe']" mode="preprocess">
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

		<xsl:variable name="targetReceiver" select="ancestor::*/adapter/receiver[listener[@className='org.frankframework.receivers.JavaListener' and @name=current()/@javaListener]]"/>
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
			<xsl:attribute name="elementID" select="generate-id()"/>
			<xsl:variable name="target" select="$pipeline/(pipe[@name=current()/@path]|./exits/exit[@name=current()/@path])[1]"/>
			<!-- When the forward is an exit, link it to the corresponding outputWrapper or outputValidator -->
			<xsl:choose>
				<xsl:when test="name($target)='exit'">
					<xsl:choose>
						<xsl:when test="not($parentName = ('outputWrapper','outputValidator')) and exists($pipeline/outputWrapper)">
							<xsl:attribute name="targetID" select="concat(generate-id($pipeline/outputWrapper), '-', $target/@name)"/>
<!--							This was used to show what exit this was forwarding to when a validator was made only once for the pipeline.
								Now that a validator is made for every exit, it is no longer necessary to show which exit this is going to.-->
<!--							<xsl:attribute name="customText" select="$target/@name"/>-->
						</xsl:when>
						<xsl:when test="$parentName != 'outputValidator' and exists(($pipeline/outputValidator,$pipeline/inputValidator[@responseRoot]))">
							<xsl:attribute name="targetID" select="concat(generate-id($pipeline/(outputValidator,inputValidator[@responseRoot])[1]), '-', $target/@name)"/>
<!--							<xsl:attribute name="customText" select="$target/@name"/>-->
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
			<xsl:apply-templates select="@*|*" mode="#current"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="*" mode="sort">
		<xsl:param name="finalProcessing" as="xs:boolean" select="true()"/>
		<xsl:param name="errorHandling" as="xs:boolean" select="false()"/>
		<xsl:param name="originalPipes"/>

		<xsl:variable name="newOriginalPipes">
			<xsl:copy-of select="$originalPipes/*[@elementID != current()/@elementID]"/>
		</xsl:variable>

		<xsl:variable name="processedPipes">
			<xsl:variable name="forwards">
				<!--Group forwards by their target, this will ensure that no 2 forwards have the same source and destination-->
				<xsl:for-each-group select="forward" group-by="@targetID">
					<pair>
						<xsl:variable name="target" select="$newOriginalPipes/*[@elementID=current-grouping-key()]"/>
						<xsl:copy>
							<xsl:attribute name="errorHandling" select="$errorHandling or count(current-group()[@name = $errorForwards]) = count(current-group())or $target/type = 'errorhandling'"/>
							<xsl:attribute name="name">
								<xsl:value-of select="current-group()/@name" separator="&lt;br/>"/>
							</xsl:attribute>
							<xsl:copy-of select="current-group()[1]/@*[not(name() = 'name')]"/>
						</xsl:copy>
						<xsl:copy-of select="$target"/>
					</pair>
				</xsl:for-each-group>
			</xsl:variable>

			<xsl:copy>
				<xsl:attribute name="errorHandling" select="$errorHandling"/>
				<xsl:copy-of select="@*|*[name() != 'forward']"/>
				<xsl:copy-of select="$forwards/pair/forward"/>
			</xsl:copy>

			<xsl:for-each select="$forwards/pair">
				<xsl:apply-templates select="*[name() != 'forward']" mode="#current">
					<xsl:with-param name="finalProcessing" select="false()"/>
					<xsl:with-param name="errorHandling" select="forward/@errorHandling"/>
					<xsl:with-param name="originalPipes">
						<xsl:copy-of select="$newOriginalPipes"/>
					</xsl:with-param>
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
		<xsl:apply-templates select="@*|*" mode="#current"/>
		<xsl:call-template name="styleElement"/>
	</xsl:template>

	<xsl:template name="defaultPipeCopyActions">
		<xsl:call-template name="defaultCopyActions"/>
		<xsl:apply-templates select="../global-forwards/forward[not(@name = current()/forward/@name)]" mode="#current"/>
	</xsl:template>

	<xsl:template name="switchPipeCopyActions">
		<xsl:attribute name="elementID" select="generate-id()"/>
		<xsl:apply-templates select="@*|*[name() != 'forward']" mode="#current"/>
		<xsl:call-template name="styleElement"/>
	</xsl:template>

	<xsl:template name="styleElement">
		<xsl:variable name="type" select="$frankElements/*[name()=(current()/@className,current()/name())][1]"/>
		<xsl:choose>
			<xsl:when test="$type/type = 'endpoint' and count(sender) = 1">
				<xsl:variable name="newType" select="$frankElements/*[name()=current()/sender/@className]"/>
				<xsl:copy-of select="($newType,$type)[1]/type"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy-of select="$type/type"/>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="attribute,$type/attribute">
			<xsl:for-each-group select="attribute,$type/attribute" group-by="@name">
				<attribute>
					<xsl:if test="not(current-group()[1]/@showValue)">
						<xsl:attribute name="showValue">true</xsl:attribute>
					</xsl:if>
					<xsl:copy-of select="current-group()[1]/(@*,*)"/>
				</attribute>
			</xsl:for-each-group>
		</xsl:if>
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

	<xsl:template match="*" mode="convertElements">
		<xsl:apply-templates select="*" mode="#current"/>
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
		<xsl:apply-templates select="*" mode="#current">
			<xsl:with-param name="extensive" select="@extensive = 'true'" tunnel="yes"/>
		</xsl:apply-templates>
		<xsl:if test="number($adapterCount) gt 1">
			<xsl:text>	end&#10;</xsl:text>
		</xsl:if>
	</xsl:template>

	<xsl:template match="receiver" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="text" select="(@name,'Receiver')[1]"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="inputValidator" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="text" select="(@name,'InputValidator')[1]"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="inputWrapper" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="text" select="(@name,'InputWrapper')[1]"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="outputWrapper" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="text" select="(@name,'OutputWrapper')[1]"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="outputValidator" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="text" select="(@name,'OutputValidator')[1]"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="pipe" mode="convertElements">
		<xsl:call-template name="createMermaidElement"/>
	</xsl:template>

	<xsl:template match="exit" mode="convertElements">
		<xsl:call-template name="createMermaidElement">
			<xsl:with-param name="text" select="@state"/>
			<xsl:with-param name="subText" select="@code"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:variable name="shapeStartMap">
		<field type="endpoint"><![CDATA[>]]></field>
		<field type="validator">([</field>
		<field type="wrapper">[[</field>
		<field type="translator">(</field>
		<field type="iterator">[/</field>
		<field type="router">{{</field>
		<field type="session">[/</field>
		<field type="errorhandling">[</field>
		<field type="database">[(</field>
	</xsl:variable>
	<xsl:variable name="shapeEndMap">
		<field type="endpoint">]</field>
		<field type="validator">])</field>
		<field type="wrapper">]]</field>
		<field type="translator">)</field>
		<field type="iterator">\]</field>
		<field type="router">}}</field>
		<field type="session">/]</field>
		<field type="errorhandling">]</field>
		<field type="database">)]</field>
	</xsl:variable>
	<xsl:template name="createMermaidElement">
		<xsl:param name="text" select="xs:string((@name,name())[1])"/>
		<xsl:param name="subText" select="(listener/@className,sender/@className,@className)[1]"/>
		<xsl:param name="extensive" tunnel="yes" select="'false'"/>

		<xsl:text>	</xsl:text>
		<xsl:value-of select="@elementID"/>
		<xsl:value-of select="($shapeStartMap/field[@type = current()/type],'(')[1]"/>
		<xsl:text>"<![CDATA[<b>]]></xsl:text>
		<xsl:value-of select="$text"/>
		<xsl:text><![CDATA[</b>]]></xsl:text>
		<xsl:if test="$subText != ''">
			<xsl:text><![CDATA[<br/>]]></xsl:text>
			<xsl:if test="$extensive and contains($subText, '.')">
				<xsl:text><![CDATA[<a style='color:#909090;'>]]></xsl:text>
				<xsl:value-of select="tokenize($subText, '\.')[position() != last()]" separator="."/>
				<xsl:text>.<![CDATA[</a>]]></xsl:text>
			</xsl:if>
			<xsl:value-of select="tokenize($subText, '\.')[last()]"/>
		</xsl:if>
		<xsl:if test="$extensive">
			<xsl:choose>
				<xsl:when test="@getInputFromFixedValue != ''">
					<xsl:text><![CDATA[<br/>]]></xsl:text>
					<xsl:text>fixed input: </xsl:text>
					<xsl:text><![CDATA[<i>]]></xsl:text>
					<xsl:value-of select="replace(@getInputFromFixedValue, '&lt;', '&amp;lt;')"/>
					<xsl:text><![CDATA[</i>]]></xsl:text>
				</xsl:when>
				<xsl:when test="@getInputFromSessionKey != ''">
					<xsl:text><![CDATA[<br/>]]></xsl:text>
					<xsl:text>input sessionKey: </xsl:text>
					<xsl:text><![CDATA[<i>]]></xsl:text>
					<xsl:value-of select="@getInputFromSessionKey"/>
					<xsl:text><![CDATA[</i>]]></xsl:text>
				</xsl:when>
			</xsl:choose>
			<xsl:if test="attribute">
				<xsl:for-each select="@*[name() = current()/attribute/@name]">
					<xsl:text><![CDATA[<br/>]]></xsl:text>
					<xsl:variable name="specialAttr" select="../attribute[@name = current()/name()][1]"/>
					<xsl:value-of select="if($specialAttr/@text) then ($specialAttr/@text) else (concat($specialAttr/@name,': '))"/>
					<xsl:if test="$specialAttr/@showValue = 'true'">
						<xsl:text><![CDATA[<i>]]></xsl:text>
						<xsl:value-of select="."/>
						<xsl:text><![CDATA[</i>]]></xsl:text>
					</xsl:if>
				</xsl:for-each>
			</xsl:if>
			<xsl:if test="@storeResultInSessionKey != ''">
				<xsl:text><![CDATA[<br/>]]></xsl:text>
				<xsl:text>output sessionKey: </xsl:text>
				<xsl:text><![CDATA[<i>]]></xsl:text>
				<xsl:value-of select="@storeResultInSessionKey"/>
				<xsl:text><![CDATA[</i>]]></xsl:text>
			</xsl:if>
			<xsl:if test="@preserveInput = 'true'">
				<xsl:text><![CDATA[<br/>]]></xsl:text>
				<xsl:text>replaces result with computed pipe input</xsl:text>
			</xsl:if>
		</xsl:if>
		<xsl:text>"</xsl:text>
		<xsl:value-of select="($shapeEndMap/field[@type = current()/type],')')[1]"/>
		<xsl:text>:::</xsl:text>
		<xsl:choose>
			<xsl:when test="xs:boolean(@errorHandling)">
				<xsl:text>errorOutline</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>normal</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>

	<xsl:template match="forward" mode="convertForwards">
		<xsl:text>	</xsl:text>
		<xsl:value-of select="parent::*/@elementID"/>
		<xsl:text> --> |</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:if test="exists(@customText)">
			<xsl:text><![CDATA[<br/>]]></xsl:text>
			<xsl:value-of select="@customText"/>
		</xsl:if>
		<xsl:text>| </xsl:text>
		<xsl:value-of select="@targetID"/>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
