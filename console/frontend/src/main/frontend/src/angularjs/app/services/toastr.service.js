import { appModule } from "../app.module";

appModule.service('Toastr', ['toaster', function (toaster) {
	function sendToast(type, title, text){
		var options = { type: type, title: title, body: text };
		if (angular.isObject(title)) {
			angular.merge(options, options, title);
		}
		toaster.pop(options);
	}

	// easy place to do lambdas
	this.error = function (title, text) {
		return sendToast('error', title, text);
	};
	this.success = function (title, text) {
		return sendToast('success', title, text);
	};
	this.warning = function (title, text) {
		return sendToast('warning', title, text);
	}
}]);
