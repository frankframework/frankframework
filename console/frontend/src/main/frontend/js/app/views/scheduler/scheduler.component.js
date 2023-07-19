import { appModule } from "../../app.module";

const SchedulerController = function ($rootScope, $scope, Api, Poller, $state, SweetAlert) {
    const ctrl = this;

    ctrl.jobs = {};
    ctrl.scheduler = {};
    ctrl.searchFilter = "";

    ctrl.$onInit = function () {
        Poller.add("schedules", function (data) {
            $.extend(ctrl, data);
        }, true, 5000);
    };

    ctrl.$onDestroy = function () {
        Poller.remove("schedules");
    };

    ctrl.start = function () {
        Api.Put("schedules", { action: "start" });
    };

    ctrl.pauseScheduler = function () {
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
    controller: ['$rootScope', '$scope', 'Api', 'Poller', '$state', 'SweetAlert', SchedulerController],
    templateUrl: 'js/app/views/scheduler/scheduler.component.html'
});