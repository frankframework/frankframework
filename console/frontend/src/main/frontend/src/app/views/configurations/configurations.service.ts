import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AppService } from 'src/app/app.service';

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

}
