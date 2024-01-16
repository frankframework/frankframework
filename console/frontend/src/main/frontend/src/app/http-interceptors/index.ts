import { HTTP_INTERCEPTORS } from "@angular/common/http";
import { AuthInterceptor } from "./auth-interceptor";
import { EtagsInterceptor } from "./etags-interceptor";
import { AllowInterceptor } from "./allow-interceptor";
import { ConnectionInterceptor } from "./connection-interceptor";

/** Http interceptor providers in outside-in order */
export const httpInterceptorProviders = [
  { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
  { provide: HTTP_INTERCEPTORS, useClass: EtagsInterceptor, multi: true },
  { provide: HTTP_INTERCEPTORS, useClass: AllowInterceptor, multi: true },
  { provide: HTTP_INTERCEPTORS, useClass: ConnectionInterceptor, multi: true },
];
