import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    courier_updates: {
      executor: 'constant-vus',
      vus: 100, // Simulating 100 concurrent couriers (can be scaled up)
      duration: '1m',
    },
    order_creation: {
      executor: 'per-vu-iterations',
      vus: 10,
      iterations: 5,
      startTime: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // Scenario 1: Courier Location Update
  const courierId = `kurye-load-${__VU}`;
  const locationPayload = JSON.stringify({
    courierId: courierId,
    latitude: 41.0082 + (Math.random() - 0.5) * 0.1,
    longitude: 28.9784 + (Math.random() - 0.5) * 0.1,
    timestamp: Date.now(),
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const resLocation = http.post(`${BASE_URL}/courier/location`, locationPayload, params);
  check(resLocation, {
    'location update status is 200': (r) => r.status === 200,
  });

  // Scenario 2: Random Order Creation (less frequent)
  if (__ITER % 10 === 0) {
    const orderPayload = JSON.stringify({
      customerId: `customer-load-${__VU}`,
      latitude: 41.0082 + (Math.random() - 0.5) * 0.05,
      longitude: 28.9784 + (Math.random() - 0.5) * 0.05,
    });

    const resOrder = http.post(`${BASE_URL}/matching/order`, orderPayload, params);
    check(resOrder, {
      'order creation status is 200': (r) => r.status === 200,
      'order matched': (r) => JSON.parse(r.body).status === 'MATCHED',
    });
  }

  sleep(3); // Wait 3 seconds before next update (standard GPS interval)
}
