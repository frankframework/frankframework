import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WebsocketTestComponent } from './websocket-test.component';

describe('WebsocketTestComponent', () => {
  let component: WebsocketTestComponent;
  let fixture: ComponentFixture<WebsocketTestComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebsocketTestComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(WebsocketTestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
