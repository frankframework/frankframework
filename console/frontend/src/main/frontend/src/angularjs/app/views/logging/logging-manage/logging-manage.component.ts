import { ApiService } from "src/angularjs/app/services/api.service";
import { ToastrService } from "src/angularjs/app/services/toastr.service";
import { appModule } from "../../../app.module";

class LoggingManageController {
  logURL = "server/logging";
  updateDynamicParams = false;
  loggers = {};
  errorLevels = ["DEBUG", "INFO", "WARN", "ERROR"];
  form = {
    loglevel: "DEBUG",
    logIntermediaryResults: true,
    maxMessageLength: -1,
    errorLevels: this.errorLevels,
    enableDebugger: true,
  };
  loggersLength: number = 0;
  definitions: any;

  constructor(
    private Api: ApiService,
    private Toastr: ToastrService,
  ) { };

  $onInit() {
    this.updateLogInformation();
    this.setForm();
  };

  setForm() {
    this.Api.Get(this.logURL, (data) => {
      this.form = data;
      this.errorLevels = data.errorLevels;
    });
  };

  //Root logger level
  changeRootLoglevel(level: "DEBUG" | "INFO" | "WARN" | "ERROR") {
    this.form.loglevel = level;
  };

  //Individual level
  changeLoglevel(logger: any, level: "DEBUG" | "INFO" | "WARN" | "ERROR") {
    this.Api.Put(this.logURL + "/settings", { logger: logger, level: level }, () => {
      this.Toastr.success("Updated logger [" + logger + "] to [" + level + "]");
      this.updateLogInformation();
    });
  };

  //Reconfigure Log4j2
  reconfigure() {
    this.Api.Put(this.logURL + "/settings", { reconfigure: true }, () => {
      this.Toastr.success("Reconfigured log definitions!");
      this.updateLogInformation();
    });
  };

  submit(formData: any) {
    this.updateDynamicParams = true;
    this.Api.Put(this.logURL, formData, () => {
      this.Api.Get(this.logURL, (data) => {
        this.form = data;
        this.updateDynamicParams = false;
        this.Toastr.success("Successfully updated log configuration!");
        this.updateLogInformation();
      });
    }, () => {
      this.updateDynamicParams = false;
    });
  };

  updateLogInformation() {
    this.Api.Get(this.logURL + "/settings", (data) => {
      this.loggers = data.loggers;
      this.loggersLength = Object.keys(data.loggers).length;
      this.definitions = data.definitions;
    }, function (data) {
      console.error(data);
    });
  };

  reset() {
    this.setForm();
  };
};

appModule.component('loggingManage', {
  controller: ['Api', 'Toastr', LoggingManageController],
  templateUrl: 'js/app/views/logging/logging-manage/logging-manage.component.html'
});
