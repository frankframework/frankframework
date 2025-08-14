import { Component, computed, inject, OnInit, Signal } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { KeyValue } from '@angular/common';
import { TabListComponent } from '../../components/tab-list/tab-list.component';
import { FormsModule } from '@angular/forms';
import { VariablesFilterPipe } from '../../pipes/variables-filter.pipe';
import { EnvironmentVariablesTableComponent } from './environment-variables-table/environment-variables-table.component';

export type KeyValueProperty = KeyValue<string, string>;

@Component({
  selector: 'app-environment-variables',
  imports: [TabListComponent, FormsModule, VariablesFilterPipe, EnvironmentVariablesTableComponent],
  templateUrl: './environment-variables.component.html',
  styleUrls: ['./environment-variables.component.scss'],
})
export class EnvironmentVariablesComponent implements OnInit {
  protected readonly GLOBAL_TAB_NAME = 'Global';
  protected searchFilter = '';
  protected selectedConfiguration = '';
  protected configProperties: KeyValueProperty[] = [];
  protected environmentProperties: KeyValueProperty[] = [];
  protected systemProperties: KeyValueProperty[] = [];
  protected appConstants: Record<string, KeyValueProperty[]> = {};
  protected configurationNames: Signal<string[]> = computed(() =>
    this.appService.configurations().map((configuration) => configuration.name),
  );

  private readonly appService: AppService = inject(AppService);

  ngOnInit(): void {
    this.appService.getEnvironmentVariables().subscribe((data) => {
      const appConstants = data['Application Constants'];
      let instanceName = null;
      for (const configName in appConstants) {
        this.appConstants[configName] = this.convertPropertiesToArray(appConstants[configName]);
        if (instanceName == null) {
          instanceName = appConstants[configName]['instance.name'];
        }
      }
      this.changeConfiguration(this.GLOBAL_TAB_NAME);
      this.environmentProperties = this.convertPropertiesToArray(data['Environment Variables']);
      this.systemProperties = this.convertPropertiesToArray(data['System Properties']);
    });
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    this.configProperties = this.appConstants[name];
  }

  private convertPropertiesToArray(propertyList: Record<string, string>): KeyValueProperty[] {
    return Object.entries(propertyList).map(([key, value]): KeyValueProperty => ({ key, value }));
  }
}
