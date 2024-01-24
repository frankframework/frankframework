import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AppService } from 'src/app/app.service';

export const errorLevelsConst = ["DEBUG", "INFO", "WARN", "ERROR"] as const;

export type ErrorLevels = typeof errorLevelsConst;

export type LoggingFile = {
  name: string;
  type: string;
  path: string;
  size: number;
  sizeDisplay: string;
  lastModified: number;
}

export type LoggingSettings = {
  errorLevels: ErrorLevels,
  logIntermediaryResults: boolean,
  enableDebugger: boolean,
  maxMessageLength: number,
  loglevel: string
}

export type LogInformation = {
  loggers: Record<string, ErrorLevels[number]>,
  definitions: {
    name: string,
    level: ErrorLevels[number],
    appenders?: string[]
  }[],
}

type Logging = {
  count: number,
  list: LoggingFile[],
  directory: string,
  wildcard: string
}

@Injectable({
  providedIn: 'root'
})
export class LoggingService {

  private logSettingsBaseURL: string = this.appService.absoluteApiPath + "server/logging";
  private logSettingsURL: string = this.logSettingsBaseURL + "/settings";

  constructor(
    private http: HttpClient,
    private appService: AppService
  ) { }

  getLogging(directory: string) {
    const url = directory.length > 0 ? `logging?directory=${directory}` : "logging";
    return this.http.get<Logging>(this.appService.absoluteApiPath + url);
  }

  getLoggingSettings() {
    return this.http.get<LoggingSettings>(this.logSettingsBaseURL);
  }

  getLoggingSettingsLogInformation(){
    return this.http.get<LogInformation>(this.logSettingsURL);
  }

  putLoggingSettingsChange(action: Record<string, any>) {
    return this.http.put(this.logSettingsURL, action);
  }

  putLoggingSettings(formData: LoggingSettings){
    return this.http.put(this.logSettingsBaseURL, formData);
  }
}
