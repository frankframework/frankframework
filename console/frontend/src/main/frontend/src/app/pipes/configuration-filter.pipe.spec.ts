import { Adapter } from '../app.service';
import { ConfigurationFilterPipe } from './configuration-filter.pipe';

describe('ConfigurationFilterPipe', () => {
  let pipe: ConfigurationFilterPipe;
  const inputAdapters: Record<string, Adapter> = {
    test1: {
      configuration: 'config1',
      description: 'test',
      configured: true,
      upSince: 0,
      name: 'test1',
      started: true,
      state: 'started',
    },
    test2: {
      configuration: 'config2',
      description: 'test',
      configured: true,
      upSince: 0,
      name: 'test2',
      started: true,
      state: 'started',
    },
    test3: {
      configuration: 'config2',
      description: 'test',
      configured: true,
      upSince: 0,
      name: 'test3',
      started: true,
      state: 'started',
    },
  };

  beforeEach(() => {
    pipe = new ConfigurationFilterPipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('returns an empty object if empty object is given', () => {
    expect(pipe.transform({}, 'test')).toEqual({});
  });

  it('returns adapters filtered by configuration', () => {
    const result = Object.values(pipe.transform(inputAdapters, 'config2'));
    expect(result.length).toBe(2);
    expect(result.every((adapter) => adapter.configuration == 'config2')).toBe(true);
  });

  it('returns an emtpy object if configuration is not found', () => {
    const result = pipe.transform(inputAdapters, 'config3');
    expect(result).toBeTruthy();
    expect(Object.values(result).length).toBe(0);
  });
});
