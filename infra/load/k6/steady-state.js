import { sleep } from 'k6';
import { obtainViewerToken, readOperationalQueue } from './lib/rheinops.js';

export const options = {
  scenarios: {
    steady_read_traffic: {
      executor: 'constant-arrival-rate',
      rate: 10,
      timeUnit: '1s',
      duration: '3m',
      preAllocatedVUs: 10,
      maxVUs: 40,
    },
  },
  thresholds: {
    'http_req_failed{name:rheinops-read}': ['rate<0.01'],
    'http_req_duration{name:rheinops-read}': ['p(95)<500', 'p(99)<1000'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  return {token: obtainViewerToken()};
}

export default function (data) {
  readOperationalQueue(data.token);
  sleep(0.05);
}
