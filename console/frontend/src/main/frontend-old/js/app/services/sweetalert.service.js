import { appModule } from "../app.module";

/* TODO replace with Toastr where possible (warns, info, errorm, success) */
appModule.service('SweetAlert', ['Debug', function (Debug) {
	this.defaultSettings = {
		//			confirmButtonColor: "#449d44"
	};
	this.defaults = function () {
		var args = arguments || [];
		var options = angular.copy(this.defaultSettings);

		if (args.length == 0 || args.length > 2)
			Debug.error("Invalid argument length specified for SweetAlert.");

		//expects (String, String) or (JsonObject, Function)
		if (typeof args[0] == "object") {
			angular.merge(options, options, args[0]);
			if (args.length == 2 && typeof args[1] == "function") {
				options.callback = args[1];
			}
		} else if (typeof args[0] == "string") {
			options.title = args[0];
			if (args.length == 2 && typeof args[1] == "string") {
				options.text = args[1];
			}
		}

		return options; //var [options, callback] = this.defaults.apply(this, arguments);
	};
	this.Input = function () {
		var options = this.defaults.apply(this, arguments);
		if (options.input == undefined)
			options.input = "text";
		options.showCancelButton = true;
		return swal(options);
	};
	this.Confirm = function () { //(JsonObject, Callback)-> returns boolean
		var options = {
			title: "Are you sure?",
			showCancelButton: true,
		};
		angular.merge(options, options, this.defaults.apply(this, arguments));
		if (!options.callback)
			return swal(options);
		return swal(options, options.callback);
	};
	this.Info = function () {
		var options = {};
		angular.merge(options, { type: "info" }, this.defaults.apply(this, arguments));
		if (!options.callback)
			return swal(options);
		return swal(options, options.callback);
	};
	this.Warning = function () {
		var options = {};
		angular.merge(options, { type: "warning" }, this.defaults.apply(this, arguments));
		if (!options.callback)
			return swal(options);
		return swal(options, options.callback);
	};
	this.Error = function () {
		var options = {};
		angular.merge(options, { type: "error" }, this.defaults.apply(this, arguments));
		if (!options.callback)
			return swal(options);
		return swal(options, options.callback);
	};
	this.Success = function () {
		var options = {};
		angular.merge(options, { type: "success" }, this.defaults.apply(this, arguments));
		if (!options.callback)
			return swal(options);
		return swal(options, options.callback);
	};
}]);
