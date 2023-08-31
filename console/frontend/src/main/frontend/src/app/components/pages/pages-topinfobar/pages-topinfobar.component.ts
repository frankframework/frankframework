import { Component } from '@angular/core';
import { StateService } from '@uirouter/angularjs';
import { AppService } from 'src/angularjs/app/app.service';

@Component({
  selector: 'app-pages-topinfobar',
  templateUrl: './pages-topinfobar.component.html',
  styleUrls: ['./pages-topinfobar.component.scss']
})
export class PagesTopinfobarComponent {
  loading = true;
  currRoute = this.$state.current

  constructor(private appService: AppService, private $state: StateService) { }

  $onInit() {
    this.appService.loading$.subscribe(loading => this.loading = loading);
  }
}
