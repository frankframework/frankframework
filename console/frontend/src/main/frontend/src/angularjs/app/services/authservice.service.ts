import { AppConstants, appModule } from "../app.module";
import { Base64Service } from "./base64.service";
import { MiscService } from "./misc.service";

export class AuthService {

  private authToken?: string;

  constructor(
    private $rootScope: angular.IRootScopeService,
    private $http: angular.IHttpService,
    private Base64: Base64Service,
    private $location: angular.ILocationService,
    private appConstants: AppConstants,
    private Misc: MiscService
  ){}

  login(username: string, password: string): void {
    if (username != "anonymous") {
      this.authToken = this.Base64.encode(username + ':' + password);
      sessionStorage.setItem('authToken', this.authToken);
      this.$http.defaults.headers!.common['Authorization'] = 'Basic ' + this.authToken;
    }
    var location = sessionStorage.getItem('location') || "status";
    var absUrl = window.location.href.split("login")[0];
    window.location.href = (absUrl + location);
    window.location.reload();
  }

  loggedin(): void {
    var token = sessionStorage.getItem('authToken');
    if (token != null && token != "null") {
      this.$http.defaults.headers!.common['Authorization'] = 'Basic ' + token;
      if (this.$location.path().indexOf("login") >= 0)
        this.$location.path(sessionStorage.getItem('location') || "status");
    }
    else {
      if (this.appConstants["init"] > 0) {
        if (this.$location.path().indexOf("login") < 0)
          sessionStorage.setItem('location', this.$location.path() || "status");
        this.$location.path("login");
      }
    }
  }

  logout(): void {
    sessionStorage.clear();
    this.$http.defaults.headers!.common['Authorization'] = null;
    this.$http.get(this.Misc.getServerPath() + "iaf/api/logout");
  }
}

appModule.factory('authService', ['$rootScope', '$http', 'Base64', '$location', 'appConstants', 'Misc',
  function (
    $rootScope: angular.IRootScopeService,
    $http: angular.IHttpService,
    Base64: Base64Service,
    $location: angular.ILocationService,
    appConstants: AppConstants,
    Misc: MiscService
  ) {
		return new AuthService($rootScope, $http, Base64, $location, appConstants, Misc);
	}]);
