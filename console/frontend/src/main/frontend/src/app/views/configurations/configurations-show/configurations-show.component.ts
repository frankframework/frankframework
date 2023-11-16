import { Component, OnInit } from '@angular/core';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { StateParams, StateService } from '@uirouter/angularjs';
import { AppService, Configuration } from 'src/angularjs/app/app.service'
;
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
  selectedConfiguration: string = (this.stateParams['name'] != '') ? this.stateParams['name'] : "All";
  loadedConfiguration: boolean = (this.stateParams['loaded'] != undefined && this.stateParams['loaded'] == false);
  // TODO: anchor = this.$location.hash();

  constructor(
    private apiService: ApiService,
    private stateParams: StateParams,
    private stateService: StateService,
    // TODO: private $location: ,
    private appService: AppService,
  ) { };

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });
    this.getConfiguration();
  }

  update() {
    this.getConfiguration();
  };

  changeConfiguration(name: string) {
    this.selectedConfiguration = name;
    // TODO: this.$location.hash(''); //clear the hash from the url
    // TODO: this.anchor = ""; //unset hash anchor
    this.getConfiguration();
  };

  updateQueryParams() {
    var transitionObj: TransitionObject = {};
    if (this.selectedConfiguration != "All")
      transitionObj.name = this.selectedConfiguration;
    if (!this.loadedConfiguration)
      transitionObj.loaded = this.loadedConfiguration;

    this.stateService.transitionTo('pages.configuration', transitionObj, { notify: false, reload: false });
  };

  clipboard() {
    if (this.configuration) {
      var el = document.createElement('textarea');
      el.value = this.configuration;
      el.setAttribute('readonly', '');
      el.style.position = 'absolute';
      el.style.left = '-9999px';
      document.body.appendChild(el);
      el.select();
      document.execCommand('copy');
      document.body.removeChild(el);
    }
  }

  getConfiguration() {
    this.updateQueryParams();
    var uri = "configurations";

    if (this.selectedConfiguration != "All") uri += "/" + this.selectedConfiguration;
    if (this.loadedConfiguration) uri += "?loadedConfiguration=true";

    this.apiService.Get(uri, (data) => {
      this.configuration = data;

      // TODO: if (this.anchor) {
      //   this.$location.hash(this.anchor);
      // }
    });
  };
}
