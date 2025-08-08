import { Component, inject, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { ApiListener, Service, WebservicesService, Wsdl } from './webservices.service';

import { HasAccessToLinkDirective } from '../../components/has-access-to-link.directive';

@Component({
  selector: 'app-webservices',
  templateUrl: './webservices.component.html',
  styleUrls: ['./webservices.component.scss'],
  imports: [HasAccessToLinkDirective],
})
export class WebservicesComponent implements OnInit {
  protected services: Service[] = [];
  protected apiListeners: ApiListener[] = [];
  protected wsdls: Wsdl[] = [];
  protected rootURL: string;

  private readonly wsService: WebservicesService = inject(WebservicesService);
  private readonly appService: AppService = inject(AppService);

  constructor() {
    this.rootURL = this.appService.getServerPath();
  }

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
