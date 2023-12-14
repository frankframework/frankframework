import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IframeLarvaComponent } from './iframe-larva.component';

describe('IframeLarvaComponent', () => {
  let component: IframeLarvaComponent;
  let fixture: ComponentFixture<IframeLarvaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ IframeLarvaComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IframeLarvaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
