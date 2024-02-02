import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';

@Component({
  selector: 'app-file-viewer',
  templateUrl: './file-viewer.component.html',
  styleUrls: ['./file-viewer.component.scss']
})
export class FileViewerComponent implements OnInit {

  @Input()
  fileName: string = "";
  @Input()
  contentType: string = "text/plain";

  protected fileContents = "Loading...";

  constructor(
    private http: HttpClient,
    private appService: AppService,
    private miscService: MiscService,
  ) {}

  ngOnInit() {
    const headers = { Accept: this.contentType };
    // TODO get chucks by using HttpClient.request?
    const fileContentsRequest = this.http.get(`${this.appService.absoluteApiPath}file-viewer?file=${this.miscService.escapeURL(this.fileName)}`, { headers, responseType: 'text' });
    fileContentsRequest.subscribe({ next: data => {
      this.fileContents = data;
    }, error: (errorData: HttpErrorResponse) => {
      this.fileContents = `Error requesting file data: ${errorData.message}`;
      console.error(errorData)
    } });
  }

}
