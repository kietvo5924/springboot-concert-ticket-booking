import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // Flash Sale load testing strategy
  stages: [
    { duration: '10s', target: 500 },   // Warm-up phase
    { duration: '30s', target: 3000 },  // Spike phase (high traffic)
    { duration: '10s', target: 0 },     // Cool-down phase
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],   // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.01'],     // Error rate must be less than 1%
  },
};

export default function () {
  const url = 'http://host.docker.internal:8080/api/v1/bookings';

  // Mock booking payload
  const payload = JSON.stringify({
    concertId: 1,
    ticketCategoryId: 1,
    quantity: 1
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      // Random User ID (1-50000) to bypass Rate Limiting
      'X-User-Id': Math.floor(Math.random() * 50000) + 1,
    },
  };

  // Execute POST request
  const res = http.post(url, payload, params);

  // Validate async response (Kafka)
  check(res, {
    'status is 202 Accepted': (r) => r.status === 202,
  });

  sleep(1); // Simulate real user delay
}
