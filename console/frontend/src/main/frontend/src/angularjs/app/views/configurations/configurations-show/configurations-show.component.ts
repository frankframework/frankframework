import { ApiService } from "src/angularjs/app/services/api.service";
import { AppService } from "src/app/app.service";
import { appModule } from "../../../app.module";
import { StateParams, StateService } from '@uirouter/angularjs';

interface TransitionObject {
  name?: string
  loaded?: boolean
}

class ConfigurationsShowController {
  configurations = {};
  configuration = "";
  selectedConfiguration = (this.$stateParams['name'] != '') ? this.$stateParams['name'] : "All";
  loadedConfiguration = (this.$stateParams['loaded'] != undefined && this.$stateParams['loaded'] == false);
  anchor = this.$location.hash();

  constructor(
    private Api: ApiService,
    private $stateParams: StateParams,
    private $stateService: StateService,
    private $location: angular.ILocationService,
    private appService: AppService,
  ) { };

  $onInit() {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });
    this.getConfiguration();
  };

  update() {
    this.getConfiguration();
  };

  changeConfiguration(name: string) {
    this.selectedConfiguration = name;
    this.$location.hash(''); //clear the hash from the url
    this.anchor = ""; //unset hash anchor
    this.getConfiguration();
  };

  updateQueryParams() {
    var transitionObj: TransitionObject = {};
    if (this.selectedConfiguration != "All")
      transitionObj.name = this.selectedConfiguration;
    if (!this.loadedConfiguration)
      transitionObj.loaded = this.loadedConfiguration;

    this.$stateService.transitionTo('pages.configuration', transitionObj, { notify: false, reload: false });
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

    this.Api.Get(uri, (data) => {
      this.configuration = data;

      if (this.anchor) {
        this.$location.hash(this.anchor);
      }
    });
  };
};

appModule.component('configurationsShow', {
  controller: ['Api', '$state', '$location', 'appService', ConfigurationsShowController],
  templateUrl: 'js/app/views/configurations/configurations-show/configurations-show.component.html',
});
