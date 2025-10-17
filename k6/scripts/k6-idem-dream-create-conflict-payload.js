import http from 'k6/http';
import {check, sleep} from 'k6';
import {CFG} from './config.js';
import {uuidv4} from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

http.setResponseCallback(http.expectedStatuses(
    {min: 200, max: 204},
    202,
    409
));

export const options = {
  vus: 1,
  duration: '1m',
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

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'Idempotency-Key': idemKey
    }
  };

  const p1 = JSON.stringify({
    title: 'k6-idem-conflict',
    content: 'same',
    dreamDate: '2025-10-01',
    isPublic: true
  });
  const r1 = http.post(url, p1, params);
  check(r1, {'first 201/200': r => r.status === 201 || r.status === 200});

  const p2 = JSON.stringify({
    title: 'k6-idem-conflict',
    content: 'DIFFERENT',
    dreamDate: '2025-10-01',
    isPublic: true
  });
  const r2 = http.post(url, p2, params);
  check(r2, {
    'second conflict on diff payload': r => [409, 400, 422].includes(r.status)
  });

  sleep(1);
}
