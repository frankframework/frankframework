import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { MiscService } from 'src/app/services/misc.service';

@Injectable({
  providedIn: 'root'
})
export class StatusService {

  constructor(
    private http: HttpClient,
    private Misc: MiscService
  ) { }

  getConfigurationFlowDiagram(flowUrl: string): Observable<{ data: HttpResponse<string>; url: string; }> {
    const baseUrl = this.Misc.getServerPath() + 'iaf/api/configurations/';
    const url = baseUrl + flowUrl;
    return this.http.get(url, { observe: "response", responseType: "text" })
      .pipe(map(data => ({ data, url })));
  }

  getAdapterFlowDiagram(flowUrl: string): Observable<HttpResponse<string>> {
    return this.http.get(flowUrl, { observe: "response", responseType: "text" });
  }

  updateAdapters(action: string, adapters: string[]){
    return this.http.put(this.Misc.absoluteApiPath + "adapters", { action, adapters });
  }
}
