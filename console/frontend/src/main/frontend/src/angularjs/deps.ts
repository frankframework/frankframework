import * as Pace from 'pace-js';
Pace.start({
  ajax: false
});

import * as jQuery from 'jquery';
// import './plugins/iCheck/icheck.min';
// import 'datatables.net/js/jquery.dataTables.min';
// import 'chart.js';
// import 'mermaid';

import 'metismenu';

import * as angular from 'angular';
import 'angular-animate';
import 'angular-aria';
import 'angular-cookies';
import 'angular-loader';
import 'angular-messages';
import 'angular-mocks';
import 'angular-parse-ext';
import 'angular-resource';
import 'angular-route';
import 'angular-sanitize';
import 'angular-touch';
import 'angular-ui-bootstrap';
import 'angular-ui-router';
import 'ng-idle/angular-idle';
import 'angular-ladda';
import 'angularjs-toaster';
import 'angular-datatables/dist/angular-datatables';
import 'angular-datatables/dist/plugins/buttons/angular-datatables.buttons';
import 'angular-chart.js';
import './plugins/mermaid/ng-mermaid';

const $ = jQuery;

export {
  jQuery, $,
  angular,
  Pace
};
