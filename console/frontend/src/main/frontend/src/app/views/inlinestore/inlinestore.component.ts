import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { AppService } from '../../app.service';
import { getProcessStateIcon, getProcessStateIconColor } from '../../utilities';

type stateItemItem = {
  configurationName: string;
  adapterName: string;
  receiverName: string;
  messageCount: number;
};

type InlineStore = Record<string, { items: stateItemItem[]; totalMessageCount: number }>;

@Component({
  selector: 'app-inlinestore',
  imports: [RouterLink, KeyValuePipe, FaIconComponent],
  templateUrl: './inlinestore.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
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
