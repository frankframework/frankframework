import { inject, Injectable, Signal, signal, WritableSignal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AppService } from '../app.service';
import { HttpClient } from '@angular/common/http';
import { HumanFileSizePipe } from '../pipes/human-file-size.pipe';
import { ServerTimeService } from './server-time.service';

export type ServerInfo = {
  fileSystem: {
    freeSpace: number;
    totalSpace: number;
  };
  framework: {
    name: string;
    version: string;
  };
  instance: {
    name: string;
    version: string;
  };
  applicationServer: string;
  javaVersion: string;
  serverTime: number;
  serverTimezone: string;
  serverTimezoneOffset: number;
  'dtap.stage': string;
  'dtap.side': string;
  processMetrics: {
    maxMemory: number;
    freeMemory: number;
    totalMemory: number;
    heapSize: number;
  };
  machineName: string;
  uptime: number;
  userName?: string;
};

export type ConsoleInfo = {
  version: string | null;
};

@Injectable({
  providedIn: 'root',
})
export class ServerInfoService {
  private _serverInfo: WritableSignal<ServerInfo | null> = signal(null);
  private _consoleInfo: WritableSignal<ConsoleInfo> = signal({
    version: null,
  });

  private readonly serverTimeService: ServerTimeService = inject(ServerTimeService);
  private readonly appService: AppService = inject(AppService);
  private readonly http: HttpClient = inject(HttpClient);

  get consoleInfo(): Signal<ConsoleInfo> {
    return this._consoleInfo.asReadonly();
  }

  get serverInfo(): Signal<ServerInfo | null> {
    return this._serverInfo.asReadonly();
  }

  fetchServerInfo(): Observable<ServerInfo> {
    return this.http.get<ServerInfo>(`${this.appService.absoluteApiPath}server/info`);
  }

  fetchConsoleVersion(): Observable<ConsoleInfo> {
    return this.http.get<ConsoleInfo>(`${this.appService.absoluteApiPath}console/info`);
  }

  refresh(): Observable<ServerInfo> {
    this.fetchConsoleVersion().subscribe((info) => this._consoleInfo.set(info));
    return this.fetchServerInfo().pipe(tap((data) => this._serverInfo.set(data)));
  }

  getMarkdownFormatedServerInfo(): string {
    if (!this.serverInfo) return '**Server info not available**';
    const humanFileSize = new HumanFileSizePipe();
    const serverInfo = this.serverInfo();
    const consoleVersion = this.consoleInfo().version ?? 'null';
    return `**${serverInfo?.framework.name}${serverInfo?.framework.version ? ` ${serverInfo?.framework.version}` : ''}**: ${serverInfo?.instance.name} ${serverInfo?.instance.version}
Running on **${serverInfo?.machineName}** using **${serverInfo?.applicationServer}**
Java Version: **${serverInfo?.javaVersion}**
Heap size: **${humanFileSize.transform(serverInfo?.processMetrics.heapSize ?? 0)}**, total JVM memory: **${humanFileSize.transform(serverInfo?.processMetrics?.totalMemory ?? 0)}**
Free memory: **${humanFileSize.transform(serverInfo?.processMetrics.freeMemory ?? 0)}**, max memory: **${humanFileSize.transform(serverInfo?.processMetrics.maxMemory ?? 0)}**
Free disk space: **${humanFileSize.transform(serverInfo?.fileSystem.freeSpace ?? 0)}**, total disk space: **${humanFileSize.transform(serverInfo?.fileSystem.totalSpace ?? 0)}**
Up since: **${this.serverTimeService.getIntialTime()}**, timezone: **${serverInfo?.serverTimezone}**
${consoleVersion === serverInfo?.framework.version ? '' : `Console version: **${consoleVersion}**`}`;
  }
}
