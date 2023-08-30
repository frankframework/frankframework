import { Component, Inject, OnInit } from '@angular/core';
import { ApiService } from 'src/app/services.types';

@Component({
  selector: 'app-inlinestore',
  templateUrl: './inlinestore.component.html',
  styleUrls: ['./inlinestore.component.scss'],
})
export class InlinestoreComponent implements OnInit {
  result: any;
  getProcessStateIcon?: "fa-server" | "fa-gears" | "fa-sign-in" | "fa-pause-circle" | "fa-times-circle";
  getProcessStateIconColor?: "success" | "warning" | "danger";

  constructor(
    @Inject("apiService") private apiService: ApiService,
    @Inject("appService") private appService: any,
  ) { };

  ngOnInit(): void {
    this.apiService.Get("inlinestores/overview", (data) => {
      this.result = data;
    });

    this.getProcessStateIcon = this.appService["getProcessStateIconColor"];
    this.getProcessStateIconColor = this.appService["getProcessStateIconColor"];
  };
}
