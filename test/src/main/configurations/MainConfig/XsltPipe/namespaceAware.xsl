<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:tom="http://www.frankframework.org/tom">
	<xsl:output method="xml" encoding="UTF-8" indent="yes" />
	<xsl:variable name="codeLookup" select="document('Domains.xml')" />

	<xsl:template match="/tom:request">
		<ServiceResponse>
			<Body>
				<xsl:apply-templates select="tom:Party" />
			</Body>
		</ServiceResponse>
	</xsl:template>

	<xsl:template match="tom:Party">
		<xsl:variable name="partyType" select="tom:PartyType" />
		<Party>
			<xsl:if test="tom:Id != ''">
				<partyRef>
					<xsl:value-of select="tom:Id" />
				</partyRef>
			</xsl:if>

			<xsl:if test="$partyType != ''">
				<partyTypeCD>
					<xsl:value-of
						select="$codeLookup/Domains/PartyTypes/PartyTypeMapping[partyType = $partyType]/partyTypeCD" />
				</partyTypeCD>
			</xsl:if>

			<xsl:apply-templates select="tom:Organisation" />
			<xsl:apply-templates select="tom:Person" />
			<xsl:apply-templates select="tom:PostalAddressContactPointUsage" />
		</Party>
	</xsl:template>

	<xsl:template match="tom:Organisation">
		<xsl:if test="tom:OrganisationName/tom:Name != ''">
			<Organisation>
				<organisationName>
					<xsl:value-of select="tom:OrganisationName/tom:Name" />
				</organisationName>
			</Organisation>
		</xsl:if>
	</xsl:template>

	<xsl:template match="tom:Person">
		<xsl:variable name="gender" select="tom:Demographics/tom:Gender" />
		<Person>
			<xsl:if test="tom:Demographics/tom:BirthDate != ''">
				<dob>
					<xsl:value-of select="tom:Demographics/tom:BirthDate" />
				</dob>
			</xsl:if>

			<xsl:if test="tom:PersonName/tom:Initials != ''">
				<forename>
					<xsl:value-of select="tom:PersonName/tom:Initials" />
				</forename>
			</xsl:if>

			<xsl:if test="tom:PersonName/tom:LastNamePrefix != ''">
				<middleName>
					<xsl:value-of select="tom:PersonName/tom:LastNamePrefix" />
				</middleName>
			</xsl:if>

			<!-- Defect 46: omzetten TOM titel code naar AppleJuice titel code -->
			<xsl:variable name="postTitleCode"
				select="tom:Titles[tom:TitleType='3' and tom:TitlePosition='11']/tom:Title" />
			<xsl:if
				test="tom:PersonName/tom:Titles[tom:TitleType='3' and tom:TitlePosition='11']/tom:Title != ''">
				<postTitleCD>
					<xsl:value-of
						select="$codeLookup/Domains/titles/posttitle[tom = $postTitleCode]/applejuice" />
				</postTitleCD>
			</xsl:if>

			<!-- Defect 46: omzetten TOM titel code naar AppleJuice titel code -->
			<xsl:variable name="preTitleCode"
				select="tom:PersonName/tom:Titles[tom:TitleType='3' and tom:TitlePosition='1']/tom:Title" />
			<xsl:if
				test="tom:PersonName/tom:Titles[tom:TitleType='3' and tom:TitlePosition='1']/tom:Title != ''">
				<preTitleCD>
					<xsl:value-of
						select="$codeLookup/Domains/titles/pretitle[tom = $preTitleCode]/applejuice" />
				</preTitleCD>
			</xsl:if>

			<xsl:if test="$gender != ''">
				<sexCD>
					<xsl:value-of
						select="$codeLookup/Domains/genders/genderMapping[gender = $gender]/sexCD" />
				</sexCD>
			</xsl:if>

			<xsl:if test="tom:PersonName/tom:BasicLastName != ''">
				<surname>
					<xsl:value-of select="tom:PersonName/tom:BasicLastName" />
				</surname>
			</xsl:if>

		</Person>
	</xsl:template>

	<xsl:template match="tom:PostalAddressContactPointUsage">
		<xsl:variable name="contactPointUsageType" select="tom:ContactPointUsageType" />
		<xsl:variable name="houseNumberAndAddition">
			<xsl:if test="tom:PostalAddress/tom:HouseNumber != ''">
				<xsl:choose>
					<xsl:when test="tom:PostalAddress/tom:HouseNumberAddition != ''">
						<xsl:value-of
							select="concat(tom:PostalAddress/tom:HouseNumber, ' ', tom:PostalAddress/tom:HouseNumberAddition)" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="tom:PostalAddress/tom:HouseNumber" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</xsl:variable>
		<Address>
			<xsl:if test="tom:PostalAddress/tom:Id != ''">
				<addressRef>
					<xsl:value-of select="tom:PostalAddress/tom:Id" />
				</addressRef>
			</xsl:if>

			<xsl:if test="$contactPointUsageType != ''">
				<addressTypeCD>
					<xsl:value-of
						select="$codeLookup/Domains/ContactPointUsageTypes/ContactPointUsageTypeMapping[contactPointUsage = $contactPointUsageType]/adresTypeCD" />
				</addressTypeCD>
			</xsl:if>

			<xsl:if test="tom:PostalAddress/tom:CareOfName != ''">
				<careOfName>
					<xsl:value-of select="tom:PostalAddress/tom:CareOfName" />
				</careOfName>
			</xsl:if>

			<xsl:if test="tom:PostalAddress/tom:CountryCode != ''">
				<countryCD>
					<xsl:value-of select="tom:PostalAddress/tom:CountryCode" />
				</countryCD>
			</xsl:if>

			<xsl:if test="tom:PostalAddress/tom:Street != ''">
				<line1>
					<xsl:value-of select="tom:PostalAddress/tom:Street" />
				</line1>
			</xsl:if>

			<xsl:if test="$houseNumberAndAddition != '' ">
				<line2>
					<xsl:value-of select="$houseNumberAndAddition" />
				</line2>
			</xsl:if>

			<xsl:if test="tom:PostalAddress/tom:City != ''">
				<line3>
					<xsl:value-of select="tom:PostalAddress/tom:City" />
				</line3>
			</xsl:if>

			<xsl:if test="tom:PostalAddress/tom:Postalcode != ''">
				<postcode>
					<xsl:value-of select="tom:PostalAddress/tom:Postalcode" />
				</postcode>
			</xsl:if>

		</Address>
	</xsl:template>


</xsl:stylesheet>