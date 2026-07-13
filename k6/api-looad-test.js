import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate } from 'k6/metrics';

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 400, 401, 403, 404, 409));

const unexpectedStatus = new Rate('unexpected_status');

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const DEFAULT_PASSWORD = __ENV.TEST_PASSWORD || 'Password123!';
const USER_EMAIL = __ENV.TEST_USER_EMAIL || 'user@example.com';
const USER_PASSWORD = __ENV.TEST_USER_PASSWORD || DEFAULT_PASSWORD;
const ACCESS_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzgzMjU2NTQ2LCJleHAiOjE3ODMyNjAxNDYsInJvbGUiOiJVU0VSIn0.3PF3GNncmyy2zViep7-757l8X8V7B7tB9nwQb-VZVsA';
const REFRESH_TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzgzMjU2NTQ2LCJleHAiOjE3ODQ0NjYxNDYsImp0aSI6ImYzMGQ3ZmY4LWY3ODgtNDI4Yy05ODI2LTAxYmQ1MGEyNGZmYiJ9.fipagMtswrqJMjYLgAmhOt5Y28eUWp5hmOT-nlcVgl0';
const ADMIN_ACCESS_TOKEN = __ENV.ADMIN_ACCESS_TOKEN || ACCESS_TOKEN;

const WRITE = envBool('WRITE');
const ADMIN_WRITE = envBool('ADMIN_WRITE');
const ADMIN_INGESTION = envBool('ADMIN_INGESTION');
const EMAIL_FLOW = envBool('EMAIL_FLOW');
const OAUTH_FLOW = envBool('OAUTH_FLOW');
const SIGNUP_USER = envBool('SIGNUP_USER');
const PROFILE_WRITE = envBool('PROFILE_WRITE');

const ACTIVITY_CATEGORIES = [
    'SPORTS',
    'CULTURE',
    'CRAFT',
    'COOKING',
    'PHOTO_VIDEO',
    'HUMANITIES',
    'TRAVEL',
    'LANGUAGE',
    'VOLUNTEER',
    'CAREER',
];
const ACTIVITY_TYPES = ['PROGRAM', 'ONE_DAY', 'EVENT', 'CLUB'];
const SOURCE_TYPES = ['KOPIS', 'EXHIBITION', 'SEOUL_CULTURE', 'SEOUL_RESERVATION', 'DISCOVERY', 'URL_MANUAL'];
const TAG_TYPES = ['TOPIC_CATEGORY', 'PREFERENCE_TAG', 'ACTIVITY_CATEGORY'];
const TAG_GROUP_TYPES = ['MOOD', 'INTENSITY', 'PURPOSE', 'DURATION', 'SIZE'];
const AD_POSITIONS = ['ACTIVITY_LIST', 'MAIN_BANNER'];

export const options = {
    scenarios: {
        mixed_api_load: {
            executor: 'ramping-arrival-rate',
            startRate: Number(__ENV.START_RATE || 1),
            timeUnit: '1s',
            preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 20),
            maxVUs: Number(__ENV.MAX_VUS || 100),
            stages: [
                { target: Number(__ENV.RAMP_RATE || 10), duration: __ENV.RAMP_DURATION || '2m' },
                { target: Number(__ENV.RAMP_RATE || 10), duration: __ENV.STEADY_DURATION || '5m' },
                { target: 0, duration: __ENV.RAMP_DOWN_DURATION || '1m' },
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<800', 'p(99)<1500'],
        unexpected_status: ['rate<0.02'],
    },
};

export function setup() {
    let accessToken = ACCESS_TOKEN;
    let refreshToken = REFRESH_TOKEN;
    let adminAccessToken = ADMIN_ACCESS_TOKEN;

    if (SIGNUP_USER && !USER_EMAIL) {
        const email = `k6-${Date.now()}@example.com`;
        request('POST', '/api/v1/auth/signup', {
            email,
            password: DEFAULT_PASSWORD,
        }, { name: 'POST /api/v1/auth/signup setup' }, [201, 409]);

        const loginRes = request('POST', '/api/v1/auth/login', {
            email,
            password: DEFAULT_PASSWORD,
        }, { name: 'POST /api/v1/auth/login setup' }, [200, 401]);
        const tokens = responseData(loginRes) || {};
        accessToken = accessToken || tokens.accessToken || '';
        refreshToken = refreshToken || tokens.refreshToken || '';
        adminAccessToken = adminAccessToken || accessToken;
    }

    if (!accessToken && USER_EMAIL) {
        const loginRes = request('POST', '/api/v1/auth/login', {
            email: USER_EMAIL,
            password: USER_PASSWORD,
        }, { name: 'POST /api/v1/auth/login setup' }, [200, 401]);
        const tokens = responseData(loginRes) || {};
        accessToken = tokens.accessToken || '';
        refreshToken = refreshToken || tokens.refreshToken || refreshToken;
    }

    const userHeaders = authHeaders(accessToken);
    const adminHeaders = authHeaders(adminAccessToken);
    const seed = loadSeed(userHeaders);

    return {
        accessToken,
        refreshToken,
        adminAccessToken,
        seed,
    };
}

export default function (state) {
    const userHeaders = authHeaders(state.accessToken);
    const adminHeaders = authHeaders(state.adminAccessToken);
    const seed = state.seed || {};
    const roll = Math.random();

    if (roll < 0.14) {
        authApiFlow(state);
    } else if (roll < 0.54) {
        readApiFlow(userHeaders, seed);
    } else if (roll < 0.74) {
        pageAndPersonalizationFlow(userHeaders, seed);
    } else if (roll < 0.86) {
        userMutationFlow(userHeaders, seed);
    } else if (roll < 0.96) {
        adminCatalogFlow(adminHeaders, seed);
    } else {
        adminIngestionFlow(adminHeaders);
    }

    sleep(Number(__ENV.SLEEP_SECONDS || 0.2));
}

function authApiFlow(state) {
    group('auth api', () => {
        if (USER_EMAIL) {
            request('POST', '/api/v1/auth/login', {
                email: USER_EMAIL,
                password: USER_PASSWORD,
            }, { name: 'POST /api/v1/auth/login' }, [200, 401]);
        } else {
            request('POST', '/api/v1/auth/login', {
                email: 'missing@example.com',
                password: DEFAULT_PASSWORD,
            }, { name: 'POST /api/v1/auth/login invalid' }, [401]);
        }

        if (state.refreshToken) {
            request('POST', '/api/v1/auth/reissue', {
                refreshToken: state.refreshToken,
            }, { name: 'POST /api/v1/auth/reissue' }, [200, 401, 409]);
        } else {
            request('POST', '/api/v1/auth/reissue', {
                refreshToken: 'invalid-refresh-token',
            }, { name: 'POST /api/v1/auth/reissue invalid' }, [400, 401]);
        }

        request('POST', '/api/v1/auth/handoff', {
            handoffToken: 'invalid-handoff-token',
        }, { name: 'POST /api/v1/auth/handoff invalid' }, [400, 401, 404]);

        request('POST', '/api/v1/auth/email/verify-code', {
            email: USER_EMAIL || 'missing@example.com',
            code: '000000',
        }, { name: 'POST /api/v1/auth/email/verify-code invalid' }, [400, 404, 409]);

        request('POST', '/api/v1/auth/password-reset/verify-code', {
            email: USER_EMAIL || 'missing@example.com',
            code: '000000',
        }, { name: 'POST /api/v1/auth/password-reset/verify-code invalid' }, [400, 404, 409]);

        request('POST', '/api/v1/auth/password-reset/reset', {
            resetToken: 'invalid-reset-token',
            newPassword: 'NewPassword123!',
            newPasswordConfirm: 'NewPassword123!',
        }, { name: 'POST /api/v1/auth/password-reset/reset invalid' }, [400, 401, 404]);

        if (EMAIL_FLOW) {
            request('POST', '/api/v1/auth/email/send-code', {
                email: USER_EMAIL || 'k6-email@example.com',
            }, { name: 'POST /api/v1/auth/email/send-code' }, [200, 400, 409]);
            request('POST', '/api/v1/auth/password-reset/send-code', {
                email: USER_EMAIL || 'k6-email@example.com',
            }, { name: 'POST /api/v1/auth/password-reset/send-code' }, [200, 400, 404]);
        }

        if (OAUTH_FLOW) {
            request('GET', '/api/v1/oauth/google/login', null, { name: 'GET /api/v1/oauth/google/login', redirects: 0 }, [302]);
            request('GET', '/api/v1/oauth/kakao/login', null, { name: 'GET /api/v1/oauth/kakao/login', redirects: 0 }, [302]);
            request('GET', '/api/v1/oauth/naver/login', null, { name: 'GET /api/v1/oauth/naver/login', redirects: 0 }, [302]);
            request('GET', '/api/v1/oauth/google/callback?error=access_denied', null, { name: 'GET /api/v1/oauth/google/callback error', redirects: 0 }, [302, 400]);
            request('GET', '/api/v1/oauth/kakao/callback?error=access_denied', null, { name: 'GET /api/v1/oauth/kakao/callback error', redirects: 0 }, [302, 400]);
            request('GET', '/api/v1/oauth/naver/callback?error=access_denied', null, { name: 'GET /api/v1/oauth/naver/callback error', redirects: 0 }, [302, 400]);
        }
    });
}

function readApiFlow(headers, seed) {
    group('read api', () => {
        const regionId = randomId(seed.regionIds, 999999);
        const activityId = randomId(seed.activityIds, 999999);
        const tagId = randomId(seed.tagIds, 999999);
        const advertisementId = randomId(seed.advertisementIds, 999999);

        request('GET', '/api/v1/regions', null, {
            name: 'GET /api/v1/regions',
            headers,
        }, [200, 401]);
        request('GET', `/api/v1/regions?parentId=${regionId}`, null, {
            name: 'GET /api/v1/regions?parentId',
            headers,
        }, [200, 401, 404]);
        request('GET', `/api/v1/regions/${regionId}`, null, {
            name: 'GET /api/v1/regions/{regionId}',
            headers,
        }, [200, 401, 404]);

        request('GET', '/api/v1/tags', null, {
            name: 'GET /api/v1/tags',
            headers,
        }, [200, 401]);
        request('GET', `/api/v1/tags?tagType=${randomItem(TAG_TYPES)}&tagGroupType=${randomItem(TAG_GROUP_TYPES)}`, null, {
            name: 'GET /api/v1/tags filtered',
            headers,
        }, [200, 400, 401]);
        request('GET', `/api/v1/tags/${tagId}`, null, {
            name: 'GET /api/v1/tags/{tagId}',
            headers,
        }, [200, 401, 404]);

        request('GET', '/api/v1/activities', null, {
            name: 'GET /api/v1/activities',
            headers,
        }, [200, 401]);
        request('GET', `/api/v1/activities?regionId=${regionId}&category=${randomItem(ACTIVITY_CATEGORIES)}&type=${randomItem(ACTIVITY_TYPES)}&keyword=${encodeURIComponent('test')}`, null, {
            name: 'GET /api/v1/activities filtered',
            headers,
        }, [200, 400, 401, 404]);
        request('GET', `/api/v1/activities/search?tagIds=${tagId}`, null, {
            name: 'GET /api/v1/activities/search',
            headers,
        }, [200, 400, 401]);
        request('GET', '/api/v1/activities/recommendations', null, {
            name: 'GET /api/v1/activities/recommendations',
            headers,
        }, [200, 401]);
        request('GET', `/api/v1/activities/${activityId}`, null, {
            name: 'GET /api/v1/activities/{activityId}',
            headers,
        }, [200, 401, 404]);

        request('GET', `/api/v1/activities/${activityId}/reviews?page=0&size=20&sort=createdAt,desc`, null, {
            name: 'GET /api/v1/activities/{activityId}/reviews',
            headers,
        }, [200, 401, 404]);
        request('GET', `/api/v1/activities/${activityId}/images`, null, {
            name: 'GET /api/v1/activities/{activityId}/images',
            headers,
        }, [200, 401, 404]);

        request('GET', '/api/v1/activity-favorites?sort=saved', null, {
            name: 'GET /api/v1/activity-favorites',
            headers,
        }, [200, 401]);

        request('GET', `/api/v1/advertisements?position=${randomItem(AD_POSITIONS)}`, null, {
            name: 'GET /api/v1/advertisements',
            headers,
        }, [200, 400, 401]);
        request('GET', `/api/v1/advertisements/${advertisementId}`, null, {
            name: 'GET /api/v1/advertisements/{advertisementId}',
            headers,
        }, [200, 401, 404]);
    });
}

function pageAndPersonalizationFlow(headers, seed) {
    group('page and personalization api', () => {
        const activityId = randomId(seed.activityIds, 999999);
        request('GET', '/api/v1/pages/home?limit=10', null, {
            name: 'GET /api/v1/pages/home',
            headers,
        }, [200, 401]);
        request('GET', `/api/v1/pages/category?type=${randomItem(ACTIVITY_TYPES)}&category=${randomItem(ACTIVITY_CATEGORIES)}&sort=recommended&limit=20`, null, {
            name: 'GET /api/v1/pages/category',
            headers,
        }, [200, 400, 401]);
        request('GET', '/api/v1/pages/custom?limit=10', null, {
            name: 'GET /api/v1/pages/custom',
            headers,
        }, [200, 401]);
        request('GET', `/api/v1/pages/detail/${activityId}`, null, {
            name: 'GET /api/v1/pages/detail/{activityId}',
            headers,
        }, [200, 401, 404]);

        request('GET', '/api/v1/users/me/onboarding', null, {
            name: 'GET /api/v1/users/me/onboarding',
            headers,
        }, [200, 401, 404]);
        request('GET', '/api/v1/users/me/onboarding/preference-tags', null, {
            name: 'GET /api/v1/users/me/onboarding/preference-tags',
            headers,
        }, [200, 401]);

        request('GET', '/api/v1/personalization/users/me/best-type', null, {
            name: 'GET /api/v1/personalization/users/me/best-type',
            headers,
        }, [200, 401, 404]);
        request('GET', '/api/v1/personalization/users/me/personalization-activities', null, {
            name: 'GET /api/v1/personalization/users/me/personalization-activities',
            headers,
        }, [200, 401]);
        request('GET', '/api/v1/personlization/users/me/best-type', null, {
            name: 'GET /api/v1/personlization/users/me/best-type legacy',
            headers,
        }, [200, 401, 404]);
        request('GET', '/api/v1/personlization/users/me/personlization-activities', null, {
            name: 'GET /api/v1/personlization/users/me/personlization-activities legacy',
            headers,
        }, [200, 401]);
    });
}

function userMutationFlow(headers, seed) {
    group('user mutation api', () => {
        if (!WRITE) {
            request('POST', '/api/v1/activity-favorites', {
                activityId: randomId(seed.activityIds, 999999),
            }, { name: 'POST /api/v1/activity-favorites auth probe', headers }, [401, 403, 404, 409]);
            return;
        }

        const activityId = randomId(seed.activityIds, 999999);
        request('POST', '/api/v1/activity-favorites', {
            activityId,
        }, { name: 'POST /api/v1/activity-favorites', headers }, [201, 400, 401, 404, 409]);
        request('DELETE', `/api/v1/activity-favorites/${activityId}`, null, {
            name: 'DELETE /api/v1/activity-favorites/{activityId}',
            headers,
        }, [204, 401, 404]);

        const reviewRes = request('POST', `/api/v1/activities/${activityId}/reviews`, {
            rating: randomInt(1, 5),
            reason: `k6 review ${__VU}-${__ITER}`,
        }, { name: 'POST /api/v1/activities/{activityId}/reviews', headers }, [201, 400, 401, 404, 409]);
        const reviewId = idFromResponse(reviewRes) || randomId(seed.reviewIds, 999999);
        request('PUT', `/api/v1/activities/${activityId}/reviews/${reviewId}`, {
            rating: randomInt(1, 5),
            reason: `k6 review updated ${__VU}-${__ITER}`,
        }, { name: 'PUT /api/v1/activities/{activityId}/reviews/{reviewId}', headers }, [200, 400, 401, 403, 404]);
        request('DELETE', `/api/v1/activities/${activityId}/reviews/${reviewId}`, null, {
            name: 'DELETE /api/v1/activities/{activityId}/reviews/{reviewId}',
            headers,
        }, [204, 401, 403, 404]);

        request('POST', '/api/v1/users/me/onboarding', {
            topicTagIds: [randomId(seed.tagIds, 1)],
            regionIds: [randomId(seed.regionIds, 1)],
            preferenceTagIds: [randomId(seed.tagIds, 1)],
        }, { name: 'POST /api/v1/users/me/onboarding', headers }, [200, 400, 401, 404]);

        if (PROFILE_WRITE) {
            request('POST', '/api/v1/users/me/profile', {
                nickname: `k6${randomInt(1000, 9999)}`,
                birthDate: '2001-03-15',
                gender: 'NONE',
                status: 'ETC',
                serviceTermsAgreed: true,
                privacyPolicyAgreed: true,
                marketingAgreed: false,
            }, { name: 'POST /api/v1/users/me/profile', headers }, [201, 400, 401, 409]);
        }
    });
}

function adminCatalogFlow(headers, seed) {
    group('admin catalog api', () => {
        if (!ADMIN_WRITE) {
            request('POST', '/api/v1/tags', {
                name: `k6-probe-${__VU}`,
                type: 'PREFERENCE_TAG',
                tagGroupType: 'MOOD',
            }, { name: 'POST /api/v1/tags admin auth probe', headers }, [401, 403]);
            return;
        }

        const suffix = `${Date.now()}-${__VU}-${__ITER}`;
        const regionRes = request('POST', '/api/v1/regions', {
            parentId: null,
            name: `k6-province-${suffix}`,
            depth: 'PROVINCE',
        }, { name: 'POST /api/v1/regions', headers }, [201, 400, 401, 403, 409]);
        const regionId = idFromResponse(regionRes) || randomId(seed.regionIds, 1);

        request('PUT', `/api/v1/regions/${regionId}`, {
            parentId: null,
            name: `k6-province-updated-${suffix}`,
            depth: 'PROVINCE',
        }, { name: 'PUT /api/v1/regions/{regionId}', headers }, [200, 400, 401, 403, 404, 409]);

        const tagRes = request('POST', '/api/v1/tags', {
            name: `k6-tag-${suffix}`,
            type: 'PREFERENCE_TAG',
            tagGroupType: 'MOOD',
        }, { name: 'POST /api/v1/tags', headers }, [201, 400, 401, 403, 409]);
        const tagId = idFromResponse(tagRes) || randomId(seed.tagIds, 999999);
        request('PUT', `/api/v1/tags/${tagId}`, {
            name: `k6-tag-updated-${suffix}`,
            type: 'PREFERENCE_TAG',
            tagGroupType: 'MOOD',
        }, { name: 'PUT /api/v1/tags/{tagId}', headers }, [200, 400, 401, 403, 404, 409]);

        const activityRes = request('POST', '/api/v1/activities', activityPayload(regionId, suffix), {
            name: 'POST /api/v1/activities',
            headers,
        }, [201, 400, 401, 403, 404, 409]);
        const activityId = idFromResponse(activityRes) || randomId(seed.activityIds, 999999);
        request('PUT', `/api/v1/activities/${activityId}`, activityPayload(regionId, `updated-${suffix}`), {
            name: 'PUT /api/v1/activities/{activityId}',
            headers,
        }, [200, 400, 401, 403, 404, 409]);

        const imageRes = request('POST', `/api/v1/activities/${activityId}/images`, {
            imageUrl: 'https://example.com/k6-image.jpg',
            sortOrder: 0,
            isThumbnail: true,
        }, { name: 'POST /api/v1/activities/{activityId}/images', headers }, [201, 400, 401, 403, 404]);
        const imageId = idFromResponse(imageRes) || 999999;
        request('PUT', `/api/v1/activities/${activityId}/images/${imageId}`, {
            imageUrl: 'https://example.com/k6-image-updated.jpg',
            sortOrder: 1,
            isThumbnail: false,
        }, { name: 'PUT /api/v1/activities/{activityId}/images/{imageId}', headers }, [200, 400, 401, 403, 404]);

        const adRes = request('POST', '/api/v1/advertisements', {
            title: `k6-ad-${suffix}`,
            imageUrl: 'https://example.com/k6-ad.jpg',
            redirectUrl: 'https://example.com',
            position: randomItem(AD_POSITIONS),
            sortOrder: 0,
            startAt: futureIso(1),
            endAt: futureIso(30),
            isActive: true,
        }, { name: 'POST /api/v1/advertisements', headers }, [201, 400, 401, 403, 409]);
        const adId = idFromResponse(adRes) || randomId(seed.advertisementIds, 999999);
        request('PUT', `/api/v1/advertisements/${adId}`, {
            title: `k6-ad-updated-${suffix}`,
            imageUrl: 'https://example.com/k6-ad-updated.jpg',
            redirectUrl: 'https://example.com/updated',
            position: randomItem(AD_POSITIONS),
            sortOrder: 1,
            startAt: futureIso(1),
            endAt: futureIso(30),
            isActive: true,
        }, { name: 'PUT /api/v1/advertisements/{advertisementId}', headers }, [200, 400, 401, 403, 404, 409]);

        request('DELETE', `/api/v1/advertisements/${adId}`, null, {
            name: 'DELETE /api/v1/advertisements/{advertisementId}',
            headers,
        }, [204, 401, 403, 404]);
        request('DELETE', `/api/v1/activities/${activityId}/images/${imageId}`, null, {
            name: 'DELETE /api/v1/activities/{activityId}/images/{imageId}',
            headers,
        }, [204, 401, 403, 404]);
        request('DELETE', `/api/v1/activities/${activityId}`, null, {
            name: 'DELETE /api/v1/activities/{activityId}',
            headers,
        }, [204, 401, 403, 404]);
        request('DELETE', `/api/v1/tags/${tagId}`, null, {
            name: 'DELETE /api/v1/tags/{tagId}',
            headers,
        }, [204, 401, 403, 404, 409]);
        request('DELETE', `/api/v1/regions/${regionId}`, null, {
            name: 'DELETE /api/v1/regions/{regionId}',
            headers,
        }, [204, 401, 403, 404, 409]);
    });
}

function adminIngestionFlow(headers) {
    group('admin ingestion api', () => {
        if (!ADMIN_INGESTION) {
            request('POST', '/api/admin/activities/sources/public/sync', null, {
                name: 'POST /api/admin/activities/sources/public/sync auth probe',
                headers,
            }, [401, 403]);
            return;
        }

        request('POST', '/api/admin/activities/sources/public/sync', null, {
            name: 'POST /api/admin/activities/sources/public/sync',
            headers,
        }, [200, 401, 403]);
        request('POST', `/api/admin/activities/sources/public/${randomItem(SOURCE_TYPES)}/sync?maxPages=1`, null, {
            name: 'POST /api/admin/activities/sources/public/{sourceType}/sync',
            headers,
        }, [200, 400, 401, 403]);
        request('POST', '/api/admin/activities/sources/discovery/collect?keywordLimit=1&resultLimit=1', null, {
            name: 'POST /api/admin/activities/sources/discovery/collect',
            headers,
        }, [200, 400, 401, 403]);
        request('POST', '/api/admin/activities/sources/discovery/publish', null, {
            name: 'POST /api/admin/activities/sources/discovery/publish',
            headers,
        }, [200, 401, 403]);
    });
}

function loadSeed(headers) {
    const regions = responseData(request('GET', '/api/v1/regions', null, {
        name: 'GET /api/v1/regions seed',
        headers,
    }, [200, 401]));
    const tags = responseData(request('GET', '/api/v1/tags', null, {
        name: 'GET /api/v1/tags seed',
        headers,
    }, [200, 401]));
    const activities = responseData(request('GET', '/api/v1/activities', null, {
        name: 'GET /api/v1/activities seed',
        headers,
    }, [200, 401]));
    const advertisements = responseData(request('GET', '/api/v1/advertisements', null, {
        name: 'GET /api/v1/advertisements seed',
        headers,
    }, [200, 401]));

    return {
        regionIds: idsFrom(regions),
        tagIds: idsFrom(tags),
        activityIds: idsFrom(activities),
        advertisementIds: idsFrom(advertisements),
        reviewIds: [],
    };
}

function request(method, path, body, params, expectedStatuses) {
    const url = `${BASE_URL}${path}`;
    const requestParams = params || {};

    requestParams.headers = Object.assign(
        {
            'Content-Type': 'application/json',
        },
        requestParams.headers || {}
    );

    const payload =
        body === null || body === undefined
            ? null
            : JSON.stringify(body);

    const start = Date.now();

    const res = http.request(method, url, payload, requestParams);

    const elapsed = Date.now() - start;

    const ok = expectedStatuses.includes(res.status);

    unexpectedStatus.add(!ok, {
        endpoint: requestParams.name || `${method} ${path}`,
    });

    if (!ok) {
        console.error("");
        console.error("======================================================");
        console.error("❌ REQUEST FAILED");
        console.error("------------------------------------------------------");
        console.error(`Name      : ${requestParams.name || `${method} ${path}`}`);
        console.error(`Method    : ${method}`);
        console.error(`URL       : ${url}`);
        console.error(`Status    : ${res.status}`);
        console.error(`Expected  : ${expectedStatuses.join(", ")}`);
        console.error(`Duration  : ${elapsed} ms`);

        if (res.error) {
            console.error(`Network Error : ${res.error}`);
        }

        if (body !== null && body !== undefined) {
            console.error("");
            console.error("[Request Body]");
            console.error(JSON.stringify(body, null, 2));
        }

        console.error("");
        console.error("[Response Body]");

        try {
            console.error(JSON.stringify(res.json(), null, 2));
        } catch (e) {
            console.error(res.body);
        }

        console.error("======================================================");
        console.error("");
    }

    if (res.status >= 500) {
        console.error("");
        console.error("################ SERVER ERROR ################");
        console.error(`${method} ${url}`);
        console.error(`Status : ${res.status}`);
        console.error(`Time   : ${elapsed} ms`);

        try {
            console.error(JSON.stringify(res.json(), null, 2));
        } catch (e) {
            console.error(res.body);
        }

        console.error("##############################################");
        console.error("");
    }

    if (elapsed > 1000) {
        console.warn(
            `[SLOW REQUEST] ${requestParams.name || `${method} ${path}`} (${elapsed} ms)`
        );
    }

    check(res, {
        [`${requestParams.name || `${method} ${path}`} expected status`]: () => ok,
    });

    return res;
}

function responseData(res) {
    if (!res || !res.body) {
        return null;
    }
    try {
        const json = res.json();
        return json && Object.prototype.hasOwnProperty.call(json, 'data') ? json.data : json;
    } catch (e) {
        return null;
    }
}

function idsFrom(data) {
    if (!data) {
        return [];
    }
    if (Array.isArray(data)) {
        return data.map(idFromObject).filter(Boolean);
    }
    if (Array.isArray(data.content)) {
        return data.content.map(idFromObject).filter(Boolean);
    }
    if (Array.isArray(data.items)) {
        return data.items.map(idFromObject).filter(Boolean);
    }
    return [];
}

function idFromResponse(res) {
    return idFromObject(responseData(res));
}

function idFromObject(item) {
    if (!item || typeof item !== 'object') {
        return null;
    }
    return item.id || item.activityId || item.regionId || item.tagId || item.advertisementId || item.imageId || item.reviewId || null;
}

function authHeaders(token) {
    return token ? { Authorization: `Bearer ${token}` } : {};
}

function envBool(name) {
    return String(__ENV[name] || '').toLowerCase() === 'true';
}

function randomItem(items) {
    return items[Math.floor(Math.random() * items.length)];
}

function randomId(ids, fallback) {
    if (ids && ids.length > 0) {
        return randomItem(ids);
    }
    return fallback;
}

function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function futureIso(days) {
    const date = new Date(Date.now() + days * 24 * 60 * 60 * 1000);
    return date.toISOString().replace(/\.\d{3}Z$/, '');
}

function activityPayload(regionId, suffix) {
    return {
        regionId,
        title: `k6-activity-${suffix}`,
        description: `k6 load test activity ${suffix}`,
        thumbnailUrl: 'https://example.com/k6-thumb.jpg',
        sourceUrl: `https://example.com/k6-activity-${suffix}`,
        address: 'Seoul',
        organizer: 'k6',
        contactInfo: 'k6@example.com',
        target: 'test',
        startAt: futureIso(7),
        endAt: futureIso(14),
        recruitStartAt: futureIso(1),
        recruitEndAt: futureIso(6),
        price: 0,
        activityType: randomItem(ACTIVITY_TYPES),
        category: randomItem(ACTIVITY_CATEGORIES),
        sourceType: 'URL_MANUAL',
        externalId: `k6-${suffix}`,
        approvalStatus: 'APPROVED',
        isActive: true,
    };
}
