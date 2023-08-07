import { appModule } from "../app.module";

appModule.filter('formatStatistics', function () {
	return function (input, format) {
		if (!input || !format) return; //skip when no input
		var formatted = {};
		for (const key in format) {
			var value = input[key];
			if (!value && value !== 0) { // if no value, return a dash
				value = "-";
			}
			if ((key.endsWith("ms") || key.endsWith("B")) && value != "-") {
				value += "%";
			}
			formatted[key] = value;
		}
		formatted["$$hashKey"] = input["$$hashKey"]; //Copy the hashKey over so Angular doesn't trigger another digest cycle
		return formatted;
	};
});
