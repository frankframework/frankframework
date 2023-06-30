import { appModule } from "../app.module";

appModule.filter('dropLastChar', function () {
	return function (input) {
		if (input && input.length > 0) {
			return input.substring(0, input.length - 1);
		}
		return input;
	};
});
