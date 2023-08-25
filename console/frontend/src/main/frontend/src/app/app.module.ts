import { DoBootstrap, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { UpgradeModule } from '@angular/upgrade/static';

import { AppRoutingModule } from './app-routing.module';

import '../angularjs/main';
import '../angularjs/app/app.module';
import '../angularjs/app/app.config';
import '../angularjs/services';
import '../angularjs/filters';
import '../angularjs/directives';
import '../angularjs/controllers';
import '../angularjs/components';

import { ChildComponent } from './child.component';

import {
  alertServiceProvider,
  apiServiceProvider,
  authServiceProvider,
  base64ServiceProvider,
  cookiesServiceProvider,
  debugServiceProvider,
  gdprServiceProvider,
  miscServiceProvider,
  notificationServiceProvider,
  pollerServiceProvider,
  sessionServiceProvider,
  sweetalertServiceProvider,
  toastrServiceProvider
} from './ajs-upgraded-services';

@NgModule({
  declarations: [ChildComponent],
  imports: [
    BrowserModule,
    UpgradeModule,
    // AppRoutingModule
  ],
  providers: [
    alertServiceProvider,
    apiServiceProvider,
    authServiceProvider,
    base64ServiceProvider,
    cookiesServiceProvider,
    debugServiceProvider,
    gdprServiceProvider,
    miscServiceProvider,
    notificationServiceProvider,
    pollerServiceProvider,
    sessionServiceProvider,
    sweetalertServiceProvider,
    toastrServiceProvider
  ],
})
export class AppModule implements DoBootstrap {
  constructor(private upgrade: UpgradeModule) {}
  ngDoBootstrap() {
    this.upgrade.bootstrap(document.documentElement, ['iaf.beheerconsole']);
    // this.upgrade.bootstrap(document.body, ['iaf.beheerconsole'], { strictDi: true });
  }
}
