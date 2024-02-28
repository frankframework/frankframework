import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService, Configuration } from 'src/app/app.service';
import { KeyValue } from '@angular/common';

type keyValueProperty = KeyValue<string, string>;

@Component({
  selector: 'app-environment-variables',
  templateUrl: './environment-variables.component.html',
  styleUrls: ['./environment-variables.component.scss'],
})
export class EnvironmentVariablesComponent implements OnInit, OnDestroy {
  searchFilter: string = '';
  selectedConfiguration: string = '';
  configProperties: keyValueProperty[] = [];
  environmentProperties: keyValueProperty[] = [];
  systemProperties: keyValueProperty[] = [];
  configurations: Configuration[] = [];

  private _subscriptions = new Subscription();
  private appConstants: AppConstants;

  constructor(private appService: AppService) {
    this.appConstants = this.appService.APP_CONSTANTS;
    const appConstantsSubscription = this.appService.appConstants$.subscribe(
      () => {
        this.appConstants = this.appService.APP_CONSTANTS;
      },
    );
    this._subscriptions.add(appConstantsSubscription);
  }

  ngOnInit() {
    function convertPropertiesToArray(propertyList: Record<string, string>) {
      var temporary: keyValueProperty[] = [];
      for (var variableName in propertyList) {
        temporary.push({
          key: variableName,
          value: propertyList[variableName],
        });
      }
      return temporary;
    }

    this.configurations = this.appService.configurations;
    const configurationsSubscription =
      this.appService.configurations$.subscribe(() => {
        this.configurations = this.appService.configurations;
      });
    this._subscriptions.add(configurationsSubscription);

    this.appService.getEnvironmentVariables().subscribe((data) => {
      var instanceName = null;
      for (var configName in data['Application Constants']) {
        this.appConstants[configName] = convertPropertiesToArray(
          data['Application Constants'][configName],
        );
        if (instanceName == null) {
          instanceName =
            data['Application Constants'][configName]['instance.name'];
        }
      }
      this.changeConfiguration('All');
      this.environmentProperties = convertPropertiesToArray(
        data['Environment Variables'],
      );
      this.systemProperties = convertPropertiesToArray(
        data['System Properties'],
      );
    });
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }

  changeConfiguration(name: string) {
    this.selectedConfiguration = name;
    this.configProperties = this.appConstants[name];
  }
}
