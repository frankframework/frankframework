import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurationsShowComponent } from './configurations-show.component';

describe('ConfigurationsShowComponent', () => {
  let component: ConfigurationsShowComponent;
  let fixture: ComponentFixture<ConfigurationsShowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ConfigurationsShowComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfigurationsShowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
