import http from 'k6/http';
import {check, sleep} from 'k6';
import {CFG} from "./config.js";

export const options = {
  vus: 1,
  duration: '2m',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<600'],
  },
};

export function setup() {
  const res = http.post(`${CFG.BASE}/api/auth/tokens`,
      JSON.stringify({email: CFG.EMAIL, password: CFG.PASSWORD}),
      {headers: {'Content-Type': 'application/json'}}
  );
  check(res, {'login 200': r => r.status === 200});
  const token = JSON.parse(res.body).data.accessToken;
  return {token};
}

export default function ({token}) {
  const url = `${CFG.BASE}/api/dreams`;
  const payload = JSON.stringify({
    title: 'k6-dup-test',
    content: 'same-payload',
    dreamDate: '2025-10-01',
    isPublic: true
  });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  };

  const res1 = http.post(url, payload, params);
  check(res1, {'first 201': r => r.status === 201});

  const res2 = http.post(url, payload, params);
  check(res2,
      {'second accepted-ish': r => [200, 201, 409, 500].includes(r.status)});

  sleep(1);
}
