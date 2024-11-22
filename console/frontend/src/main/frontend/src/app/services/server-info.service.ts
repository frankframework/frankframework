import { Injectable } from '@angular/core';
import { Observable, ReplaySubject, tap } from 'rxjs';
import { AppService } from '../app.service';
import { HttpClient } from '@angular/common/http';
import { HumanFileSizePipe } from '../pipes/human-file-size.pipe';

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
  private serverInfoSubject = new ReplaySubject<ServerInfo>(1);

  serverInfo$ = this.serverInfoSubject.asObservable();

  private serverInfo?: ServerInfo;

  constructor(
    private appService: AppService,
    private http: HttpClient,
  ) {}

  fetchServerInfo(): Observable<ServerInfo> {
    return this.http.get<ServerInfo>(`${this.appService.absoluteApiPath}server/info`);
  }

  refresh(): Observable<ServerInfo> {
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
Free disk space: **${humanFileSize.transform(this.serverInfo?.fileSystem.freeSpace ?? 0)}**, total disk space: **${humanFileSize.transform(this.serverInfo?.fileSystem.totalSpace ?? 0)}**`;
  }
}
