import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { ApiListener, Service, WebservicesService, Wsdl } from './webservices.service';

@Component({
  selector: 'app-webservices',
  templateUrl: './webservices.component.html',
  styleUrls: ['./webservices.component.scss'],
})
export class WebservicesComponent implements OnInit {
  protected rootURL: string = this.appService.getServerPath();
  protected services: Service[] = [];
  protected apiListeners: ApiListener[] = [];
  protected wsdls: Wsdl[] = [];

  constructor(
    private appService: AppService,
    private wsService: WebservicesService,
  ) {}

  ngOnInit(): void {
    this.wsService.getWebservices().subscribe((data) => {
      this.apiListeners = data.apiListeners;
      this.services = data.services;
      this.wsdls = data.wsdls;
    });
  }

  compileURL(apiListener: ApiListener): string {
    return `${this.rootURL}iaf/api/webservices/openapi.json?uri=${encodeURI(apiListener.uriPattern)}`;
  }
}
