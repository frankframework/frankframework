import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants } from 'src/angularjs/app/app.module';
import { AppService, Configuration } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { APPCONSTANTS } from 'src/app/app.module';

interface keyValProperty {
  key: string,
  val: string,
}

@Component({
  selector: 'app-environment-variables',
  templateUrl: './environment-variables.component.html',
  styleUrls: ['./environment-variables.component.scss']
})
export class EnvironmentVariablesComponent implements OnInit, OnDestroy {
  searchFilter: string = "";
  selectedConfiguration: string = "";
  configProperties: keyValProperty[] = [];
  environmentProperties: keyValProperty[] = [];
  systemProperties: keyValProperty[] = [];
  configurations: Configuration[] = [];

  private _subscriptions = new Subscription();

  constructor(
    private appService: AppService,
    @Inject(APPCONSTANTS) private appConstants: AppConstants,
    private apiService: ApiService,
  ) { };

  ngOnInit() {
    function convertPropertiesToArray(propertyList: keyValProperty[]) {
      var tmp = new Array();
      for (var variableName in propertyList) {
        tmp.push({
          key: variableName,
          val: propertyList[variableName]
        });
      }
      return tmp;
    }

    this.configurations = this.appService.configurations;
    const configurationsSubscription = this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });
    this._subscriptions.add(configurationsSubscription);

    this.apiService.Get("environmentvariables", (data) => {
      var instanceName = null;
      for (var configName in data["Application Constants"]) {
        this.appConstants[configName] = convertPropertiesToArray(data["Application Constants"][configName]);
        if (instanceName == null) {
          instanceName = data["Application Constants"][configName]["instance.name"];
        }
      }
      this.changeConfiguration("All");
      this.environmentProperties = convertPropertiesToArray(data["Environment Variables"]);
      this.systemProperties = convertPropertiesToArray(data["System Properties"]);
    });
  };

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }

  changeConfiguration(name: string) {
    this.selectedConfiguration = name;
    this.configProperties = this.appConstants[name];
  };
};
