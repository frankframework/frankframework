import { appModule } from "../../../../app.module";

appModule.controller('ManageConfigurationDetailsCtrl', ['$scope', '$state', 'Api', 'Debug', 'Misc', '$interval', 'SweetAlert', 'Toastr', function ($scope, $state, Api, Debug, Misc, $interval, SweetAlert, Toastr) {
	$scope.loading = false;

	var promise = $interval(function () {
		update();
	}, 30000);
	$scope.$on('$destroy', function () {
		$interval.cancel(promise);
	});

	$scope.configuration = $state.params.name;
	function update() {
		$scope.loading = true;
		Api.Get("configurations/" + $state.params.name + "/versions", function (data) {
			for (const x in data) {
				var configs = data[x];
				if (configs.active) {
					configs.actived = true;
				}
			}

			$scope.versions = data;
			$scope.loading = false;
		});
	};
	update();
	$scope.download = function (config) {
		window.open(Misc.getServerPath() + "iaf/api/configurations/" + config.name + "/versions/" + encodeURIComponent(config.version) + "/download");
	};
	$scope.deleteConfig = function (config) {
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
					update();
				});
			}
		});
	};

	$scope.activate = function (config) {
		for (const x in $scope.versions) {
			var configs = $scope.versions[x];
			if (configs.version != config.version)
				configs.actived = false;
		}
		Api.Put("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), { activate: config.active }, function (data) {
			Toastr.success("Successfully changed startup config to version '" + config.version + "'");
		}, function () {
			update();
		});
	};

	$scope.scheduleReload = function (config) {
		Api.Put("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), { autoreload: config.autoreload }, function (data) {
			Toastr.success("Successfully " + (config.autoreload ? "enabled" : "disabled") + " Auto Reload for version '" + config.version + "'");
		}, function () {
			update();
		});
	};
}]);
