import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';

type stateItemItem = {
  configurationName: string,
  adapterName: string,
  receiverName: string,
  messageCount: number
}

type InlineStore = Record<string, { items: stateItemItem[], totalMessageCount: number }>;

@Component({
  selector: 'app-inlinestore',
  templateUrl: './inlinestore.component.html',
  styleUrls: ['./inlinestore.component.scss'],
})
export class InlinestoreComponent implements OnInit {
  result: InlineStore = {};
  getProcessStateIcon = (processState: string) => this.appService.getProcessStateIcon(processState);
  getProcessStateIconColor = (processState: string) => this.appService.getProcessStateIconColor(processState)

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) { };

  ngOnInit(): void {
    this.http.get<InlineStore>(this.appService.absoluteApiPath + "inlinestores/overview").subscribe((data) => {
      this.result = data;
    });
  };
}
