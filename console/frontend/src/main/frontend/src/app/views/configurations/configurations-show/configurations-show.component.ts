import { Component, OnInit } from '@angular/core';
import { ViewportScroller } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, Configuration } from 'src/app/app.service';
import { ConfigurationsService } from '../configurations.service';

type TransitionObject = {
  name?: string;
  loaded?: boolean;
  adapter?: string;
};

@Component({
  selector: 'app-configurations-show',
  templateUrl: './configurations-show.component.html',
  styleUrls: ['./configurations-show.component.scss'],
})
export class ConfigurationsShowComponent implements OnInit {
  configurations: Configuration[] = [];
  configuration: string = '';
  selectedConfiguration: string = 'All';
  loadedConfiguration: boolean = false;
  anchor = '';

  private selectedAdapter?: string;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private viewportScroller: ViewportScroller,
    private configurationsService: ConfigurationsService,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => {
      this.configurations = this.appService.configurations;
    });

    this.route.fragment.subscribe((hash) => {
      this.anchor = hash ?? '';
    });

    this.route.queryParamMap.subscribe((parameters) => {
      this.selectedConfiguration =
        parameters.has('name') && parameters.get('name') != ''
          ? parameters.get('name')!
          : 'All';
      this.loadedConfiguration = !(
        parameters.has('loaded') && parameters.get('loaded') == 'false'
      );
      if (parameters.has('adapter'))
        this.selectedAdapter = parameters.get('adapter')!;

      this.getConfiguration();
    });
  }

  update(loaded: boolean): void {
    this.loadedConfiguration = loaded;
    this.anchor = '';
    this.getConfiguration();
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    this.selectedAdapter = undefined;
    this.router.navigate([], { relativeTo: this.route, fragment: '' });
    this.anchor = ''; //unset hash anchor
    this.getConfiguration();
  }

  updateQueryParams(): void {
    const transitionObject: TransitionObject = {};
    if (this.selectedConfiguration != 'All')
      transitionObject.name = this.selectedConfiguration;
    if (!this.loadedConfiguration)
      transitionObject.loaded = this.loadedConfiguration;
    if (this.selectedAdapter) transitionObject.adapter = this.selectedAdapter;

    const fragment = this.anchor == '' ? undefined : this.anchor;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: transitionObject,
      fragment,
    });
  }

  clipboard(): void {
    if (this.configuration) {
      this.appService.copyToClipboard(this.configuration);
    }
  }

  getConfiguration(): void {
    this.updateQueryParams();

    this.configurationsService
      .getConfiguration(this.selectedConfiguration, this.loadedConfiguration)
      .subscribe((data) => {
        this.configuration = data;

        if (this.anchor) {
          this.viewportScroller.scrollToAnchor(this.anchor);
        }
      });
  }
}
