import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JmsSendMessageComponent } from './jms-send-message.component';

describe('JmsSendMessageComponent', () => {
  let component: JmsSendMessageComponent;
  let fixture: ComponentFixture<JmsSendMessageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ JmsSendMessageComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(JmsSendMessageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
