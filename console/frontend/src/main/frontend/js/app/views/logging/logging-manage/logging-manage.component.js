import { appModule } from "../../../app.module";

const LoggingManageController = function ($scope, Api, Misc, $timeout, $state, Toastr) {
    const ctrl = this;

    var logURL = "server/logging";
    ctrl.updateDynamicParams = false;
    ctrl.loggers = {};
    ctrl.errorLevels = ["DEBUG", "INFO", "WARN", "ERROR"];
    ctrl.form = {
        loglevel: "DEBUG",
        logIntermediaryResults: true,
        maxMessageLength: -1,
        errorLevels: ctrl.errorLevels,
        enableDebugger: true,
    };

    ctrl.$onInit = function () {
        updateLogInformation();

        Api.Get(logURL, function (data) {
            ctrl.form = data;
            ctrl.errorLevels = data.errorLevels;
        });
    };

    //Root logger level
    ctrl.changeRootLoglevel = function (level) {
        ctrl.form.loglevel = level;
    };

    //Individual level
    ctrl.changeLoglevel = function (logger, level) {
        Api.Put(logURL + "/settings", { logger: logger, level: level }, function () {
            Toastr.success("Updated logger [" + logger + "] to [" + level + "]");
            updateLogInformation();
        });
    };

    //Reconfigure Log4j2
    ctrl.reconfigure = function () {
        Api.Put(logURL + "/settings", { reconfigure: true }, function () {
            Toastr.success("Reconfigured log definitions!");
            updateLogInformation();
        });
    }

    ctrl.submit = function (formData) {
        ctrl.updateDynamicParams = true;
        Api.Put(logURL, formData, function () {
            Api.Get(logURL, function (data) {
                ctrl.form = data;
                ctrl.updateDynamicParams = false;
                Toastr.success("Successfully updated log configuration!");
                updateLogInformation();
            });
        }, function () {
            ctrl.updateDynamicParams = false;
        });
    };

    function updateLogInformation() {
        Api.Get(logURL + "/settings", function (data) {
            ctrl.loggers = data.loggers;
            ctrl.loggersLength = Object.keys(data.loggers).length;
            ctrl.definitions = data.definitions;
        }, function (data) {
            console.error(data);
        });
    };
};

appModule.component('loggingManage', {
    controller: ['$scope', 'Api', 'Misc', '$timeout', '$state', 'Toastr', LoggingManageController],
    templateUrl: 'js/app/views/logging/logging-manage/logging-manage.component.html'
});
