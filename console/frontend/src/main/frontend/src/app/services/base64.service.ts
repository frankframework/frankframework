import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class Base64Service {
  encode(input: string): string {
    return btoa(input);
  }

  decode(input: string): string {
    return atob(input);
  }
}
