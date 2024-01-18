import { Component, OnInit } from '@angular/core';
import { ViewportScroller } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, Configuration } from 'src/app/app.service';
import { ConfigurationsService } from '../configurations.service';

type TransitionObject = {
  name?: string
  loaded?: boolean
  adapter?: string
}

@Component({
  selector: 'app-configurations-show',
  templateUrl: './configurations-show.component.html',
  styleUrls: ['./configurations-show.component.scss']
})
export class ConfigurationsShowComponent implements OnInit {
  configurations: Configuration[] = [];
  configuration: string = "";
  selectedConfiguration: string = "All"
  loadedConfiguration: boolean = false;
  anchor = "";

  private selectedAdapter?: string;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private viewportScroller: ViewportScroller,
    private configurationsService: ConfigurationsService,
    private appService: AppService,
  ) { };

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });

    this.route.fragment.subscribe(hash => {
      this.anchor = hash ?? "";
    })

    this.route.queryParamMap.subscribe(params => {
      this.selectedConfiguration = params.has('name') && params.get('name') != '' ? params.get('name')! : "All";
      this.loadedConfiguration = !(params.has('loaded') && params.get('loaded') == "false");
      if(params.has('adapter'))
        this.selectedAdapter = params.get('adapter')!;

      this.getConfiguration();
    });
  }

  update(loaded: boolean) {
    this.loadedConfiguration = loaded;
    this.anchor = "";
    this.getConfiguration();
  };

  changeConfiguration(name: string) {
    this.selectedConfiguration = name;
    this.selectedAdapter = undefined;
    this.router.navigate([], { relativeTo: this.route, fragment: "" });
    this.anchor = ""; //unset hash anchor
    this.getConfiguration();
  };

  updateQueryParams() {
    const transitionObj: TransitionObject = {};
    if (this.selectedConfiguration != "All")
      transitionObj.name = this.selectedConfiguration;
    if (!this.loadedConfiguration)
      transitionObj.loaded = this.loadedConfiguration;
    if(this.selectedAdapter)
      transitionObj.adapter = this.selectedAdapter;

    const fragment = this.anchor != "" ? this.anchor : undefined;

    this.router.navigate([], { relativeTo: this.route, queryParams: transitionObj, fragment });
  };

  clipboard() {
    if (this.configuration) {
      this.appService.copyToClipboard(this.configuration);
    }
  }

  getConfiguration() {
    this.updateQueryParams();


    this.configurationsService.getConfiguration(this.selectedConfiguration, this.loadedConfiguration).subscribe((data) => {
      this.configuration = data;

      if (this.anchor) {
        this.viewportScroller.scrollToAnchor(this.anchor);
      }
    });
  };
}
