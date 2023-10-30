import { Component, Inject, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-iframe-ladybug',
  templateUrl: '../iframe.component.html',
  styleUrls: ['../iframe.component.scss']
})
export class IframeLadybugComponent implements OnInit {
  url: string = "";
  iframeSrc?: SafeResourceUrl;
  redirectURL?: string;

  constructor(
    private sanitizer: DomSanitizer,
    private appService: AppService,
  ) { };

  ngOnInit(): void {
    this.url = this.appService.getServerPath() + "iaf/testtool";
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(this.url);
  };
}
