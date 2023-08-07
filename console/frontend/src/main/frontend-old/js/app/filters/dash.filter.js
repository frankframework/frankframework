import { appModule } from "../app.module";

appModule.filter('dash', function () {
	return function (input) {
		if (input || input === 0) return input;
		else return "-";
	};
});
