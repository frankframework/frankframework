import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { MiscService } from 'src/app/services/misc.service';

export type StatisticsKeeper = {
  name: string;
  count: number;
  min: number | null;
  max: number | null;
  avg: number | null;
  stdDev: number | null;
  sum: number | null;
  first: number | null;
  last: number | null;
};

type StatisticDetails = StatisticsKeeper & {
  p50: number | null;
  p90: number | null;
  p95: number | null;
  p98: number | null;
};

type StatisticDetailsTime = StatisticDetails & {
  '100ms': null;
  '1000ms': null;
  '2000ms': null;
  '10000ms': null;
};

type StatisticDetailsSize = StatisticDetails & {
  '100000B': null;
  '1000000B': null;
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
  constructor(
    private http: HttpClient,
    private appService: AppService,
    private Misc: MiscService,
  ) {}

  getStatistics(configurationName: string, adapterName: string): Observable<Statistics> {
    return this.http.get<Statistics>(
      `${this.appService.absoluteApiPath}configurations/${this.Misc.escapeURL(
        configurationName,
      )}/adapters/${this.Misc.escapeURL(adapterName)}/statistics`,
    );
  }
}
