import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AppService } from 'src/app/app.service';

export type JDBC = {
  queryTypes: string[]
  datasources: string[]
  resultTypes: string[]
}

export interface JdbcBrowseForm {
  datasource: string
  resultType: string
  table: string
  where: string
  order: string
  numberOfRowsOnly: boolean
  minRow: number
  maxRow: number
}

export interface JdbcQueryForm {
  query: string
  queryType: string
  datasource: string
  resultType: string
  avoidLocking: boolean
  trimSpaces: boolean
}

export interface JdbcBrowseReturnData {
  query: string
  fielddefinition: Record<string, string>
  result: Record<string, Record<string, string>>
}

@Injectable({
  providedIn: 'root'
})
export class JdbcService {

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) { }

  getJdbc(){
    return this.http.get<JDBC>(this.appService.absoluteApiPath + "jdbc");
  }

  postJdbcBrowse(formData: JdbcBrowseForm) {
    return this.http.post<JdbcBrowseReturnData>(this.appService.absoluteApiPath + "jdbc/browse", formData);
  }

  postJdbcQuery(formData: JdbcQueryForm) {
    return this.http.post(this.appService.absoluteApiPath + "jdbc/query", formData, { responseType: 'text' });
  }

  postJdbcLiquibase(formData: FormData) {
    return this.http.post<{ result: string }>(this.appService.absoluteApiPath + "jdbc/liquibase", formData);
  }

}
