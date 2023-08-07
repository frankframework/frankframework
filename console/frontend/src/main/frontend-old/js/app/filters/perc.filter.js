import { appModule } from "../app.module";

appModule.filter('perc', function () {
	return function (input) {
		if (input || input === 0) return input + "%";
		else return "-";
	};
});
