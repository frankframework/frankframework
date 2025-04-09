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
  serverTimeISO: string;
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

@Injectable({
  providedIn: 'root',
})
export class ServerInfoService {
  private readonly serverTimeService: ServerTimeService = inject(ServerTimeService);
  private serverInfoSubject = new ReplaySubject<ServerInfo>(1);
  private consoleVersionSubject = new BehaviorSubject<string>('null');

  serverInfo$ = this.serverInfoSubject.asObservable();
  consoleVersion$ = this.consoleVersionSubject.asObservable();

  private serverInfo?: ServerInfo;

  constructor(
    private appService: AppService,
    private http: HttpClient,
  ) {}

  fetchServerInfo(): Observable<ServerInfo> {
    return this.http.get<ServerInfo>(`${this.appService.absoluteApiPath}server/info`);
  }

  fetchConsoleVersion(): Observable<string> {
    return this.http.get<string>(`${this.appService.absoluteApiPath}server/version`);
  }

  refresh(): Observable<ServerInfo> {
    this.fetchConsoleVersion().subscribe({
      next: (version) => this.consoleVersionSubject.next(version),
      error: () => this.consoleVersionSubject.next('null'),
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
Up since: **${this.serverTimeService.getIntialTime()}**, timezone: **${this.serverInfo?.serverTimezone}**`;
  }
}
