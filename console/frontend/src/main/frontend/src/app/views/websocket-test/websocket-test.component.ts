import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Client, IMessage } from '@stomp/stompjs';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-websocket-test',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, HttpClientModule],
  templateUrl: './websocket-test.component.html',
  styleUrl: './websocket-test.component.scss',
})
export class WebsocketTestComponent implements OnInit, OnDestroy {
  @ViewChild('wsLog')
  private wsLog!: ElementRef<HTMLPreElement>;

  protected message: string = '';

  private client: Client = new Client({
    brokerURL: 'ws://localhost:4200/iaf/api/ws',
    connectionTimeout: 60_000,
    debug: (message) => console.debug(message),
    onConnect: () => this.onConnected(),
  });

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.client.activate();
  }

  ngOnDestroy(): void {
    this.client.deactivate();
  }

  onConnected(): void {
    this.client.subscribe(
      '/event/greetings',
      this.debugHandler('/event/greetings'),
    );
    this.client.subscribe('/event/test', this.debugHandler('/event/test'));
    this.client.publish({
      destination: '/hello',
      body: JSON.stringify({ name: 'Vivy' }),
    });
  }

  push(): void {
    this.http
      .post(
        `${this.appService.absoluteApiPath}event/push`,
        JSON.stringify({ message: this.message }),
        {
          headers: { 'Content-Type': 'application/json' },
        },
      )
      .subscribe(() => {
        this.message = '';
      });
  }

  private debugHandler(channel: string): (message: IMessage) => void {
    const htmlLogElement: HTMLPreElement = this.wsLog.nativeElement;
    return (message: IMessage): void => {
      htmlLogElement.innerHTML += `<p>${channel} | ${message.body}</p>`;
      htmlLogElement.scrollTo(0, htmlLogElement.scrollHeight);
    };
  }
}
