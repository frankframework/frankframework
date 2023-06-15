import { appModule } from "../app.module";

appModule.service('Toastr', ['toaster', function (toaster) {
	this.error = function (title, text) {
		var options = { type: 'error', title: title, body: text };
		if (angular.isObject(title)) {
			angular.merge(options, options, title);
		}
		toaster.pop(options);
	};
	this.success = function (title, text) {
		var options = { type: 'success', title: title, body: text };
		if (angular.isObject(title)) {
			angular.merge(options, options, title);
		}
		toaster.pop(options);
	};
}]);
