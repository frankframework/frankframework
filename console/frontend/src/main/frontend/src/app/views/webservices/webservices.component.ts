import { Component, inject, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { ApiListener, Service, WebservicesService, Wsdl } from './webservices.service';

import { HasAccessToLinkDirective } from '../../components/has-access-to-link.directive';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faExternalLink, faArrowAltCircleDown, faFileArchive, faFileCode } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-webservices',
  templateUrl: './webservices.component.html',
  styleUrls: ['./webservices.component.scss'],
  imports: [HasAccessToLinkDirective, FaIconComponent],
})
export class WebservicesComponent implements OnInit {
  protected services: Service[] = [];
  protected apiListeners: ApiListener[] = [];
  protected wsdls: Wsdl[] = [];
  protected rootURL: string;
  protected readonly faExternalLink = faExternalLink;
  protected readonly faArrowAltCircleDown = faArrowAltCircleDown;
  protected readonly faFileArchive = faFileArchive;
  protected readonly faFileCode = faFileCode;

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
