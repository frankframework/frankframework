import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';

interface stateItemItem {
  adapterName: string,
  receiverName: string,
  messageCount: number
}

@Component({
  selector: 'app-inlinestore',
  templateUrl: './inlinestore.component.html',
  styleUrls: ['./inlinestore.component.scss'],
})
export class InlinestoreComponent implements OnInit {
  result: Record<string, { items: stateItemItem[], totalMessageCount: number }> = {};
  getProcessStateIcon = (processState: string) => this.appService.getProcessStateIcon(processState);
  getProcessStateIconColor = (processState: string) => this.appService.getProcessStateIconColor(processState)

  constructor(
    private apiService: ApiService,
    private appService: AppService,
  ) { };

  ngOnInit(): void {
    this.apiService.Get("inlinestores/overview", (data) => {
      this.result = data;
    });
  };
}
