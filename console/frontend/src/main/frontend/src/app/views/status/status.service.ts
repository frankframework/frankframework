import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';

@Injectable({
  providedIn: 'root'
})
export class StatusService {

  constructor(
    private http: HttpClient,
    private appService: AppService,
    private Misc: MiscService
  ) { }

  getConfigurationFlowDiagram(flowUrl: string): Observable<{ data: HttpResponse<string>; url: string; }> {
    const baseUrl = this.appService.getServerPath() + 'iaf/api/configurations/';
    const url = baseUrl + flowUrl;
    return this.http.get(url, { observe: "response", responseType: "text" })
      .pipe(map(data => ({ data, url })));
  }

  getAdapterFlowDiagram(flowUrl: string): Observable<HttpResponse<string>> {
    return this.http.get(flowUrl, { observe: "response", responseType: "text" });
  }

  updateConfigurations(action: string) {
    return this.http.put(this.appService.absoluteApiPath + "configurations", { action });
  }

  updateSelectedConfiguration(selectedConfiguration: string, action: string) {
    return this.http.put(this.appService.absoluteApiPath + "configurations/" + selectedConfiguration, { action });
  }

  updateAdapters(action: string, adapters: string[]) {
    return this.http.put(this.appService.absoluteApiPath + "adapters", { action, adapters });
  }

  updateAdapter(configuration: string, adapter: string, action: string) {
    return this.http.put(this.appService.absoluteApiPath + "configurations/" + configuration + "/adapters/" + this.Misc.escapeURL(adapter), { action });
  }

  updateReceiver(configuration: string, adapter: string, receiver: string, action: string) {
    return this.http.put(this.appService.absoluteApiPath + "configurations/" + configuration + "/adapters/" + this.Misc.escapeURL(adapter) + "/receivers/" + this.Misc.escapeURL(receiver), { action });
  }
}
