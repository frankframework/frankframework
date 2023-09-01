import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';

@Component({
  selector: 'app-inlinestore',
  templateUrl: './inlinestore.component.html',
  styleUrls: ['./inlinestore.component.scss'],
})
export class InlinestoreComponent implements OnInit {
  result: any;
  getProcessStateIcon?: (processState: string) => "fa-server" | "fa-gears" | "fa-sign-in" | "fa-pause-circle" | "fa-times-circle";
  getProcessStateIconColor?: (processState: string) => "success" | "warning" | "danger";

  constructor(
    private apiService: ApiService,
    private appService: AppService,
  ) { };

  ngOnInit(): void {
    this.apiService.Get("inlinestores/overview", (data) => {
      this.result = data;
    });

    this.getProcessStateIcon = this.appService["getProcessStateIcon"];
    this.getProcessStateIconColor = this.appService["getProcessStateIconColor"];
  };
}
