import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.DEMO_PASSWORD || 'Test@1234';
const ACCOUNTS = ['req1', 'req2', 'req3', 'req4', 'req5'].map((name) => `${name}@civicconnect.test`);
const QUICK = __ENV.QUICK === 'true';

export const options = {
  stages: QUICK
    ? [
        { duration: '10s', target: 5 },
        { duration: '20s', target: 5 },
        { duration: '5s', target: 0 }
      ]
    : [
        { duration: '1m', target: 50 },
        { duration: '3m', target: 50 },
        { duration: '30s', target: 0 }
      ],
  thresholds: {
    'http_req_duration{name:submit}': ['p(95)<3000'],
    'http_req_failed{name:submit}': ['rate<0.01'],
    checks: ['rate>0.99']
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'max']
};

const json = { 'Content-Type': 'application/json' };

function login(email) {
  const response = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ email, password: PASSWORD }), {
    headers: json,
    tags: { name: 'login' }
  });
  if (response.status !== 200) {
    throw new Error(`Login failed for ${email}: ${response.status} ${response.body}`);
  }
  return response.json('token');
}

export function setup() {
  const tokens = ACCOUNTS.map(login);
  const categories = http.get(`${BASE_URL}/api/categories`, {
    headers: { Authorization: `Bearer ${tokens[0]}` },
    tags: { name: 'categories' }
  });
  const ids = categories.json().map((category) => category.id);
  if (ids.length === 0) {
    throw new Error('No categories were returned');
  }
  http.post(
    `${BASE_URL}/api/requests`,
    JSON.stringify({ categoryId: ids[0], description: 'Warm-up request before the measured run', priority: 'MEDIUM' }),
    { headers: { ...json, Authorization: `Bearer ${tokens[0]}` }, tags: { name: 'warmup' } }
  );
  return { tokens, ids };
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];
  const categoryId = data.ids[(__VU + __ITER) % data.ids.length];
  const body = JSON.stringify({
    categoryId,
    description: `Load test request from virtual user ${__VU}, iteration ${__ITER}`,
    priority: 'MEDIUM'
  });
  const response = http.post(`${BASE_URL}/api/requests`, body, {
    headers: { ...json, Authorization: `Bearer ${token}` },
    tags: { name: 'submit' }
  });
  check(response, {
    'submission returns 201': (r) => r.status === 201,
    'reference number is returned': (r) => /CC-\d{8}/.test(r.body)
  });
  sleep(1);
}
