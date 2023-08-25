import { appModule } from "../app.module";
import { DebugService } from "./debug.service";

export class GDPRService {
  settings: Record<string, any> | null = null;
  defaults: {
    necessary: boolean,
    functional: boolean,
    personalization: boolean
  } = { necessary: true, functional: true, personalization: true };
  cookieName: string = "_cookieSettings";
  options: { expires: Date, path: string };

  constructor(
    private $cookies: angular.cookies.ICookiesService,
    private $rootScope: angular.IRootScopeService,
    private Debug: DebugService
  ){
    var date = new Date();
    date.setFullYear(date.getFullYear() + 10);
    this.options = {
      expires: date,
      path: '/'
    };
  }

  showCookie(): boolean {
    this.getSettings();
    return this.settings == null;
  };

  getSettings(): Record<string, any> | null {
    if (this.settings == null) {
      var cookie = this.$cookies.getObject(this.cookieName);
      if (cookie != undefined) {
        this.Debug.log("fetch cookie with GDPR settings", cookie);
        this.settings = cookie;

        //Extend the cookie lifetime by another 10 years
        this.$cookies.putObject(this.cookieName, cookie, this.options);
      }
      else {
        this.Debug.log("unable to find GDPR settings, falling back to defaults", this.defaults);
        return this.defaults;
      }

      this.Debug.info("set GDPR settings to", this.settings);
    }
    return this.settings;
  }

  allowFunctional(): boolean {
    const settings = this.getSettings();
    return settings ? settings["functional"] : false;
  }

  allowPersonalization(): boolean {
    const settings = this.getSettings();
    return settings ? settings["personalization"] : false;
  }

  setSettings(settings: Record<string, any>): void {
    this.settings = settings;
    this.$cookies.putObject(this.cookieName, settings, this.options);

    this.$rootScope.$broadcast('GDPR');
  };

}

appModule.service('GDPR', ['$cookies', '$rootScope', 'Debug', GDPRService]);
