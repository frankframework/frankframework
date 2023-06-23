import { appModule } from "../../app.module";

appModule.controller('SchedulerCtrl', ['$scope', 'Api', 'Poller', '$state', 'SweetAlert', function ($scope, Api, Poller, $state, SweetAlert) {
	$scope.jobs = {};
	$scope.scheduler = {};
	$scope.searchFilter = "";

	Poller.add("schedules", function (data) {
		$.extend($scope, data);
	}, true, 5000);
	$scope.$on('$destroy', function () {
		Poller.remove("schedules");
	});

	$scope.start = function () {
		Api.Put("schedules", { action: "start" });
	};

	$scope.pauseScheduler = function () {
		Api.Put("schedules", { action: "pause" });
	};

	$scope.pause = function (jobGroup, jobName) {
		Api.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "pause" });
	};

	$scope.resume = function (jobGroup, jobName) {
		Api.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "resume" });
	};

	$scope.remove = function (jobGroup, jobName) {
		SweetAlert.Confirm({ title: "Please confirm the deletion of '" + jobName + "'" }, function (imSure) {
			if (imSure) {
				Api.Delete("schedules/" + jobGroup + "/jobs/" + jobName);
			}
		});
	};

	$scope.trigger = function (jobGroup, jobName) {
		Api.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "trigger" });
	};

	$scope.edit = function (jobGroup, jobName) {
		$state.go('pages.edit_schedule', { name: jobName, group: jobGroup });
	};
}]);
