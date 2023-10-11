import { appModule } from "../../../app.module";

const ConfigurationsUploadController = function ($scope, Api, appConstants, $rootScope) {
	const ctrl = this;

	ctrl.datasources = {};
	ctrl.form = {};
	ctrl.form = {
		datasource: "",
		encoding: "",
		multiple_configs: false,
		activate_config: true,
		automatic_reload: false,
	};
	ctrl.file = null;

	ctrl.$onInit = function () {
		ctrl.unregister$on = $rootScope.$on('appConstants', function () {
			ctrl.form.datasource = appConstants['jdbc.datasource.default'];
		});

		Api.Get("jdbc", function (data) {
			$.extend(ctrl, data);
			ctrl.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
		});
	}

	ctrl.$onDestroy = function () {
		ctrl.unregister$on();
	}

	ctrl.updateFile = function (file) {
		ctrl.file = file;
	}

	ctrl.submit = function () {
		if (ctrl.file == null) return;

		var fd = new FormData();
		if (ctrl.form.datasource && ctrl.form.datasource != "")
			fd.append("datasource", ctrl.form.datasource);
		else
			fd.append("datasource", ctrl.datasources[0]);

		fd.append("encoding", ctrl.form.encoding);
		fd.append("multiple_configs", ctrl.form.multiple_configs);
		fd.append("activate_config", ctrl.form.activate_config);
		fd.append("automatic_reload", ctrl.form.automatic_reload);
		fd.append("file", ctrl.file, ctrl.file.name);

		Api.Post("configurations", fd, function (data) {
			ctrl.error = "";
			ctrl.result = "";
			for (const pair in data) {
				if (data[pair] == "loaded") {
					ctrl.result += "Successfully uploaded configuration [" + pair + "]<br/>";
				} else {
					ctrl.error += "Could not upload configuration from the file [" + pair + "]: " + data[pair] + "<br/>";
				}
			}

			ctrl.form = {
				datasource: ctrl.datasources[0],
				encoding: "",
				multiple_configs: false,
				activate_config: true,
				automatic_reload: false,
			};
			if (ctrl.file != null) {
				angular.element(".form-file")[0].value = null;
				ctrl.file = null;
			}
		}, function (errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			ctrl.error = error;
			ctrl.result = "";
		}, false);
	};

	ctrl.reset = function () {
		ctrl.result = "";
		ctrl.error = "";
		ctrl.form = {
			datasource: ctrl.datasources[0],
			name: "",
			version: "",
			encoding: "",
			multiple_configs: false,
			activate_config: true,
			automatic_reload: false,
		};
	};
};

appModule.component('configurationsUpload', {
	controller: ['$scope', 'Api', 'appConstants', '$rootScope', ConfigurationsUploadController],
	templateUrl: 'js/app/views/configurations/configurations-upload/configurations-upload.component.html',
});
