import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { getProcessStateIcon, getProcessStateIconColor } from 'src/app/utilities';
import { KeyValuePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

type stateItemItem = {
  configurationName: string;
  adapterName: string;
  receiverName: string;
  messageCount: number;
};

type InlineStore = Record<string, { items: stateItemItem[]; totalMessageCount: number }>;

@Component({
  selector: 'app-inlinestore',
  imports: [RouterLink, KeyValuePipe],
  templateUrl: './inlinestore.component.html',
  styleUrls: ['./inlinestore.component.scss'],
})
export class InlinestoreComponent implements OnInit {
  protected readonly getProcessStateIconColorFn = getProcessStateIconColor;
  protected readonly getProcessStateIconFn = getProcessStateIcon;
  protected result: InlineStore = {};

  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);

  ngOnInit(): void {
    this.http.get<InlineStore>(`${this.appService.absoluteApiPath}inlinestores/overview`).subscribe((data) => {
      this.result = data;
    });
  }
}
