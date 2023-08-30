import { appModule } from "../../../app.module";

const LoggingManageController = function (Api, Toastr) {
    const ctrl = this;

    ctrl.logURL = "server/logging";
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
		ctrl.updateLogInformation();

		ctrl.setForm();
    };

	ctrl.setForm = function(){
		Api.Get(ctrl.logURL, function (data) {
			ctrl.form = data;
			ctrl.errorLevels = data.errorLevels;
		});
	}

    //Root logger level
    ctrl.changeRootLoglevel = function (level) {
        ctrl.form.loglevel = level;
    };

    //Individual level
    ctrl.changeLoglevel = function (logger, level) {
		Api.Put(ctrl.logURL + "/settings", { logger: logger, level: level }, function () {
            Toastr.success("Updated logger [" + logger + "] to [" + level + "]");
			ctrl.updateLogInformation();
        });
    };

    //Reconfigure Log4j2
    ctrl.reconfigure = function () {
		Api.Put(ctrl.logURL + "/settings", { reconfigure: true }, function () {
            Toastr.success("Reconfigured log definitions!");
			ctrl.updateLogInformation();
        });
    }

    ctrl.submit = function (formData) {
        ctrl.updateDynamicParams = true;
		Api.Put(ctrl.logURL, formData, function () {
			Api.Get(ctrl.logURL, function (data) {
                ctrl.form = data;
                ctrl.updateDynamicParams = false;
                Toastr.success("Successfully updated log configuration!");
				ctrl.updateLogInformation();
            });
        }, function () {
            ctrl.updateDynamicParams = false;
        });
    };

	ctrl.updateLogInformation = function() {
		Api.Get(ctrl.logURL + "/settings", function (data) {
            ctrl.loggers = data.loggers;
            ctrl.loggersLength = Object.keys(data.loggers).length;
            ctrl.definitions = data.definitions;
        }, function (data) {
            console.error(data);
        });
    };

	ctrl.reset = function(){
		ctrl.setForm();
	}
};

appModule.component('loggingManage', {
    controller: ['Api', 'Toastr', LoggingManageController],
    templateUrl: 'js/app/views/logging/logging-manage/logging-manage.component.html'
});
