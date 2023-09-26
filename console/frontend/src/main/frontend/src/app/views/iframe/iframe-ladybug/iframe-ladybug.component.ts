import { Component, Inject, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MiscService } from 'src/angularjs/app/services/misc.service';

@Component({
  selector: 'app-iframe-ladybug',
  templateUrl: './iframe-ladybug.component.html',
  styleUrls: ['./iframe-ladybug.component.scss']
})
export class IframeLadybugComponent implements OnInit {
  url = "";
  iframeSrc?: SafeResourceUrl;
  redirectURL?: string;

  constructor(
    private sanitizer: DomSanitizer,
    private Misc: MiscService,
  ) { };

  ngOnInit(): void {
    this.url = this.Misc.getServerPath() + "iaf/testtool";
    this.iframeSrc = this.sanitizer.bypassSecurityTrustResourceUrl(this.url);
  };
}
