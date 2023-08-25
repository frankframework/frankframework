import { appModule } from "../app.module";

export class Base64Service {
  encode(input: string): string {
    return btoa(input);
  }

  decode(input: string): string {
    return atob(input);
  }
}

appModule.factory('Base64', function () {
	return new Base64Service();
});
