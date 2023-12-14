import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InlinestoreComponent } from './inlinestore.component';

describe('InlinestoreComponent', () => {
  let component: InlinestoreComponent;
  let fixture: ComponentFixture<InlinestoreComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ InlinestoreComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InlinestoreComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
