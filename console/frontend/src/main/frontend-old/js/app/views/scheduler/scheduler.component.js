import { appModule } from "../../app.module";

const SchedulerController = function ($rootScope, $timeout, Api, Poller, $state, SweetAlert, appService) {
    const ctrl = this;

    ctrl.jobs = {};
    ctrl.scheduler = {};
    ctrl.searchFilter = "";
	ctrl.refreshing = false;

    ctrl.$onInit = function () {
        Poller.add("schedules", function (data) {
            $.extend(ctrl, data);
			ctrl.refreshing = false;
        }, true, 5000);

		ctrl.databaseSchedulesEnabled = appService.databaseSchedulesEnabled;
		$rootScope.$on('databaseSchedulesEnabled', function (){
			ctrl.databaseSchedulesEnabled = appService.databaseSchedulesEnabled;
		});
    };

    ctrl.$onDestroy = function () {
        Poller.remove("schedules");
    };

    ctrl.start = function () {
		ctrl.refreshing = true;
        Api.Put("schedules", { action: "start" });
    };

	ctrl.pauseScheduler = function () {
		ctrl.refreshing = true;
		Api.Put("schedules", { action: "pause" });
    };

    ctrl.pause = function (jobGroup, jobName) {
        Api.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "pause" });
    };

    ctrl.resume = function (jobGroup, jobName) {
        Api.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "resume" });
    };

    ctrl.remove = function (jobGroup, jobName) {
        SweetAlert.Confirm({ title: "Please confirm the deletion of '" + jobName + "'" }, function (imSure) {
            if (imSure) {
                Api.Delete("schedules/" + jobGroup + "/jobs/" + jobName);
            }
        });
    };

    ctrl.trigger = function (jobGroup, jobName) {
        Api.Put("schedules/" + jobGroup + "/jobs/" + jobName, { action: "trigger" });
    };

    ctrl.edit = function (jobGroup, jobName) {
        $state.go('pages.edit_schedule', { name: jobName, group: jobGroup });
    };
};

appModule.component('scheduler', {
	controller: ['$rootScope', '$timeout', 'Api', 'Poller', '$state', 'SweetAlert', 'appService', SchedulerController],
    templateUrl: 'js/app/views/scheduler/scheduler.component.html'
});
