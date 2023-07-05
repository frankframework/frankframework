import { appModule } from "../../../app.module";

appModule.controller('EditScheduleCtrl', ['$scope', 'Api', '$stateParams', function ($scope, Api, $stateParams) {
	$scope.state = [];
	$scope.addLocalAlert = function (type, message) {
		$scope.state.push({ type: type, message: message });
	};
	var url = "schedules/" + $stateParams.group + "/jobs/" + $stateParams.name;
	$scope.editMode = true;
	$scope.selectedConfiguration = "";

	$scope.form = {
		name: "",
		group: "",
		adapter: "",
		listener: "",
		cron: "",
		interval: "",
		message: "",
		description: "",
		locker: false,
		lockkey: "",
	};

	Api.Get(url, function (data) {
		$scope.selectedConfiguration = data.configuration;
		$scope.form = {
			name: data.name,
			group: data.group,
			adapter: data.adapter,
			listener: data.listener,
			cron: data.triggers[0].cronExpression || "",
			interval: data.triggers[0].repeatInterval || "",
			message: data.message,
			description: data.description,
			locker: data.locker,
			lockkey: data.lockkey,
		};
	});

	$scope.submit = function (form) {
		var fd = new FormData();
		$scope.state = [];

		fd.append("name", $scope.form.name);
		fd.append("group", $scope.form.group);
		fd.append("configuration", $scope.selectedConfiguration);
		fd.append("adapter", $scope.form.adapter);
		fd.append("listener", $scope.form.listener);
		if ($scope.form.cron)
			fd.append("cron", $scope.form.cron);
		if ($scope.form.interval)
			fd.append("interval", $scope.form.interval);
		fd.append("message", $scope.form.message);
		fd.append("description", $scope.form.description);
		fd.append("locker", $scope.form.locker);
		if ($scope.form.lockkey)
			fd.append("lockkey", $scope.form.lockkey);

		Api.Put(url, fd, function (data) {
			$scope.addLocalAlert("success", "Successfully edited schedule!");
		}, function (errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.addLocalAlert("warning", error);
		}, false);
	};

}]);
