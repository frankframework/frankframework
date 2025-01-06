import { Component, OnDestroy, OnInit } from '@angular/core';
import { AppService, Configuration } from 'src/app/app.service';
import { ConfigurationsService } from '../configurations.service';
import { Subscription } from 'rxjs';
import { NgClass, NgFor, NgIf } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HasAccessToLinkDirective } from '../../../components/has-access-to-link.directive';

@Component({
  selector: 'app-configurations-manage',
  imports: [NgClass, RouterLink, HasAccessToLinkDirective, NgFor, NgIf],
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
