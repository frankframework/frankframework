import { appModule } from "../app.module";

appModule.filter('truncate', function () {
	return function (input, length) {
		if (input && input.length > length) {
			return input.substring(0, length) + "... (" + (input.length - length) + " characters more)";
		}
		return input;
	};
});
