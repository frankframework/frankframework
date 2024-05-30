import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Client } from '@stomp/stompjs';

@Component({
  selector: 'app-websocket-test',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './websocket-test.component.html',
  styleUrl: './websocket-test.component.scss',
})
export class WebsocketTestComponent implements OnInit {
  constructor() {}

  ngOnInit(): void {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/iaf-example/gs-guide-websocket',
      onConnect: (): void => {
        client.subscribe('/topic/greetings', (message) => {
          console.log(`Received: ${message.body}`);
        });
        client.publish({
          destination: '/app/hello',
          body: JSON.stringify({ name: 'Vivy' }),
        });
      },
    });

    client.activate();
  }
}
