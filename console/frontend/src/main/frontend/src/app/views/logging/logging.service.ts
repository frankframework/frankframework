import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AppService } from 'src/app/app.service';

@Injectable({
  providedIn: 'root'
})
export class LoggingService {

  constructor(
    private http: HttpClient,
    private appService: AppService
  ) { }

  getLogging(directory: string){
    const url = directory.length > 0 ? "logging?directory=" + directory  : "logging";
    return this.http.get(this.appService.absoluteApiPath + url);
  }
}
