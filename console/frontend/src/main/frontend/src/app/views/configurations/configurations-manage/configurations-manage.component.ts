import { Component, OnInit } from '@angular/core';
import { StateParams, StateService } from '@uirouter/angularjs';
import { Configuration } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { MiscService } from 'src/angularjs/app/services/misc.service';
import { SweetAlertService } from 'src/angularjs/app/services/sweetalert.service';
import { ToastrService } from 'src/angularjs/app/services/toastr.service';

@Component({
  selector: 'app-configurations-manage',
  templateUrl: './configurations-manage.component.html',
  styleUrls: ['./configurations-manage.component.scss']
})
export class ConfigurationsManageComponent implements OnInit {
  configuration: Configuration = {
    name: "",
    stubbed: false,
    state: "STOPPED",
    type: "DatabaseClassLoader",
    jdbcMigrator: false,
    version: ""
  };
  configurations: Configuration[] = [];
  loading: boolean = false;
  promise: any;
  versions: Configuration[] = [];

  constructor(
    private stateParams: StateParams,
    private stateService: StateService,
    private apiService: ApiService,
    private miscService: MiscService,
    private sweetAlertService: SweetAlertService,
    private toastrService: ToastrService,
  ) { };

  ngOnInit(): void {
    if (this.stateParams && this.stateParams['name'] && this.stateParams['name'] != "")
      this.stateService.$current.data.breadcrumbs = "Configurations > Manage > " + this.stateParams['name'];
    else
      this.stateService.go("pages.manage_configurations");

    this.configuration = this.stateParams['name'];

    this.promise = setInterval(() => {
      this.update();
    }, 30000);

    this.update();
  };

  onDestroy() {
    clearInterval(this.promise);
  };

  update() {
    this.loading = true;
    this.apiService.Get("configurations/" + this.stateParams['name'] + "/versions", (data) => {
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

  download(config: Configuration) {
    window.open(this.miscService.getServerPath() + "iaf/api/configurations/" + config.name + "/versions/" + encodeURIComponent(config.version) + "/download");
  };

  deleteConfig(config: Configuration) {
    var message = "";

    if (config.version) {
      message = "Are you sure you want to remove version '" + config.version + "'?";
    } else {
      message = "Are you sure?";
    };

    this.sweetAlertService.Confirm({ title: message }, (imSure) => {
      if (imSure) {
        this.apiService.Delete("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), () => {
          this.toastrService.success("Successfully removed version '" + config.version + "'");
          this.update();
        });
      }
    });
  };

  activate(config: Configuration) {
    for (const x in this.versions) {
      var configs = this.versions[x];
      if (configs.version != config.version)
        configs.actived = false;
    }
    this.apiService.Put("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), { activate: config.active }, (data) => {
      this.toastrService.success("Successfully changed startup config to version '" + config.version + "'");
    }, () => {
      this.update();
    });
  };

  scheduleReload(config: Configuration) {
    this.apiService.Put("configurations/" + config.name + "/versions/" + encodeURIComponent(config.version), { autoreload: config.autoreload }, (data) => {
      this.toastrService.success("Successfully " + (config.autoreload ? "enabled" : "disabled") + " Auto Reload for version '" + config.version + "'");
    }, () => {
      this.update();
    });
  };
}
