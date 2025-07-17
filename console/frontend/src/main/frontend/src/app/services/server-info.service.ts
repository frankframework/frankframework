import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, ReplaySubject, tap } from 'rxjs';
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
  private serverInfoSubject = new ReplaySubject<ServerInfo>(1);
  public serverInfo$ = this.serverInfoSubject.asObservable();
  private consoleInfoSubject = new BehaviorSubject<ConsoleInfo>({
    version: null,
  });
  public consoleVersion$ = this.consoleInfoSubject.asObservable();

  private readonly serverTimeService: ServerTimeService = inject(ServerTimeService);
  private readonly appService: AppService = inject(AppService);
  private readonly http: HttpClient = inject(HttpClient);
  private serverInfo?: ServerInfo;
  private consoleInfo?: ConsoleInfo;

  fetchServerInfo(): Observable<ServerInfo> {
    return this.http.get<ServerInfo>(`${this.appService.absoluteApiPath}server/info`);
  }

  fetchConsoleVersion(): Observable<ConsoleInfo> {
    return this.http.get<ConsoleInfo>(`${this.appService.absoluteApiPath}console/info`);
  }

  refresh(): Observable<ServerInfo> {
    this.fetchConsoleVersion().subscribe({
      next: (info) => {
        this.consoleInfo = info;
        this.consoleInfoSubject.next(info);
      },
    });
    return this.fetchServerInfo().pipe(
      tap({
        next: (data) => {
          this.serverInfo = data;
          this.serverInfoSubject.next(data);
        },
      }),
    );
  }

  getMarkdownFormatedServerInfo(): string {
    if (!this.serverInfo) return '**Server info not available**';
    const humanFileSize = new HumanFileSizePipe();
    return `**${this.serverInfo?.framework.name}${this.serverInfo?.framework.version ? ` ${this.serverInfo?.framework.version}` : ''}**: ${this.serverInfo?.instance.name} ${this.serverInfo?.instance.version}
Running on **${this.serverInfo?.machineName}** using **${this.serverInfo?.applicationServer}**
Java Version: **${this.serverInfo?.javaVersion}**
Heap size: **${humanFileSize.transform(this.serverInfo?.processMetrics.heapSize ?? 0)}**, total JVM memory: **${humanFileSize.transform(this.serverInfo?.processMetrics?.totalMemory ?? 0)}**
Free memory: **${humanFileSize.transform(this.serverInfo?.processMetrics.freeMemory ?? 0)}**, max memory: **${humanFileSize.transform(this.serverInfo?.processMetrics.maxMemory ?? 0)}**
Free disk space: **${humanFileSize.transform(this.serverInfo?.fileSystem.freeSpace ?? 0)}**, total disk space: **${humanFileSize.transform(this.serverInfo?.fileSystem.totalSpace ?? 0)}**
Up since: **${this.serverTimeService.getIntialTime()}**, timezone: **${this.serverInfo?.serverTimezone}**
${this.consoleInfo?.version === this.serverInfo.framework.version ? '' : `Console version: **${this.consoleInfo?.version ?? 'null'}**`}`;
  }
}
