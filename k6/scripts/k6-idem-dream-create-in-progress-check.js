import http from 'k6/http';
import {check} from 'k6';
import {Counter} from 'k6/metrics';
import {CFG} from './config.js';

export const options = {
  scenarios: {
    bursts: {
      executor: 'ramping-arrival-rate',
      timeUnit: '1s',
      startRate: 100,
      preAllocatedVUs: 300,
      maxVUs: 1000,
      stages: [
        {target: 300, duration: '40s'},
        {target: 600, duration: '40s'},
        {target: 0, duration: '10s'},
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{phase:burst}': ['p(95)<600'],
    checks: ['rate>0.99'],
  },
};

const created201 = new Counter('created201_count');
const inProgress202 = new Counter('in_progress_202');
const serverErrs = new Counter('server_error');

export function setup() {
  const res = http.post(
      `${CFG.BASE}/api/auth/tokens`,
      JSON.stringify({email: CFG.EMAIL, password: CFG.PASSWORD}),
      {headers: {'Content-Type': 'application/json'}},
  );
  check(res, {'login 200': (r) => r.status === 200});
  const token = JSON.parse(res.body).data.accessToken;

  const keyCount = 1;
  const keys = Array.from({length: keyCount}, () =>
      (globalThis.crypto?.randomUUID?.() ??
          `idem-${Math.random().toString(36).slice(2)}${Date.now().toString(
              36)}`),
  );

  return {token, keys};
}

export default function ({token, keys}) {
  const key = keys[0];
  const shortKey = key.slice(0, 8);

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'Idempotency-Key': key,
  };

  const payload = JSON.stringify({
    title: `race-burst-in-progress-check:${shortKey}`,
    content: 'same',
    dreamDate: '2025-10-01',
    isPublic: true,
  });

  const r = http.post(`${CFG.BASE}/api/dreams`, payload, {
    headers,
    tags: {phase: 'burst'},
  });

  if (r.status === 201) {
    created201.add(1);
  }
  if (r.status === 202) {
    inProgress202.add(1);
  }
  if (r.status >= 500) {
    serverErrs.add(1);
  }

  check(r, {'idempotent-ok': (resp) => [200, 201, 202].includes(resp.status)});
}
