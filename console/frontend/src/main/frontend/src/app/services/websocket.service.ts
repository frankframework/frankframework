import { Injectable, isDevMode } from '@angular/core';
import { Client, IFrame, IMessage, IStompSocket, StompSubscription } from '@stomp/stompjs';
import { AppService, ClusterMember } from '../app.service';
import { Subject } from 'rxjs';

type ChannelMessage = {
  channel: string;
  message: IMessage;
};

export type ClusterMemberEventType = 'ADD_MEMBER' | 'REMOVE_MEMBER';

export type ClusterMemberEvent = {
  type: ClusterMemberEventType;
  member: ClusterMember;
};

@Injectable({
  providedIn: 'root',
})
export class WebsocketService {
  private onConnectedSubject = new Subject<void>();
  private onDisconnectedSubject = new Subject<void>();
  private onStompErrorSubject = new Subject<IFrame>();
  private onWebSocketCloseSubject = new Subject<void>();
  private onWebSocketErrorSubject = new Subject<Error>();
  private onMessageSubject = new Subject<ChannelMessage>();

  onConnected$ = this.onConnectedSubject.asObservable();
  onDisconnected$ = this.onDisconnectedSubject.asObservable();
  onStompError$ = this.onStompErrorSubject.asObservable();
  onWebSocketClose$ = this.onWebSocketCloseSubject.asObservable();
  onWebSocketError$ = this.onWebSocketErrorSubject.asObservable();
  onMessage$ = this.onMessageSubject.asObservable();

  private baseUrl: string = `${window.location.host}${this.appService.absoluteApiPath}`;
  private errorCount: number = 0;
  private client: Client = new Client({
    brokerURL: `ws://${this.baseUrl}ws`,
    connectionTimeout: 3000,
    debug: (message) => (): void => {
      if (isDevMode()) console.debug(message);
    },
    onConnect: (): void => {
      this.errorCount = 0;
      this.onConnectedSubject.next();
    },
    onDisconnect: (): void => {
      this.onDisconnectedSubject.next();
    },
    onStompError: (frame): void => {
      console.error(`Broker reported error: ${frame.headers['message']}`);
      console.error(`Additional details: ${frame.body}`);
    },
    onWebSocketClose: () => this.onWebSocketCloseSubject.next(),
    onWebSocketError: (event): void => {
      this.errorCount += 1;
      if (this.errorCount > 1) {
        this.enableSockJs();
      }
      this.onWebSocketErrorSubject.next(event);
    },
  });
  private stompSubscriptions: Map<string, StompSubscription> = new Map<string, StompSubscription>();

  constructor(private appService: AppService) {}

  activate(): void {
    if (!this.client.connected) {
      if (typeof WebSocket !== 'function') {
        this.enableSockJs();
      }
      this.client.activate();
    }
  }

  deactivate(): void {
    if (this.client.connected) {
      this.client.deactivate();
    }
  }

  connected(): boolean {
    return this.client.connected;
  }

  publish(channel: string, message: string): void {
    this.client.publish({ destination: channel, body: message });
  }

  subscribe<T>(channel: string, callback?: (message: T) => void): void {
    const subscription = this.client.subscribe(channel, (message: IMessage) => {
      this.onMessageSubject.next({ channel, message });
      if (callback) callback(JSON.parse(message.body) as T);
    });
    this.stompSubscriptions.set(channel, subscription);
  }

  unsubscribe(channel: string): void {
    const subscription = this.stompSubscriptions.get(channel);
    if (subscription) {
      subscription.unsubscribe();
      this.stompSubscriptions.delete(channel);
    }
  }

  private enableSockJs(): void {
    this.client.webSocketFactory = (): IStompSocket => {
      return new window.SockJS(`http://${this.baseUrl}stomp`, undefined, {
        transports: [
          'xhr-streaming',
          'xhr-polling',
          // IE 6-9 support
          'xdr-streaming',
          'xdr-polling',
          'jsonp-polling',
        ],
      }) as IStompSocket;
    };
  }
}
