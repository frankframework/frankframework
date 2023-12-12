import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, Configuration } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { ConfigurationsService } from '../../configurations.service';
import { ToastService } from 'src/app/services/toast.service';

@Component({
  selector: 'app-configurations-manage-details',
  templateUrl: './configurations-manage-details.component.html',
  styleUrls: ['./configurations-manage-details.component.scss']
})
export class ConfigurationsManageDetailsComponent implements OnInit, OnDestroy {
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
  promise: number = -1;
  versions: Configuration[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private appService: AppService,
    private configurationsService: ConfigurationsService,
    private sweetalertService: SweetalertService,
    private toastService: ToastService
  ) {
    const routeState = this.router.getCurrentNavigation()?.extras.state ?? {}
    if(!routeState['configuration']){
      this.router.navigate(['..'], { relativeTo: this.route });
    }
    this.configuration = routeState['configuration'];
  };

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const nameParam = params.get('name');
      if (nameParam && nameParam != "")
        this.appService.customBreadcrumbs("Configurations > Manage > " + nameParam);
      else
        this.router.navigate(['..'], { relativeTo: this.route });

      this.promise = window.setInterval(() => {
        this.update();
      }, 30000);

      this.update();
    });
  }

  ngOnDestroy() {
    clearInterval(this.promise);
  };

  update() {
    this.loading = true;
    this.configurationsService.getConfigurationVersions(this.configuration.name).subscribe((data) => {
      for (const configs of data) {
        if (configs.active) {
          configs.actived = true;
        }
      }

      this.versions = data;
      this.loading = false;
    });
  };

  download(config: Configuration) {
    window.open(this.appService.getServerPath() + "iaf/api/configurations/" + config.name + "/versions/" + encodeURIComponent(config.version!) + "/download");
  };

  deleteConfig(config: Configuration) {
    var message = "";

    if (config.version) {
      message = "Are you sure you want to remove version '" + config.version + "'?";
    } else {
      message = "Are you sure?";
    };

    this.sweetalertService.Confirm({ title: message }).then(result => {
      if (result.isConfirmed) {
        this.configurationsService.deleteConfigurationVersion(config.name, encodeURIComponent(config.version!)).subscribe(() => {
          this.toastService.success("Successfully removed version '" + config.version + "'");
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
    this.configurationsService.updateConfigurationVersion( config.name, encodeURIComponent(config.version!), { activate: config.active! }).subscribe({ next: () => {
      this.toastService.success("Successfully changed startup config to version '" + config.version + "'");
    }, error: () => {
      this.update();
    }});
  };

  scheduleReload(config: Configuration) {
    this.configurationsService.updateConfigurationVersion( config.name, encodeURIComponent(config.version!), { autoreload: config.autoreload }).subscribe({ next: () => {
      this.toastService.success("Successfully " + (config.autoreload ? "enabled" : "disabled") + " Auto Reload for version '" + config.version + "'");
    }, error: () => {
      this.update();
    }});
  };
}
