import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import type * as angularjs from 'angular';
import type * as jQuery from 'jquery';

platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => console.error(err));

declare global {
  var ff_version: string;
  var serverurl: string;
  // var jQuery: jQuery; already defined in @types/jquery (type import solves this for us?)
  // var $: jQuery;
  var angular: angularjs.IAngularStatic;
}
