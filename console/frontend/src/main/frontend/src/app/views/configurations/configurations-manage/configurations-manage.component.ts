import { Component, OnInit } from '@angular/core';
import { AppService, Configuration } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';

@Component({
  selector: 'app-configurations-manage',
  templateUrl: './configurations-manage.component.html',
  styleUrls: ['./configurations-manage.component.scss']
})
export class ConfigurationsManageComponent implements OnInit {
  configurations: Configuration[] = [];

  constructor(
    private Api: ApiService,
    private appService: AppService,
  ) { };

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });

    this.Api.Get("server/configurations", (data) => {
      this.appService.updateConfigurations(data);
    });
  };
}
