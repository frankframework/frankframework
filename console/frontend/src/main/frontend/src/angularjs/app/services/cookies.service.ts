import { appModule } from "../app.module";
import { DebugService } from "./debug.service";
import { GDPRService } from "./gdpr.service";

export class CookiesService {
  cache: Record<string, any> | null = null;
  options: { expires: Date, path: string };

  constructor(
    private Debug: DebugService,
    private $cookies: angular.cookies.ICookiesService,
    private GDPR: GDPRService,
    private $rootScope: angular.IRootScopeService
  ){
    const date = new Date();
    date.setDate(date.getDate() + 7);
    this.options = {
      expires: date,
      path: '/'
    };

    //Runs everytime the GDPR settings update
    $rootScope.$on('GDPR', () => {
      this.flushCache();
    });
  }

  addToCache(key: string, value: any): void {
    this.Debug.log("adding cookie[" + key + "] to cache");

    if (this.cache == null)
      this.cache = {};

    //If the same key is set twice, just overwrite the old setting
    this.cache[key] = value;
  }

  flushCache(): void {
    this.Debug.info("trying to save cookies from cache", this.cache);

    if (this.GDPR.allowFunctional() === true) { //Only run when explicitly set to true
      for (const c in this.cache) {
        this.set(c, this.cache[c]);
      }
      this.cache = null; //Clear the cache, free up memory :)
    }
  }

  get(key: string): any {
    var val = null;
    if (this.cache != null) //Maybe a cookie has been set but the user has not accepted cookies?
      val = this.cache[key];
    if (val == null)
      val = this.$cookies.getObject(key);
    return val;
  }

  set(key: string, value: any): void {
    if (this.GDPR.allowFunctional())
      this.$cookies.putObject(key, value, this.options); //Only actually set the cookie when allowed to
    else
      this.addToCache(key, value); //Cache the request while the user hasn't selected their preference or disallowed functional cookies
  }

  remove(key: string): void {
    this.$cookies.remove(key, { path: '/' });
  }

  clear(): void {
    for (const key in this.$cookies.getAll()) {
      if (!key.startsWith("_"))
        this.remove(key);
    }
  };
}

appModule.service('Cookies', ['Debug', '$cookies', 'GDPR', '$rootScope', CookiesService]);
