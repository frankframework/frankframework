import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService, Configuration } from 'src/app/app.service';
import { KeyValue } from '@angular/common';
import { TabListComponent } from '../../components/tab-list/tab-list.component';
import { FormsModule } from '@angular/forms';
import { VariablesFilterPipe } from '../../pipes/variables-filter.pipe';
import { OrderByPipe } from '../../pipes/orderby.pipe';

type keyValueProperty = KeyValue<string, string>;

@Component({
  selector: 'app-environment-variables',
  imports: [TabListComponent, FormsModule, VariablesFilterPipe, OrderByPipe],
  templateUrl: './environment-variables.component.html',
  styleUrls: ['./environment-variables.component.scss'],
})
export class EnvironmentVariablesComponent implements OnInit, OnDestroy {
  protected readonly GLOBAL_TAB_NAME = 'Global';
  protected searchFilter: string = '';
  protected selectedConfiguration: string = '';
  protected configProperties: keyValueProperty[] = [];
  protected environmentProperties: keyValueProperty[] = [];
  protected systemProperties: keyValueProperty[] = [];
  protected configurations: Configuration[] = [];
  protected configurationNames: string[] = [];

  private _subscriptions = new Subscription();
  private appConstants: AppConstants = this.appService.APP_CONSTANTS;

  constructor(private appService: AppService) {}

  ngOnInit(): void {
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
    });
    this._subscriptions.add(appConstantsSubscription);

    this.configurations = this.appService.configurations;
    this.configurationNames = this.configurations.map((configuration) => configuration.name);
    const configurationsSubscription = this.appService.configurations$.subscribe(() => {
      this.configurations = this.appService.configurations;
      this.configurationNames = this.configurations.map((configuration) => configuration.name);
    });
    this._subscriptions.add(configurationsSubscription);

    const environmentVariablesSubscription = this.appService.getEnvironmentVariables().subscribe((data) => {
      let instanceName = null;
      for (const configName in data['Application Constants']) {
        this.appConstants[configName] = this.convertPropertiesToArray(data['Application Constants'][configName]);
        if (instanceName == null) {
          instanceName = data['Application Constants'][configName]['instance.name'];
        }
      }
      this.changeConfiguration(this.GLOBAL_TAB_NAME);
      this.environmentProperties = this.convertPropertiesToArray(data['Environment Variables']);
      this.systemProperties = this.convertPropertiesToArray(data['System Properties']);
    });
    this._subscriptions.add(environmentVariablesSubscription);
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    this.configProperties = this.appConstants[name] as keyValueProperty[];
  }

  private convertPropertiesToArray(propertyList: Record<string, string>): keyValueProperty[] {
    const propertiesArray: keyValueProperty[] = [];
    for (const variableName in propertyList) {
      propertiesArray.push({
        key: variableName,
        value: propertyList[variableName],
      });
    }
    return propertiesArray;
  }
}
