/*
   Copyright 2018 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.ladybug.metadata;

import java.util.HashMap;
import java.util.Map;

import nl.nn.testtool.MetadataFieldExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.metadata.DefaultValueMetadataFieldExtractor;

/**
 * @author Jaco de Groot
 */
public class JuiceServiceName extends DefaultValueMetadataFieldExtractor {
	private MetadataFieldExtractor serviceTypeExtractor;
	private static final Map serviceNames = new HashMap();
	static {
		// Based on ING_REQ_tabel.doc received from Hou, K.W. (Kwok Wah) on 20100616
		// Updated by Dorus van der Kroft on 20101228
		// servicenames_ibis4juice.txt
		serviceNames.put("ING_REQ1002", "getProposalClients");
		serviceNames.put("ING_REQ1003", "getAgent");
		serviceNames.put("ING_REQ1007", "getPolicyClients");
		serviceNames.put("ING_REQ1009", "setPolicyCommissionDetails");
		serviceNames.put("ING_REQ1010", "setPolicyCollectionDetails");
		serviceNames.put("ING_REQ1012", "calculateProposal");
		serviceNames.put("ING_REQ1014", "printPolicy");
		serviceNames.put("ING_REQ1015", "setPolicyIntoForce");
		serviceNames.put("ING_REQ1016", "searchPolicyProposalForRoles");
		serviceNames.put("ING_REQ1017", "setRoleOnProposal");
		serviceNames.put("ING_REQ1018", "removeRoleOnProposal");
		serviceNames.put("ING_REQ1019", "calculatePremiumAllocationInfo");
		serviceNames.put("ING_REQ1020", "calculateInitialCharges");
		serviceNames.put("ING_REQ1021", "calculateChangeSplits");
		serviceNames.put("ING_REQ1022", "setPolicyPremData");
		serviceNames.put("ING_REQ1023", "getPolicyClientsForRoles");
		serviceNames.put("ING_REQ1024", "cancelPolicyClients");
		serviceNames.put("ING_REQ1025", "setData");
		serviceNames.put("ING_REQ1026", "getClientFinancialDetails");
		serviceNames.put("ING_REQ1027", "setPolicyChangeCollectionDetails");
		serviceNames.put("ING_REQ1028", "setPolicyClientsAmendment");
		serviceNames.put("ING_REQ1030", "printSwitchUnitsDetailReport");
		serviceNames.put("ING_REQ1031", "printPolicyChange");
		serviceNames.put("ING_REQ1032", "calculateSwitchDetails");
		serviceNames.put("ING_REQ1033", "calculateFuturePolicyValue");
		serviceNames.put("ING_REQ1034", "calculatePolicyChange");
		serviceNames.put("ING_REQ1035", "setPolicyChangeCommissionDetails");
		serviceNames.put("ING_REQ1037", "setPolicySurrender (DISB)");
		serviceNames.put("ING_REQ1038", "getPolicySurrenderCollectionDetails");
		serviceNames.put("ING_REQ1039", "setPolicySurrender (COLL)");
		serviceNames.put("ING_REQ1040", "setRoleOnPolicy");
		serviceNames.put("ING_REQ1041", "setPolicySurrender");
		serviceNames.put("ING_REQ1042", "printPolicyCancellation");
		serviceNames.put("ING_REQ1044", "estimateSurrenderValues");
		serviceNames.put("ING_REQ1045", "calculateSurrenderValues");
		serviceNames.put("ING_REQ1046", "printPolicySurrender");
		serviceNames.put("ING_REQ1050", "printEstimatedFuturePolicyValueQuote");
		serviceNames.put("ING_REQ1051", "calculateCurrentAndProjectedValues");
		serviceNames.put("ING_REQ1052", "printAnnualStatementsSummaryReport");
		serviceNames.put("ING_REQ1053", "printRedirectFuturePremiumsReport");
		serviceNames.put("ING_REQ1054", "printPolicyPUP");
		serviceNames.put("ING_REQ1055", "calculatePUPvalue");
		serviceNames.put("ING_REQ1056", "calculateRedirectFuturePremiumDetails");
		serviceNames.put("ING_REQ1057", "setPUPCommissions");
		serviceNames.put("ING_REQ1058", "notifyDeathOfClientRole");
		serviceNames.put("ING_REQ1059", "deleteDeathNotification");
		serviceNames.put("ING_REQ1060", "confirmDeathOfClient");
		serviceNames.put("ING_REQ1061", "printDeathClaimForGuardian");
		serviceNames.put("ING_REQ1062", "printDeathClaimForLifeAssured");
		serviceNames.put("ING_REQ1063", "printDeathClaimQuote");
		serviceNames.put("ING_REQ1064", "calculateDeathClaimValue");
		serviceNames.put("ING_REQ1065", "confirmDeathOfLifeAssured(DISB)");
		serviceNames.put("ING_REQ1066", "confirmDeathOfGuardian");
		serviceNames.put("ING_REQ1067", "confirmDeathOfLifeAssured");
		serviceNames.put("ING_REQ1068", "deathNotificationOfClient");
		serviceNames.put("ING_REQ1069", "calculateQuoteForSurrender");
		serviceNames.put("ING_REQ1070", "printPolicyQuoteForSurrender");
		serviceNames.put("ING_REQ1073", "getSurrenderQuoteDetails");
		serviceNames.put("ING_REQ1074", "printPolicyForPremiumHoliday");
		serviceNames.put("ING_REQ1075", "setPolicyForPremiumHoliday");
		serviceNames.put("ING_REQ1076", "setPolicyForPremiumHoliday");
		serviceNames.put("ING_REQ1078", "calculatePolicyForPremiumHoliday");
		serviceNames.put("ING_REQ1086", "enquireOnAPolicy");
		serviceNames.put("ING_REQ1088", "setPolicyMaturity (COLL)");
		serviceNames.put("ING_REQ1089", "setPolicyMaturity (DISB)");
		serviceNames.put("ING_REQ1092", "calculateMaturityValue");
		serviceNames.put("ING_REQ1095", "printPolicyMaturity");
		serviceNames.put("ING_REQ1098", "Receive A Premium ");
		serviceNames.put("ING_REQ1100", "calculateHedgingData");
		serviceNames.put("ING_REQ1101", "Issue Policy Via Web");
		serviceNames.put("ING_REQ1103", "sendHedgingPolicyData");
		serviceNames.put("ING_REQ1104", "calculateUnitCorrection");
		serviceNames.put("ING_REQ1105", "printUnitCorrection");
		serviceNames.put("PDG_REQ0007", "getTotalArrearAmount");
		serviceNames.put("PDG_REQ0008", "notifyPolicyArrearsProcessingError");
		serviceNames.put("PDG_REQ0010", "setPremiumDues");
		serviceNames.put("PDG_REQ0011", "setPremiumRefund");
		serviceNames.put("PDG_REQ1004", "Trigger PDG_Premium_Due_Genration ");
		serviceNames.put("PDG_REQ1008", "IssuePolicy");
		serviceNames.put("PDG_REQ1009", "AmendPolicy");
		serviceNames.put("PDG_REQ1012", "setPolicyForPremiumHoliday");
		serviceNames.put("PDG_REQ1013", "getPolicyPremiumsPaid");
		serviceNames.put("PDG_REQ1015", "Trigger PDG_Actuarial_Reporting");
		serviceNames.put("UNISYS_REQ1000", "getClient");
		serviceNames.put("UNISYS_REQ1021", "cancelPolicyPremiums");
		serviceNames.put("UNISYS_REQ1028", "setPolicySuspensionType");
		serviceNames.put("UNISYS_REQ1031", "notifyDeathOfClientRole(COLL)");
		serviceNames.put("UNISYS_REQ1049", "getFirstYearPremDates");
		serviceNames.put("UNISYS_REQ1050", "getNextPremiumDueDate");
		serviceNames.put("UNISYS_REQ1051", "getPolicyPremiumsPaidDue");
		serviceNames.put("UNISYS_REQ1053", "getCurrentAndAmendedNextPremDueDates");
		serviceNames.put("UNISYS_REQ1077", "Report_Actual_Fund_Movements");
		serviceNames.put("UNISYS_REQ1078", "Report_Fund_Movements_Monthly");
		serviceNames.put("UNISYS_REQ1080", "generatePolicyReference");
		// servicenames_ibis4iwa.txt
		serviceNames.put("UNISYS_REQ1083", "calculateIwaPolicyData");
		serviceNames.put("UNISYS_REQ1090", "calculateFinalCompensation");
		serviceNames.put("UNISYS_REQ1093", "calculateNormFundPrices");
		serviceNames.put("UNISYS_REQ1094", "calculateProvisionalLeverageEatingIntoCompensation");
		serviceNames.put("UNISYS_REQ1095", "calculateFinalLeverageEatingIntoCompensation");
		serviceNames.put("UNISYS_REQ1084", "Calculate IWA Compensation Calculation");
		serviceNames.put("UNISYS_REQ1092", "Calculate and Store Norm Fund Prices");
		serviceNames.put("UNISYS_REQ1088", "sendCompensationData");
		serviceNames.put("UNISYS_REQ1085", "Create IWA Starting Point");
		serviceNames.put("UNISYS_REQ1087", "printCompensateInactivePolicies");
		serviceNames.put("UNISYS_REQ1089", "printCompensateActivePolicies");
	}

	public JuiceServiceName() {
		name = "serviceName";
		label = "Service Name";
		shortLabel = "ServiceName";
	}

	public void setServiceTypeExtractor(MetadataFieldExtractor serviceTypeExtractor) {
		this.serviceTypeExtractor = serviceTypeExtractor;
	}

	public Object extractMetadata(Report report) {
		Object serviceType = serviceTypeExtractor.extractMetadata(report);
		String value = (String)serviceNames.get(serviceType);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

}
