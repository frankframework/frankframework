import { Component, OnInit } from '@angular/core';
import { ViewportScroller } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, Configuration } from 'src/app/app.service';
import { ConfigurationsService } from '../configurations.service';

interface TransitionObject {
  name?: string
  loaded?: boolean
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
      // this.loadedConfiguration = true; // used to be always "" but `"" == false` returns true so idk why this even exists
      this.loadedConfiguration = !(params.has('loaded') && params.get('loaded') == "false");

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

    const fragment = this.anchor != "" ? this.anchor : undefined;

    this.router.navigate([], { relativeTo: this.route, queryParams: transitionObj, fragment });
  };

  clipboard() {
    if (this.configuration) {
      let el = document.createElement('textarea');
      el.value = this.configuration;
      el.setAttribute('readonly', '');
      el.style.position = 'absolute';
      el.style.left = '-9999px';
      document.body.appendChild(el);
      el.select();
      document.execCommand('copy'); // TODO: soon deprecated
      document.body.removeChild(el);
    }
  }

  getConfiguration() {
    this.updateQueryParams();


    this.configurationsService.getConfiguration(this.selectedConfiguration, this.loadedConfiguration).subscribe((data) => {
      this.configuration = data;

      if (this.anchor) {
        // this.router.navigate([], { relativeTo: this.route, fragment: this.anchor });
        this.viewportScroller.scrollToAnchor(this.anchor);
      }
    });
  };
}
