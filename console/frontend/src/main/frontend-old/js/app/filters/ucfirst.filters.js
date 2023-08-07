import { appModule } from "../app.module";

appModule.filter('ucfirst', function () {
	return function (s) {
		return (angular.isString(s) && s.length > 0) ? s[0].toUpperCase() + s.substr(1).toLowerCase() : s;
	};
});
