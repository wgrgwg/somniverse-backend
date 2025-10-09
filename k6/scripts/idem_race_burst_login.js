import http from 'k6/http';
import {check} from 'k6';
import {Counter} from 'k6/metrics';
import {CFG} from './config.js';

// 목적: 짧은 시간에 같은 페이로드가 폭발적으로 도착할 때
// 중복/정합성/지연/에러 분포를 관찰 (멱등키 도입 전/후 비교용)
export const options = {
  scenarios: {
    bursts: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 100,
      stages: [
        {target: 50, duration: '30s'},
        {target: 100, duration: '30s'},
        {target: 0, duration: '20s'},
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{phase:burst}': ['p(95)<600'],
  },
};

const dupSuspect = new Counter('dup_suspect');
const conflicts = new Counter('conflict_count');
const serverErrs = new Counter('server_error');

export function setup() {
  const res = http.post(`${CFG.BASE}/api/auth/tokens`,
      JSON.stringify({email: CFG.EMAIL, password: CFG.PASSWORD}),
      {headers: {'Content-Type': 'application/json'}}
  );
  check(res, {'login 200': r => r.status === 200});
  const token = JSON.parse(res.body).data.accessToken;
  return {token};
}

const payload = JSON.stringify({
  title: 'race-burst',
  content: 'same',
  dreamDate: '2025-10-01',
  isPublic: true,
});

export default function ({token}) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  const r = http.post(`${CFG.BASE}/api/dreams`, payload,
      {headers, tags: {phase: 'burst'}});

  // 상태 분포 관찰: 멱등 전/후 비교 시 스토리텔링에 유용
  if ([200, 201].includes(r.status)) {
    dupSuspect.add(1);
  }           // 도입 전엔 중복 의심
  if ([409, 400, 422].includes(r.status)) {
    conflicts.add(1);
  }       // 정책 충돌(정상)
  if (r.status >= 500) {
    serverErrs.add(1);
  }                         // 서버 에러

  // 체크: 허용 가능한 응답군(정책에 맞춰 조정 가능)
  check(r, {'ok-ish': resp => [200, 201, 202, 409].includes(resp.status)});
}
