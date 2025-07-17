import { Component, computed, inject, OnInit, Signal } from '@angular/core';
import { AppService } from 'src/app/app.service';
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
export class EnvironmentVariablesComponent implements OnInit {
  protected readonly GLOBAL_TAB_NAME = 'Global';
  protected searchFilter: string = '';
  protected selectedConfiguration: string = '';
  protected configProperties: keyValueProperty[] = [];
  protected environmentProperties: keyValueProperty[] = [];
  protected systemProperties: keyValueProperty[] = [];

  private readonly appService: AppService = inject(AppService);
  // eslint-disable-next-line unicorn/consistent-function-scoping
  protected configurationNames: Signal<string[]> = computed(() =>
    this.appService.configurations().map((configuration) => configuration.name),
  );

  ngOnInit(): void {
    this.appService.getEnvironmentVariables().subscribe((data) => {
      let instanceName = null;
      for (const configName in data['Application Constants']) {
        this.appService.appConstants()[configName] = this.convertPropertiesToArray(
          data['Application Constants'][configName],
        );
        if (instanceName == null) {
          instanceName = data['Application Constants'][configName]['instance.name'];
        }
      }
      this.changeConfiguration(this.GLOBAL_TAB_NAME);
      this.environmentProperties = this.convertPropertiesToArray(data['Environment Variables']);
      this.systemProperties = this.convertPropertiesToArray(data['System Properties']);
    });
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    this.configProperties = this.appService.appConstants()[name] as keyValueProperty[];
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
