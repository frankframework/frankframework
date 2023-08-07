import { appModule } from "../app.module";

appModule.filter('variablesFilter', [function () {
	return function (variables, filterText) {
		var returnArray = new Array();

		filterText = filterText.toLowerCase();
		for (const i in variables) {
			var variable = variables[i];
			if (JSON.stringify(variable).toLowerCase().indexOf(filterText) > -1) {
				returnArray.push(variable);
			}
		}

		return returnArray;
	};
}]);
