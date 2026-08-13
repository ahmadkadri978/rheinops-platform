import { sleep } from 'k6';
import { obtainViewerToken, readOperationalQueue } from './lib/rheinops.js';

export const options = {
  scenarios: {
    sudden_read_spike: {
      executor: 'ramping-vus',
      startVUs: 2,
      stages: [
        {duration: '30s', target: 5},
        {duration: '20s', target: 50},
        {duration: '40s', target: 50},
        {duration: '30s', target: 5},
      ],
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    'http_req_failed{name:rheinops-read}': ['rate<0.05'],
    'http_req_duration{name:rheinops-read}': ['p(95)<1000', 'p(99)<2000'],
    checks: ['rate>0.95'],
  },
};

export function setup() {
  return {token: obtainViewerToken()};
}

export default function (data) {
  readOperationalQueue(data.token);
  sleep(0.2);
}
