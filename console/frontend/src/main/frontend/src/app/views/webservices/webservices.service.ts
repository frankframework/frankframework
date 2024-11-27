import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from 'src/app/app.service';

export interface Service {
  name: string;
  methods: string[];
  view: string;
  uriPattern: string;
}

export interface ApiListener {
  methods: string[];
  uriPattern: string;
  error: string;
}

export interface Wsdl {
  configuration: string;
  adapter: string;
  error: string;
}

interface WebServices {
  services: Service[];
  wsdls: Wsdl[];
  apiListeners: ApiListener[];
}

@Injectable({
  providedIn: 'root',
})
export class WebservicesService {
  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  getWebservices(): Observable<WebServices> {
    return this.http.get<WebServices>(`${this.appService.absoluteApiPath}webservices`);
  }
}
