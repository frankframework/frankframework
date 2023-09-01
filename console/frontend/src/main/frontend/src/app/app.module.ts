import { DoBootstrap, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { UpgradeModule } from '@angular/upgrade/static';
import { AppRoutingModule } from './app-routing.module';
import { InjectionToken } from '@angular/core';
import { InputFileUploadController } from 'src/angularjs/app/components/input-file-upload/input-file-upload.component';

import '../angularjs/main';
import '../angularjs/app/app.module';
import '../angularjs/app/app.config';
import '../angularjs/services';
import '../angularjs/filters';
import '../angularjs/directives';
import '../angularjs/controllers';
import '../angularjs/components';

import { ChildComponent } from './child.component';
import { InlinestoreComponent } from './views/inlinestore/inlinestore.component';
import { JdbcBrowseTablesComponent } from './views/jdbc/jdbc-browse-tables/jdbc-browse-tables.component';
import { OrderByPipe } from './filters/orderby.pipe';
import { FormsModule } from '@angular/forms';
import { JdbcExecuteQueryComponent } from './views/jdbc/jdbc-execute-query/jdbc-execute-query.component';
import { IframeCustomViewComponent } from './views/iframe/iframe-custom-view/iframe-custom-view.component';
import { LaddaModule } from 'angular2-ladda';
import { IframeLadybugComponent } from './views/iframe/iframe-ladybug/iframe-ladybug.component';
import { IframeLadybugBetaComponent } from './views/iframe/iframe-ladybug-beta/iframe-ladybug-beta.component';
import { IframeLarvaComponent } from './views/iframe/iframe-larva/iframe-larva.component';
import { IbisstoreSummaryComponent } from './views/ibisstore-summary/ibisstore-summary.component';

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
import { AppConstants, appConstants } from '../angularjs/app/app.module';
import { LocationStrategy, PathLocationStrategy } from '@angular/common';
import { JmsBrowseQueueComponent } from './views/jms/jms-browse-queue/jms-browse-queue.component';
import { JmsSendMessageComponent } from './views/jms/jms-send-message/jms-send-message.component';
import { LiquibaseComponent } from './views/liquibase/liquibase.component';
import { FilterPipe } from './filters/filter.pipe';

export const APP_APPCONSTANTS = new InjectionToken<AppConstants>('app.appConstants');

@NgModule({
  declarations: [
    ChildComponent,
    InlinestoreComponent,
    JdbcBrowseTablesComponent,
    OrderByPipe,
    JdbcExecuteQueryComponent,
    IframeCustomViewComponent,
    IframeLadybugComponent,
    IframeLadybugBetaComponent,
    IframeLarvaComponent,
    IbisstoreSummaryComponent,
    JmsBrowseQueueComponent,
    JmsSendMessageComponent,
    LiquibaseComponent,
    FilterPipe
  ],
  imports: [
    BrowserModule,
    UpgradeModule,
    FormsModule,
    LaddaModule
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
    toastrServiceProvider,
    { provide: Window, useValue: window },
    { provide: APP_APPCONSTANTS, useValue: appConstants }, // Use AngularJS Injector to get value
    Location, { provide: LocationStrategy, useClass: PathLocationStrategy }
  ],
})

export class AppModule implements DoBootstrap {
  constructor(private upgrade: UpgradeModule) { }
  ngDoBootstrap() {
    this.upgrade.bootstrap(document.documentElement, ['iaf.beheerconsole']);
    // this.upgrade.bootstrap(document.body, ['iaf.beheerconsole'], { strictDi: true });
  }
}
