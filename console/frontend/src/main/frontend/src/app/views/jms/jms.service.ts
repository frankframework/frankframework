import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from 'src/app/app.service';

export interface JmsBrowseForm {
  destination: string;
  connectionFactory: string;
  type: string;
  rowNumbersOnly: boolean;
  payload: boolean;
  lookupDestination: boolean;
}

interface JmsBrowseResponse {
  numberOfMessages: number;
  messages: Message[];
}

export interface Message {
  id: string;
  correlationId: string;
  text: string;
  insertDate: string;
  host: string;
  comment: string;
  expiryDate?: number;
}

@Injectable({
  providedIn: 'root',
})
export class JmsService {
  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  getJms(): Observable<{
    connectionFactories: string[];
  }> {
    return this.http.get<{ connectionFactories: string[] }>(`${this.appService.absoluteApiPath}jms`);
  }

  postJmsBrowse(formData: JmsBrowseForm): Observable<JmsBrowseResponse> {
    return this.http.post<JmsBrowseResponse>(`${this.appService.absoluteApiPath}jms/browse`, formData);
  }
  postJmsMessage(formData: FormData): Observable<object> {
    return this.http.post(`${this.appService.absoluteApiPath}jms/message`, formData);
  }
}
