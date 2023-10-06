import { Component, Inject, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MiscService } from 'src/angularjs/app/services/misc.service';

@Component({
  selector: 'app-iframe-larva',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss']
})
export class IframeLarvaComponent implements OnInit {
  url: string = "";
  iframeSrc?: SafeResourceUrl;
  redirectURL?: string;

  constructor(
    private sanitizer: DomSanitizer,
    private miscService: MiscService,
  ) { };

  ngOnInit(): void {
    this.url = this.miscService.getServerPath() + "iaf/larva";
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(this.url);
  };
}
