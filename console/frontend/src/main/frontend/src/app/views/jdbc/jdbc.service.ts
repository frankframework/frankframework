import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from 'src/app/app.service';

interface Date {
  id: string;
  count: number;
}

interface Slot {
  id: string;
  configuration: string;
  adapter: string;
  receiver: string;
  pipe: string;
  msgcount: number;
  dates: Date[];
}

interface JdbcBrowseReturnData {
  query: string;
  fielddefinition: Record<string, string>;
  result: Record<string, Record<string, string>>;
}

interface JdbcSummaryReturnData {
  result: JdbcSummary[];
}

export type JDBC = {
  queryTypes: string[];
  datasources: string[];
  resultTypes: string[];
};

export interface JdbcBrowseForm {
  datasource: string;
  resultType: string;
  table: string;
  where: string;
  order: string;
  numberOfRowsOnly: boolean;
  minRow: number;
  maxRow: number;
}

export interface JdbcQueryForm {
  query: string;
  queryType: string;
  datasource: string;
  resultType: string;
  avoidLocking: boolean;
  trimSpaces: boolean;
}

export interface JdbcSummaryForm {
  datasource: string;
}

export interface JdbcSummary {
  name: string;
  slotcount: number;
  slots?: Slot[];
  msgcount: number;
  type: string;
}

@Injectable({
  providedIn: 'root',
})
export class JdbcService {
  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  getJdbc(): Observable<JDBC> {
    return this.http.get<JDBC>(`${this.appService.absoluteApiPath}jdbc`);
  }

  postJdbcBrowse(formData: JdbcBrowseForm): Observable<JdbcBrowseReturnData> {
    return this.http.post<JdbcBrowseReturnData>(`${this.appService.absoluteApiPath}jdbc/browse`, formData);
  }

  postJdbcQuery(formData: JdbcQueryForm): Observable<string> {
    return this.http.post(`${this.appService.absoluteApiPath}jdbc/query`, formData, { responseType: 'text' });
  }

  postJdbcSummary(formData: JdbcSummaryForm): Observable<JdbcSummaryReturnData> {
    return this.http.post<JdbcSummaryReturnData>(`${this.appService.absoluteApiPath}jdbc/summary`, formData);
  }

  postJdbcLiquibase(formData: FormData): Observable<{
    result: string;
  }> {
    return this.http.post<{ result: string }>(`${this.appService.absoluteApiPath}jdbc/liquibase`, formData);
  }
}
