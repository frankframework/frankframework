import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';

@Injectable({
  providedIn: 'root',
})
export class StatusService {
  private http = inject(HttpClient);
  private appService = inject(AppService);
  private Misc = inject(MiscService);

  getConfigurationFlowDiagramUrl(flowUrl: string): string {
    return `${this.appService.getServerPath()}iaf/api/configurations${flowUrl}`;
  }

  getAdapterFlowDiagramContentLength(flowUrl: string): Observable<number> {
    return this.http
      .head(flowUrl, {
        observe: 'response',
        responseType: 'text',
      })
      .pipe(map((data) => +(data.headers.get('Content-Length') ?? 0)));
  }

  getAdapterFlowDiagram(flowUrl: string): Observable<HttpResponse<string>> {
    return this.http.get(flowUrl, {
      observe: 'response',
      responseType: 'text',
    });
  }

  updateConfigurations(action: string): Observable<object> {
    return this.http.put(`${this.appService.absoluteApiPath}configurations`, {
      action,
    });
  }

  updateSelectedConfiguration(selectedConfiguration: string, action: string): Observable<object> {
    return this.http.put(`${this.appService.absoluteApiPath}configurations/${selectedConfiguration}`, { action });
  }

  updateAdapters(action: string, adapters: string[]): Observable<object> {
    return this.http.put(`${this.appService.absoluteApiPath}adapters`, {
      action,
      adapters,
    });
  }

  updateAdapter(configuration: string, adapter: string, action: string): Observable<object> {
    return this.http.put(
      `${this.appService.absoluteApiPath}configurations/${configuration}/adapters/${this.Misc.escapeURL(adapter)}`,
      { action },
    );
  }

  updateReceiver(configuration: string, adapter: string, receiver: string, action: string): Observable<object> {
    return this.http.put(
      `${this.appService.absoluteApiPath}configurations/${configuration}/adapters/${this.Misc.escapeURL(
        adapter,
      )}/receivers/${this.Misc.escapeURL(receiver)}`,
      { action },
    );
  }
}
