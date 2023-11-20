import { Component, OnInit } from '@angular/core';
import { AppService, Configuration } from 'src/app/app.service';
import { ConfigurationsService } from '../configurations.service';

@Component({
  selector: 'app-configurations-manage',
  templateUrl: './configurations-manage.component.html',
  styleUrls: ['./configurations-manage.component.scss']
})
export class ConfigurationsManageComponent implements OnInit {
  configurations: Configuration[] = [];

  constructor(
    private configurationsService: ConfigurationsService,
    private appService: AppService
  ) { };

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });

    this.configurationsService.getConfigurations().subscribe((data) => {
      this.appService.updateConfigurations(data);
    });
  };
}
