import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AppService, Configuration } from 'src/app/app.service';

@Injectable({
  providedIn: 'root'
})
export class ConfigurationsService {

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) { }

  getConfiguration(selectedConfiguration: string, loadedConfiguration: boolean){
    let uri = "configurations";

    if (selectedConfiguration != "All") uri += "/" + selectedConfiguration;
    if (loadedConfiguration) uri += "?loadedConfiguration=true";

    return this.http.get(this.appService.absoluteApiPath + uri, { responseType: 'text' });
  }

  getConfigurations(){
    return this.http.get<Configuration[]>(this.appService.absoluteApiPath + "server/configurations");
  }

  getConfigurationVersions(configurationName: string){
    return this.http.get<Configuration[]>("configurations/" + configurationName + "/versions");
  }

  updateConfigurationVersion(configurationName: string, configurationVersion: string, configuration: Record<string, any>){
    return this.http.put("configurations/" + configurationName + "/versions/" + configurationVersion, configuration);
  }

  deleteConfigurationVersion(configurationName: string, configurationVersion: string){
    return this.http.delete("configurations/" + configurationName + "/versions/" + configurationVersion);
  }

}
