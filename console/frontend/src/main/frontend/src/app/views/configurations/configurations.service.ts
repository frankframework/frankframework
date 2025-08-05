import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService, Configuration } from 'src/app/app.service';

@Injectable({
  providedIn: 'root',
})
export class ConfigurationsService {
  private http = inject(HttpClient);
  private appService = inject(AppService);

  getConfiguration(selectedConfiguration: string, loadedConfiguration: boolean): Observable<string> {
    let uri = 'configurations';

    if (selectedConfiguration != 'All') uri += `/${selectedConfiguration}`;
    if (loadedConfiguration) uri += '?loadedConfiguration=true';

    return this.http.get(this.appService.absoluteApiPath + uri, {
      responseType: 'text',
    });
  }

  getConfigurations(): Observable<Configuration[]> {
    return this.http.get<Configuration[]>(`${this.appService.absoluteApiPath}server/configurations`);
  }

  getConfigurationVersions(configurationName: string): Observable<Configuration[]> {
    return this.http.get<Configuration[]>(
      `${this.appService.absoluteApiPath}configurations/${configurationName}/versions`,
    );
  }

  postConfiguration(data: FormData): Observable<Record<string, string>> {
    return this.http.post<Record<string, string>>(`${this.appService.absoluteApiPath}configurations`, data);
  }

  updateConfigurationVersion(
    configurationName: string,
    configurationVersion: string,
    configuration: Record<string, NonNullable<unknown>>,
  ): Observable<object> {
    return this.http.put(
      `${this.appService.absoluteApiPath}configurations/${configurationName}/versions/${configurationVersion}`,
      configuration,
    );
  }

  deleteConfigurationVersion(configurationName: string, configurationVersion: string): Observable<object> {
    return this.http.delete(
      `${this.appService.absoluteApiPath}configurations/${configurationName}/versions/${configurationVersion}`,
    );
  }
}
