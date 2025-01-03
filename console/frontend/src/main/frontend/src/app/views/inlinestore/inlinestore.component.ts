import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { getProcessStateIcon, getProcessStateIconColor } from 'src/app/utils';

type stateItemItem = {
  configurationName: string;
  adapterName: string;
  receiverName: string;
  messageCount: number;
};

type InlineStore = Record<string, { items: stateItemItem[]; totalMessageCount: number }>;

@Component({
  selector: 'app-inlinestore',
  templateUrl: './inlinestore.component.html',
  styleUrls: ['./inlinestore.component.scss'],
  standalone: false,
})
export class InlinestoreComponent implements OnInit {
  protected result: InlineStore = {};
  protected readonly getProcessStateIconColorFn = getProcessStateIconColor;
  protected readonly getProcessStateIconFn = getProcessStateIcon;

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.http.get<InlineStore>(`${this.appService.absoluteApiPath}inlinestores/overview`).subscribe((data) => {
      this.result = data;
    });
  }
}
