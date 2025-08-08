import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';

export type StatisticsKeeper = {
  name: string;
  count: number;
  min: number;
  max: number;
  avg: number;
  stdDev: number;
  sum: number;
  first: number;
  last: number;
};

export type StatisticDetails = StatisticsKeeper & {
  p50: number;
  p90: number;
  p95: number;
  p98: number;
};

export type StatisticDetailsTime = StatisticDetails & {
  '100ms': number;
  '1000ms': number;
  '2000ms': number;
  '10000ms': number;
};

export type StatisticDetailsSize = StatisticDetails & {
  '100000B': number;
  '1000000B': number;
};

export type Statistics = {
  types: string[];
  totalMessageProccessingTime: StatisticDetailsTime;
  receivers: {
    class: string;
    idle: unknown[];
    messagesReceived: number;
    messagesRetried: number;
    name: string;
    processing: StatisticsKeeper[];
  }[];
  durationPerPipe: StatisticDetailsTime[];
  hourly: {
    count: number;
    time: string;
  }[];
  sizePerPipe: StatisticDetailsSize[];
  labels: string[];
};

@Injectable({
  providedIn: 'root',
})
export class AdapterstatisticsService {
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);
  private readonly Misc: MiscService = inject(MiscService);

  getStatistics(configurationName: string, adapterName: string): Observable<Statistics> {
    return this.http.get<Statistics>(
      `${this.appService.absoluteApiPath}configurations/${this.Misc.escapeURL(
        configurationName,
      )}/adapters/${this.Misc.escapeURL(adapterName)}/statistics`,
    );
  }
}
