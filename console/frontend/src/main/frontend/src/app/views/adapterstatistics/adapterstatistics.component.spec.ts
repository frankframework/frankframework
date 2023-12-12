import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdapterstatisticsComponent } from './adapterstatistics.component';

describe('AdapterstatisticsComponent', () => {
  let component: AdapterstatisticsComponent;
  let fixture: ComponentFixture<AdapterstatisticsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AdapterstatisticsComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdapterstatisticsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
