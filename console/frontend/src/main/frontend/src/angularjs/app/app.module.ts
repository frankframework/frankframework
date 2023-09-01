import { HamburgerComponent } from 'src/app/components/pages/pages-topnavbar/hamburger.component';
import '../deps';
import { downgradeComponent } from '@angular/upgrade/static';
import { MinimalizaSidebarComponent } from 'src/app/components/pages/pages-navigation/minimaliza-sidebar.component';
import { PagesFooterComponent } from 'src/app/components/pages/pages-footer/pages-footer.component';
import { PagesNavigationComponent } from 'src/app/components/pages/pages-navigation/pages-navigation.component';
import { PagesTopinfobarComponent } from 'src/app/components/pages/pages-topinfobar/pages-topinfobar.component';
import { PagesTopnavbarComponent } from 'src/app/components/pages/pages-topnavbar/pages-topnavbar.component';
import { ScrollToTopComponent } from 'src/app/components/pages/pages-navigation/scroll-to-top.component';

var server: string; //Try and see if serverurl has been defined, if not try to deduct from local url;
try {
  server = serverurl;
}
catch (e) {
  var path = window.location.pathname;

  if (path.indexOf("/iaf/gui") >= 0)
    path = path.substr(0, path.indexOf("/iaf/gui") + 1);
  else
    if (path.indexOf("/", 1) >= 0)
      path = path.substr(0, path.indexOf("/", 1) + 1);
  server = path;
}

export const appConstants: AppConstants = {
  //Configure these in the server AppConstants!!!
  //The settings here are defaults and will be overwritten upon set in any .properties file.

  //Server to connect to, defaults to local server.
  "server": server,

  //How often the interactive frontend should poll the IAF API for new data
  "console.pollerInterval": 30000,

  //How often the interactive frontend should poll during IDLE state
  "console.idle.pollerInterval": 180000,

  //After x minutes the app goes into 'idle' state (use 0 to disable)
  "console.idle.time": 300,

  //After x minutes the user will be forcefully logged out
  "console.idle.timeout": 0,

  //Time format in which to display the time and date.
  "console.dateFormat": "yyyy-MM-dd HH:mm:ss",

  //These will automatically be updated.
  "timeOffset": 0,
  "init": -1,
  getString: function (variable: keyof AppConstants) {
    return this[variable];
  },
  getBoolean: function (variable: keyof AppConstants, dfault: any) {
    if (this[variable] != undefined) return (this[variable] === "true");
    return dfault;
  }
};

export const appModule = angular.module('iaf.beheerconsole', [
  'ngCookies',                    // Angular Cookies
  'ui.router',                    // Routing
  'ui.bootstrap',                 // Ui Bootstrap
  'ngIdle',                       // Idle timer
  'ngSanitize',                   // ngSanitize
  'angular-ladda',                // Ladda
  'toaster',                       // Toastr
  'datatables',
  'chart.js',
  'angular-mermaid'
]).constant("appConstants", appConstants);
export type AppConstants = Record<string, string | any> // typeof appConstants;
console.timeEnd("startup");
