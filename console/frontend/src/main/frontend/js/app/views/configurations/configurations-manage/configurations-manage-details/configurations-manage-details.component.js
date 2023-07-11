import { appModule } from "../../../../app.module";

const ConfigurationsManageDetailsController = function ($scope, $state, Api, Debug, Misc, $interval, SweetAlert, Toastr) {
	const ctrl = this;

	ctrl.loading = false;

	ctrl.$onInit = function () {
		if ($state.params && $state.params.name && $state.params.name != "")
			$state.$current.data.breadcrumbs = "Configurations > Manage > " + $state.params.name;
		else
			$state.go("pages.manage_configurations");

		ctrl.configuration = $state.params.name;

		ctrl.promise = $interval(function () {
			ctrl.update();
		}, 30000);
		ctrl.update();
	};

	ctrl.$onDestroy = function () {
		$interval.cancel(ctrl.promise);
	};

	ctrl.update = function() {
		ctrl.loading = true;
		Api.Get("configurations/" + $state.params.name + "/versions", function (data) {
			for (const x in data) {
				var configs = data[x];
				if (configs.active) {
					configs.actived = true;
				}
			}

			ctrl.versions = data;
			ctrl.loading = false;
		});
	};

	ctrl.download = function (config) {
		window.open(Misc.getServerPath() + "iaf/api/configurations/" + config.name + "/versions/" + encodeURIComponent(config.version) + "/download");
	};

	ctrl.deleteConfig = function (config) {
		var message = "";
		if (config.version) {
			message = "Are you sure you want to remove version '" + config.version + "'?";
		} else {
			message = "Are you sure?";
		}
		SweetAlert.Confirm({ title: message }, function (imSure) {
			if (imSure) {
				Api.Delete("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), function () {
					Toastr.success("Successfully removed version '" + config.version + "'");
					ctrl.update();
				});
			}
		});
	};

	ctrl.activate = function (config) {
		for (const x in ctrl.versions) {
			var configs = ctrl.versions[x];
			if (configs.version != config.version)
				configs.actived = false;
		}
		Api.Put("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), { activate: config.active }, function (data) {
			Toastr.success("Successfully changed startup config to version '" + config.version + "'");
		}, function () {
			ctrl.update();
		});
	};

	ctrl.scheduleReload = function (config) {
		Api.Put("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), { autoreload: config.autoreload }, function (data) {
			Toastr.success("Successfully " + (config.autoreload ? "enabled" : "disabled") + " Auto Reload for version '" + config.version + "'");
		}, function () {
			ctrl.update();
		});
	};
}

appModule.component('configurationsManageDetails', {
	controller: ['$scope', '$state', 'Api', 'Debug', 'Misc', '$interval', 'SweetAlert', 'Toastr', ConfigurationsManageDetailsController],
	templateUrl: "js/app/views/configurations/configurations-manage/configurations-manage-details/configurations-manage-details.component.html",
});
