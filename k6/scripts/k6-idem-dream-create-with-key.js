import http from 'k6/http';
import {check, sleep} from 'k6';
import {CFG} from './config.js';
import {uuidv4} from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  vus: 1,
  duration: '2m',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<600'],
  },
};

export function setup() {
  const res = http.post(
      `${CFG.BASE}/api/auth/tokens`,
      JSON.stringify({email: CFG.EMAIL, password: CFG.PASSWORD}),
      {headers: {'Content-Type': 'application/json'}}
  );
  check(res, {'login 200': r => r.status === 200});
  const token = JSON.parse(res.body).data.accessToken;
  return {token};
}

export default function ({token}) {
  const url = `${CFG.BASE}/api/dreams`;
  const idemKey = uuidv4();

  const payload = JSON.stringify({
    title: 'k6-idem',
    content: 'same',
    dreamDate: '2025-10-01',
    isPublic: true
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'Idempotency-Key': idemKey
    }
  };

  const res1 = http.post(url, payload, params);
  check(res1, {'first 201': r => r.status === 201});

  const res2 = http.post(url, payload, params);
  check(res2, {'second 201': r => r.status === 201});

  sleep(1);
}