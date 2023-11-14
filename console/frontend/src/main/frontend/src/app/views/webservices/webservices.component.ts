import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';
import { ApiListener, Service, WebservicesService, Wsdl } from './webservices.service';

@Component({
  selector: 'app-webservices',
  templateUrl: './webservices.component.html',
  styleUrls: ['./webservices.component.scss']
})
export class WebservicesComponent implements OnInit {
  rootURL: string = this.appService.getServerPath();
  services: Service[] = [];
  apiListeners: ApiListener[] = [];
  wsdls: Wsdl[] = [];

  constructor(
    private appService: AppService,
    private wsService: WebservicesService
  ) { };

  ngOnInit() {
    this.wsService.getWebservices().subscribe((data) => {
      this.apiListeners = data.apiListeners;
      this.services = data.services;
      this.wsdls = data.wsdls;

      console.log(this.rootURL)
    });
  };

  compileURL(apiListener: ApiListener) {
    return this.rootURL + "iaf/api/webservices/openapi.json?uri=" + encodeURI(apiListener.uriPattern);
  };
};
