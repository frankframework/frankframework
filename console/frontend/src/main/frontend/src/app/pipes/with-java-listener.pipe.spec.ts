import { Adapter } from '../app.service';
import { WithJavaListenerPipe } from './with-java-listener.pipe';

describe('WithJavaListenerPipe', () => {
  let pipe: WithJavaListenerPipe;

  beforeEach(() => {
    pipe = new WithJavaListenerPipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('returns an empty array if empty records is given', () => {
    expect(pipe.transform({})).toEqual([]);
  });

  it('returns an array of adapters that has a java listener', () => {
    const inputAdapters: Record<string, Adapter> = {
      test1: {
        configuration: 'test',
        description: 'test',
        configured: true,
        upSince: 0,
        name: 'test1',
        started: true,
        state: 'started',
      },
      test2: {
        configuration: 'test',
        description: 'test',
        configured: true,
        upSince: 0,
        name: 'test2',
        started: true,
        state: 'started',
        receivers: [
          {
            name: 'JavaListener',
            messages: {
              received: 0,
              rejected: 0,
              retried: 0,
            },
            isEsbJmsFFListener: false,
            listener: {
              class: 'JavaListener',
              name: 'JavaListener',
              destination: 'test',
              isRestListener: false,
            },
            state: 'started',
            transactionalStores: {
              ERROR: {
                name: '',
                numberOfMessages: 0,
              },
              DONE: {
                name: '',
                numberOfMessages: 0,
              },
            },
          },
        ],
      },
      test3: {
        configuration: 'test',
        description: 'test',
        configured: true,
        upSince: 0,
        name: 'test3',
        started: true,
        state: 'started',
        receivers: [
          {
            name: 'OtherListener',
            messages: {
              received: 0,
              rejected: 0,
              retried: 0,
            },
            isEsbJmsFFListener: false,
            listener: {
              class: 'OtherListener',
              name: 'OtherListener',
              destination: 'test',
              isRestListener: false,
            },
            state: 'started',
            transactionalStores: {
              ERROR: {
                name: '',
                numberOfMessages: 0,
              },
              DONE: {
                name: '',
                numberOfMessages: 0,
              },
            },
          },
        ],
      },
    };
    const result = pipe.transform(inputAdapters);
    expect(result.length).toBe(1);
    expect(result[0].receivers![0].listener.class).toBe('JavaListener');
  });
});
