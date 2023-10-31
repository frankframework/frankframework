import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AppService } from 'src/app/app.service';

export type JDBC = {
  "queryTypes": string[],
  "datasources": string[],
  "resultTypes": string[]
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

  postJdbcBrowse(formData: string) {
    return this.http.post<JdbcBrowseReturnData>(this.appService.absoluteApiPath + "jdbc/browse", formData);
  }

}
