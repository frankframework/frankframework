import { IIntervalService, IPromise } from "angular";
import { appModule } from "src/angularjs/app/app.module";
import { ApiService } from "src/angularjs/app/services/api.service";
import { MiscService } from "src/angularjs/app/services/misc.service";
import { SweetAlertService } from "src/angularjs/app/services/sweetalert.service";
import { ToastrService } from "src/angularjs/app/services/toastr.service";
import { StateParams, StateService } from '@uirouter/angularjs';

class ConfigurationsManageDetailsController {
  configuration = {};
  configurations = {};
  loading = false;
  promise: any;
  versions: any;

  constructor(
    private $stateParams: StateParams,
    private $stateService: StateService,
    private Api: ApiService,
    private Misc: MiscService,
    private $interval: IIntervalService,
    private SweetAlert: SweetAlertService,
    private Toastr: ToastrService,
  ) { };

  $onInit() {
    if (this.$stateParams && this.$stateParams['name'] && this.$stateParams['name'] != "")
      this.$stateService.$current.data.breadcrumbs = "Configurations > Manage > " + this.$stateParams['name'];
    else
      this.$stateService.go("pages.manage_configurations");

    this.configuration = this.$stateParams['name'];

    this.promise = this.$interval(() => {
      this.update();
    }, 30000);

    this.update();
  };

  $onDestroy() {
    this.$interval.cancel(this.promise);
  };

  update() {
    this.loading = true;
    this.Api.Get("configurations/" + this.$stateParams['name'] + "/versions", (data) => {
      for (const x in data) {
        var configs = data[x];
        if (configs.active) {
          configs.actived = true;
        }
      }

      this.versions = data;
      this.loading = false;
    });
  };

  download(config: any) {
    window.open(this.Misc.getServerPath() + "iaf/api/configurations/" + config.name + "/versions/" + encodeURIComponent(config.version) + "/download");
  };

  deleteConfig(config: any) {
    var message = "";

    if (config.version) {
      message = "Are you sure you want to remove version '" + config.version + "'?";
    } else {
      message = "Are you sure?";
    };

    this.SweetAlert.Confirm({ title: message }, (imSure) => {
      if (imSure) {
        this.Api.Delete("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), () => {
          this.Toastr.success("Successfully removed version '" + config.version + "'");
          this.update();
        });
      }
    });
  };

  activate(config: any) {
    for (const x in this.versions) {
      var configs = this.versions[x];
      if (configs.version != config.version)
        configs.actived = false;
    }
    this.Api.Put("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), { activate: config.active }, (data) => {
      this.Toastr.success("Successfully changed startup config to version '" + config.version + "'");
    }, () => {
      this.update();
    });
  };

  scheduleReload(config: any) {
    this.Api.Put("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), { autoreload: config.autoreload }, (data) => {
      this.Toastr.success("Successfully " + (config.autoreload ? "enabled" : "disabled") + " Auto Reload for version '" + config.version + "'");
    }, () => {
      this.update();
    });
  };
};

appModule.component('configurationsManageDetails', {
  controller: ['$state', 'Api', 'Misc', '$interval', 'SweetAlert', 'Toastr', ConfigurationsManageDetailsController],
  templateUrl: "js/app/views/configurations/configurations-manage/configurations-manage-details/configurations-manage-details.component.html",
});
