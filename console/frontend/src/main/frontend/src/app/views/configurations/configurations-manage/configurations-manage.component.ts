import { Component, OnDestroy, OnInit } from '@angular/core';
import { AppService, Configuration } from 'src/app/app.service';
import { ConfigurationsService } from '../configurations.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-configurations-manage',
  templateUrl: './configurations-manage.component.html',
  styleUrls: ['./configurations-manage.component.scss'],
})
export class ConfigurationsManageComponent implements OnInit, OnDestroy {
  protected configurations: Configuration[] = [];

  private subscriptions = new Subscription();

  constructor(
    private configurationsService: ConfigurationsService,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    const appConfigurationsSubscription = this.appService.configurations$.subscribe(() => {
      this.configurations = this.appService.configurations;
    });
    this.subscriptions.add(appConfigurationsSubscription);

    this.configurationsService.getConfigurations().subscribe((data) => {
      this.appService.updateConfigurations(data);
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  downloadAll(): void {
    window.open(`${this.appService.absoluteApiPath}server/configurations/download`, '_blank');
  }
}
