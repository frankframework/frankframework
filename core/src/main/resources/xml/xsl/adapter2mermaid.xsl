<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0">
	<xsl:output method="text" indent="yes" omit-xml-declaration="yes"/>

	<xsl:variable name="adapterCount" select="count(//adapter)"/>
	<xsl:variable name="pipeTypes" select="document('pipeTypes.xml')/root"/>
	<xsl:variable name="defaultStyle">normal</xsl:variable>
	<xsl:variable name="errorForwards" select="('exception','failure','fail','timeout','illegalResult','presumedTimeout','interrupt','parserError','outputParserError','outputFailure')"/>

	<xsl:template match="/">
		<!-- Create the Mermaid graph in 2 steps
			- First preprocess adapters, putting pipes in the correct order, explicitly adding implicit forwards and preprocess input and output validators and wrappers
			- Then convert the adapter to mermaid code-->
		<xsl:variable name="preproccessedAdapter">
			<xsl:apply-templates select="*" mode="preprocess"/>
		</xsl:variable>

		<xsl:text>graph&#10;</xsl:text>
		<xsl:apply-templates select="$preproccessedConfiguration" mode="convertElements"/>
		<xsl:text>	classDef normal fill:#fff,stroke-width:2px,stroke:#4FD365;&#10;</xsl:text>
		<xsl:text>	classDef errorOutline stroke:#D13030;&#10;</xsl:text>
		<xsl:text>	classDef endpoint fill:#48C0B7;&#10;</xsl:text>
		<xsl:text>	classDef validator fill:#D6FBD1;&#10;</xsl:text>
		<xsl:text>	classDef wrapper fill:#CBF4FC;&#10;</xsl:text>
		<xsl:text>	classDef router fill:#F8C243;&#10;</xsl:text>
		<xsl:text>	classDef iterator fill:#E479D1;&#10;</xsl:text>
		<xsl:text>	classDef translator fill:#3EABE4;&#10;</xsl:text>
		<xsl:variable name="elements" select="$preproccessedConfiguration//(receiver|inputValidator|inputWrapper|pipe|outputWrapper|outputValidator|exit)"/>
		<xsl:for-each-group select="$elements" group-by="@style">
			<xsl:text>	class </xsl:text>
			<xsl:value-of select="current-group()/@elementID" separator=","/>
			<xsl:text> </xsl:text>
			<xsl:value-of select="current-grouping-key()"/>
			<xsl:text>;&#10;</xsl:text>
		</xsl:for-each-group>
		<xsl:variable name="forwards" select="$preproccessedConfiguration//forward"/>
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
					<xsl:text> stroke:#D13030,fill:none;</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text> stroke:#4FD365,fill:none;</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:text>&#10;</xsl:text>
		</xsl:for-each-group>
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
				<xsl:attribute name="errorHandling" select="'false'"/>
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

			<xsl:variable name="usedExits" select=".//exit[@path = $elementsWithExplicitForwards//forward/@path]"/>

			<xsl:variable name="pipelineWithExplicitForwards">
				<xsl:copy-of select="$elementsWithExplicitForwards/*"/>
				<!-- Create outputwrappers and outputvalidators per exit -->
				<xsl:for-each select="$usedExits">
					<xsl:apply-templates select="ancestor::pipeline/outputWrapper" mode="#current">
						<xsl:with-param name="exit" select="."/>
					</xsl:apply-templates>
					<xsl:apply-templates select="ancestor::pipeline/outputValidator" mode="#current">
						<xsl:with-param name="exit" select="."/>
					</xsl:apply-templates>
					<xsl:apply-templates select="." mode="#current"/>
				</xsl:for-each>
			</xsl:variable>

			<xsl:apply-templates select="$pipelineWithExplicitForwards/*[@elementID=$firstElementID]" mode="sort">
				<xsl:with-param name="originalPipes">
					<xsl:copy-of select="$pipelineWithExplicitForwards/*"/>
				</xsl:with-param>
			</xsl:apply-templates>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="pipeline/inputValidator" mode="preprocess">
		<xsl:param name="firstPipe"/>

		<xsl:copy>
			<xsl:call-template name="defaultPipeCopyActions"/>
			<!-- Add success forward -->
			<xsl:element name="forward">
				<xsl:attribute name="name" select="'success'"/>
				<xsl:choose>
					<xsl:when test="exists(../inputWrapper)">
						<xsl:attribute name="path" select="generate-id(../inputWrapper)"/>
						<xsl:attribute name="targetID" select="generate-id(../inputWrapper)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="path" select="$firstPipe/@name"/>
						<xsl:attribute name="targetID" select="generate-id($firstPipe)"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="pipeline/inputWrapper" mode="preprocess">
		<xsl:param name="firstPipe"/>

		<xsl:copy>
			<xsl:call-template name="defaultCopyActions"/>
			<!-- Add success forward -->
			<xsl:element name="forward">
				<xsl:attribute name="name" select="'success'"/>
				<xsl:attribute name="path" select="$firstPipe/@name"/>
				<xsl:attribute name="targetID" select="generate-id($firstPipe)"/>
			</xsl:element>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipeline/outputWrapper" mode="preprocess">
		<xsl:param name="exit"/>

		<xsl:copy>
			<xsl:attribute name="elementID" select="concat(generate-id(), '-', $exit/@path)"/>
			<xsl:call-template name="stylePipe"/>
			<xsl:attribute name="name" select="'OutputWrapper'"/>
			<xsl:apply-templates select="*|@*" mode="#current"/>
			<!-- Add success forward -->
			<xsl:element name="forward">
				<xsl:attribute name="name" select="'success'"/>
				<xsl:choose>
					<xsl:when test="exists(../outputValidator)">
						<xsl:attribute name="customText" select="$exit/@path" />
						<xsl:attribute name="path" select="generate-id(../outputValidator)"/>
						<xsl:attribute name="targetID" select="concat(generate-id(../outputValidator), '-', $exit/@path)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:attribute name="path" select="$exit/@path" />
						<xsl:attribute name="targetID" select="generate-id($exit)"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:element>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipeline/outputValidator" mode="preprocess">
		<xsl:param name="exit"/>

		<xsl:copy>
			<xsl:attribute name="elementID" select="concat(generate-id(), '-', $exit/@path)"/>
			<xsl:call-template name="stylePipe"/>
			<xsl:attribute name="name" select="'OutputValidator'"/>
			<xsl:apply-templates select="*|@*" mode="#current"/>
			<xsl:apply-templates select="../global-forwards/forward[not(@name = current()/forward/@name)]" mode="#current"/>
			<!-- Add success forward -->
			<xsl:element name="forward">
				<xsl:attribute name="name" select="'success'"/>
				<xsl:attribute name="customText" select="$exit/@path" />
				<xsl:attribute name="path" select="$exit/@path" />
				<xsl:attribute name="targetID" select="generate-id($exit)"/>
			</xsl:element>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="pipe" mode="preprocess">
		<xsl:param name="createDefaultSuccessForward" as="xs:boolean" select="true()"/>

		<xsl:copy>
			<xsl:call-template name="defaultPipeCopyActions"/>
			<!-- Add success forward if not present -->
			<xsl:if test="$createDefaultSuccessForward and empty(forward[@name='success'])">
				<xsl:variable name="nextTarget" select="(following-sibling::pipe,..//exit[@state='success'])[1]"/>
				<xsl:call-template name="createForward">
					<xsl:with-param name="name" select="'success'"/>
					<xsl:with-param name="path" select="($nextTarget/@name,$nextTarget/@path)[1]"/>
				</xsl:call-template>
			</xsl:if>
		</xsl:copy>
	</xsl:template>
	
	<!-- The following pipes do not have a success forward, so it should not be added by default -->
	<xsl:template match="pipe[@className=('nl.nn.adapterframework.pipes.CompareStringPipe',
										 'nl.nn.adapterframework.pipes.CompareIntegerPipe')]"
		mode="preprocess">
		<xsl:next-match>
			<xsl:with-param name="createDefaultSuccessForward" select="false()"/>
		</xsl:next-match>
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

			<xsl:if test="empty($forwards/forward[@name=$thenForwardName])">
				<xsl:call-template name="createForward">
					<xsl:with-param name="name" select="'then'"/>
					<xsl:with-param name="path" select="$thenForwardName"/>
				</xsl:call-template>
			</xsl:if>

			<xsl:if test="empty($forwards/forward[@name=$elseForwardName])">
				<xsl:call-template name="createForward">
					<xsl:with-param name="name" select="'else'"/>
					<xsl:with-param name="path" select="$elseForwardName"/>
				</xsl:call-template>
			</xsl:if>
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
			
			<xsl:variable name="notFoundForwardName" select="@notFoundForwardName"/>
			<xsl:if test="exists($notFoundForwardName) and empty($forwards/forward[@name=$notFoundForwardName])">
				<xsl:call-template name="createForward">
					<xsl:with-param name="name" select="'notFoundForwardName'"/>
					<xsl:with-param name="path" select="$notFoundForwardName"/>
				</xsl:call-template>
			</xsl:if>
			
			<xsl:variable name="emptyForwardName" select="@emptyForwardName"/>
			<xsl:if test="exists($emptyForwardName) and empty($forwards/forward[@name=$emptyForwardName])">
				<xsl:call-template name="createForward">
					<xsl:with-param name="name" select="'emptyForwardName'"/>
					<xsl:with-param name="path" select="$emptyForwardName"/>
				</xsl:call-template>
			</xsl:if>
		</xsl:copy>
	</xsl:template>
	
	<!-- For local senders, add a 'forward' to the sub-adapter -->
	<xsl:template match="sender[@className='nl.nn.adapterframework.senders.IbisLocalSender']" mode="preprocess">
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
		<xsl:copy>
			<xsl:attribute name="elementID" select="generate-id()"/>
			<xsl:variable name="target" select="$pipeline/(pipe[@name=current()/@path]|exits/exit[@path=current()/@path])[1]"/>
			<!-- When the forward is an exit, link it to the corresponding outputWrapper or outputValidator -->
			<xsl:choose>
				<xsl:when test="local-name($target)='exit'">
					<xsl:attribute name="customText" select="$target/@path"/>
					<xsl:choose>
						<xsl:when test="not(local-name(parent::*) = ('outputWrapper', 'outputValidator')) and exists($pipeline/outputWrapper)">
							<xsl:attribute name="targetID" select="concat(generate-id($pipeline/outputWrapper), '-', $target/@path)"/>
						</xsl:when>
						<xsl:when test="not(local-name(parent::*) = 'outputValidator') and exists($pipeline/outputValidator)">
							<xsl:attribute name="targetID" select="concat(generate-id($pipeline/outputValidator), '-', $target/@path)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="targetID" select="generate-id($target)"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise><xsl:attribute name="targetID" select="generate-id($target)"/></xsl:otherwise>
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
				<xsl:copy-of select="@*|*[local-name() != 'forward']"/>
				<xsl:for-each select="forward">
					<xsl:copy>
						<xsl:attribute name="errorHandling" select="$errorHandling = true() or @name = $errorForwards"/>
						<xsl:copy-of select="@*|*"/>
					</xsl:copy>
				</xsl:for-each>
			</xsl:copy>

			<xsl:for-each select="forward">
				<xsl:variable name="errorHandling" select="$errorHandling = true() or @name = $errorForwards"/>
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

		<!-- When processing the pipes via forwards, loops might result in pipes being duplicated, so sort them by errorHandling only keep the first of that-->
		<xsl:choose>
			<xsl:when test="$finalProcessing">
				<xsl:for-each-group select="$processedPipes/*" group-by="@elementID">
					<xsl:variable name="sorted">
						<xsl:for-each select="current-group()">
							<xsl:sort select="@errorHandling"/>
							<xsl:copy-of select="."/>
						</xsl:for-each>
					</xsl:variable>
					<xsl:copy-of select="$sorted/*[1]"/>
				</xsl:for-each-group>
			</xsl:when>
			<xsl:otherwise><xsl:copy-of select="$processedPipes"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="defaultCopyActions">
		<xsl:attribute name="elementID" select="generate-id()"/>
		<xsl:call-template name="stylePipe"/>
		<xsl:apply-templates select="*|@*" mode="#current"/>
	</xsl:template>
	
	<xsl:template name="defaultPipeCopyActions">
		<xsl:call-template name="defaultCopyActions"/>
		<xsl:apply-templates select="../global-forwards/forward[not(@name = current()/forward/@name)]" mode="#current"/>
	</xsl:template>

	<xsl:template name="switchPipeCopyActions">
		<xsl:attribute name="elementID" select="generate-id()"/>
		<xsl:call-template name="stylePipe"/>
		<xsl:apply-templates select="*[local-name() != 'forward']|@*" mode="#current"/>
	</xsl:template>

	<xsl:template name="stylePipe">
		<xsl:variable name="pipeType" select="($pipeTypes/*[name()=(current()/@className,current()/local-name())]|$defaultStyle)[1]"/>
		<xsl:attribute name="style" select="$pipeType"/>
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
		<xsl:apply-templates select="$forward" mode="#current">
			<!--Scope is the variable which has no parent, so explicitly pass pipeline parameter-->
			<xsl:with-param name="pipeline" select="ancestor::pipeline"/>
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template match="*|@*|text()" mode="convertElements">
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
		<xsl:apply-templates select="*|@*|text()" mode="convertElements"/>
		<xsl:if test="number($adapterCount) gt 1">
			<xsl:text>	end&#10;</xsl:text>
		</xsl:if>
	</xsl:template>

	<xsl:template match="receiver" mode="convertElements">
		<xsl:text>	</xsl:text>
		<xsl:value-of select="@elementID"/>
		<xsl:text>{{Receiver</xsl:text>
		<xsl:text>&lt;br/></xsl:text>
		<xsl:value-of select="tokenize(listener/@className, '\.')[last()]"/>
		<xsl:text>}}:::normal&#10;</xsl:text>
	</xsl:template>

	<xsl:template match="inputValidator" mode="convertElements">
		<xsl:text>	</xsl:text>
		<xsl:value-of select="@elementID"/>
		<xsl:text>([InputValidator&lt;br/></xsl:text>
		<xsl:value-of select="tokenize(@className, '\.')[last()]"/>
		<xsl:text>]):::normal&#10;</xsl:text>
	</xsl:template>

	<xsl:template match="outputValidator" mode="convertElements">
		<xsl:text>	</xsl:text>
		<xsl:value-of select="@elementID"/>
		<xsl:text>([OutputValidator&lt;br/></xsl:text>
		<xsl:value-of select="tokenize(@className, '\.')[last()]"/>
		<xsl:text>])</xsl:text>
		<xsl:apply-templates select="@errorHandling"/>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>

	<xsl:template match="inputWrapper" mode="convertElements">
		<xsl:text>	</xsl:text>
		<xsl:value-of select="@elementID"/>
		<xsl:text>([InputWrapper&lt;br/></xsl:text>
		<xsl:value-of select="tokenize(@className, '\.')[last()]"/>
		<xsl:text>]):::normal&#10;</xsl:text>
	</xsl:template>

	<xsl:template match="outputWrapper" mode="convertElements">
		<xsl:text>	</xsl:text>
		<xsl:value-of select="@elementID"/>
		<xsl:text>([OutputWrapper&lt;br/></xsl:text>
		<xsl:value-of select="tokenize(@className, '\.')[last()]"/>
		<xsl:text>])</xsl:text>
		<xsl:apply-templates select="@errorHandling"/>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>

	<xsl:template match="pipe" mode="convertElements">
		<xsl:variable name="isSwitch" select="@className='nl.nn.adapterframework.pipes.XmlSwitch'
											or@className='nl.nn.adapterframework.pipes.CompareIntegerPipe'
											or@className='nl.nn.adapterframework.pipes.CompareStringPipe'
											or@className='nl.nn.adapterframework.pipes.FilenameSwitch'
											or@className='nl.nn.adapterframework.pipes.XmlIf'"/>
		<xsl:text>	</xsl:text>
		<xsl:value-of select="@elementID"/>
		<xsl:value-of select="if ($isSwitch) then ('{') else ('(')"/>
		<xsl:value-of select="@name"/>
		<xsl:text>&lt;br/></xsl:text>
		<xsl:choose>
			<xsl:when test="sender">
				<xsl:value-of select="tokenize(sender/@className, '\.')[last()]"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="tokenize(@className, '\.')[last()]"/>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="if ($isSwitch) then ('}') else (')')"/>
		<xsl:apply-templates select="@errorHandling"/>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>

	<xsl:template match="exit" mode="convertElements">
		<xsl:text>	</xsl:text>
		<xsl:value-of select="@elementID"/>
		<xsl:text>{{</xsl:text>
		<xsl:value-of select="@state"/>
		<xsl:if test="@code">
			<xsl:text>&lt;br/></xsl:text>
			<xsl:value-of select="@code"/>
		</xsl:if>
		<xsl:text>}}</xsl:text>
		<xsl:apply-templates select="@errorHandling"/>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>

	<xsl:template match="@errorHandling">
		<xsl:text>:::</xsl:text>
		<xsl:choose>
			<xsl:when test="xs:boolean(.)">
				<xsl:text>errorOutline</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>normal</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="forward" mode="convertForwards">
		<xsl:text>	</xsl:text>
		<xsl:value-of select="parent::*/@elementID"/>
		<xsl:text> --> |</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:if test="exists(@customText)">
			<xsl:text>&lt;br/></xsl:text>
			<xsl:value-of select="@customText"/>
		</xsl:if>
		<xsl:text>| </xsl:text>
		<xsl:value-of select="@targetID"/>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
