import { Component, inject, OnInit, Signal } from '@angular/core';
import { AppService, Configuration } from 'src/app/app.service';
import { ConfigurationsService } from '../configurations.service';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HasAccessToLinkDirective } from '../../../components/has-access-to-link.directive';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faDownload, faUpload } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-configurations-manage',
  imports: [NgClass, RouterLink, HasAccessToLinkDirective, FaIconComponent],
  templateUrl: './configurations-manage.component.html',
  styleUrls: ['./configurations-manage.component.scss'],
})
export class ConfigurationsManageComponent implements OnInit {
  protected configurations: Signal<Configuration[]>;
  protected readonly faDownload = faDownload;
  protected readonly faUpload = faUpload;

  private readonly configurationsService: ConfigurationsService = inject(ConfigurationsService);
  private readonly appService: AppService = inject(AppService);

  constructor() {
    this.configurations = this.appService.configurations;
  }

  ngOnInit(): void {
    this.configurationsService.getConfigurations().subscribe((data) => this.appService.updateConfigurations(data));
  }

  downloadAll(): void {
    window.open(`${this.appService.absoluteApiPath}server/configurations/download`, '_blank');
  }
}
