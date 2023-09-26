import { Component, Inject, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MiscService } from 'src/angularjs/app/services/misc.service';

@Component({
  selector: 'app-iframe-ladybug-beta',
  templateUrl: './iframe-ladybug-beta.component.html',
  styleUrls: ['./iframe-ladybug-beta.component.scss']
})
export class IframeLadybugBetaComponent implements OnInit {
  url = "";
  iframeSrc?: SafeResourceUrl;
  redirectURL?: string;

  constructor(
    private sanitizer: DomSanitizer,
    private miscService: MiscService
  ) { };

  ngOnInit(): void {
    this.url = this.miscService.getServerPath() + "iaf/ladybug";
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(this.url);
  };
}
