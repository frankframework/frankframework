import { Component, OnInit } from '@angular/core';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { MiscService } from 'src/angularjs/app/services/misc.service';

interface Service {
  name: string
  method: string
  view: string
  uriPattern: string
}

interface ApiListener {
  method: string
  uriPattern: string
  error: string
}

interface Wsdl {
  configuration: string
  adapter: string
  error: string
}

@Component({
  selector: 'app-webservices',
  templateUrl: './webservices.component.html',
  styleUrls: ['./webservices.component.scss']
})
export class WebservicesComponent implements OnInit {
  rootURL: string = this.miscService.getServerPath();
  services: Service[] = [];
  apiListeners: ApiListener[] = [];
  wsdls: Wsdl[] = [];

  constructor(
    private apiService: ApiService,
    private miscService: MiscService
  ) { };

  ngOnInit() {
    this.apiService.Get("webservices", (data) => {
      Object.assign(this, data);
    });
  };

  compileURL(apiListener: any) {
    return this.rootURL + "iaf/api/webservices/openapi.json?uri=" + encodeURI(apiListener.uriPattern);
  };
};
