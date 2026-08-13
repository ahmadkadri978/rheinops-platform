import http from 'k6/http';
import { check, fail } from 'k6';

const baseUrl = __ENV.RHEINOPS_BASE_URL || 'http://api:8080';
const tokenUrl = __ENV.RHEINOPS_TOKEN_URL
  || 'http://keycloak:8080/realms/rheinops/protocol/openid-connect/token';

export function obtainViewerToken() {
  const response = http.post(tokenUrl, {
    client_id: 'rheinops-cli',
    grant_type: 'password',
    username: __ENV.RHEINOPS_USERNAME || 'viewer',
    password: __ENV.RHEINOPS_PASSWORD || 'viewer-demo',
  }, {
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    tags: {name: 'oidc-token'},
  });

  const accepted = check(response, {
    'OIDC token issued': (result) => result.status === 200,
    'OIDC response contains access token': (result) => Boolean(result.json('access_token')),
  });
  if (!accepted) {
    fail(`Unable to obtain the local viewer token (status ${response.status}).`);
  }
  return response.json('access_token');
}

export function readOperationalQueue(token) {
  const response = http.get(`${baseUrl}/api/v1/support-cases?size=20`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Correlation-ID': `k6-${__VU}-${__ITER}`,
    },
    tags: {name: 'rheinops-read'},
  });

  check(response, {
    'queue read returns 200': (result) => result.status === 200,
    'queue response is JSON': (result) =>
      String(result.headers['Content-Type'] || '').includes('application/json'),
    'correlation ID is returned': (result) => Boolean(result.headers['X-Correlation-Id']),
  });
}
