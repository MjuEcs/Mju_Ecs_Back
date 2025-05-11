/**
 * API 요청 관련 함수
 */

/**
 * 기본 API 엔드포인트
 */
const API_BASE_URL = 'http://localhost:8080'; // 서버 URL을 여기에 설정 (비어있으면 현재 호스트 사용)

/**
 * API 요청을 보내는 기본 함수
 * @param {string} endpoint - API 엔드포인트 경로
 * @param {string} method - HTTP 메서드 (GET, POST, DELETE 등)
 * @param {Object|null} data - 요청 데이터 (선택적)
 * @returns {Promise} - API 응답 Promise
 */
async function apiRequest(endpoint, method = 'GET', data = null) {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const headers = {
        'Content-Type': 'application/json'
    };
    
    // 쿠키에 토큰이 없는 경우, localStorage에서 토큰을 가져와 헤더에 추가
    // 이는 서버가 쿠키를 사용하지만 클라이언트에서 백업으로 localStorage에도 저장하기 때문
    const token = localStorage.getItem('token');
    if (token && !getCookie('token')) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    
    const options = {
        method,
        headers,
        credentials: 'include' // 항상 쿠키 포함 (중요)
    };
    
    // POST, PUT 등의 요청인 경우 body 추가
    if (data && method !== 'GET') {
        options.body = JSON.stringify(data);
    }
    
    try {
        const response = await fetch(url, options);
        
        // 서버가 Set-Cookie 응답 헤더 보낸 경우는 자동으로 브라우저가 처리함
        
        // 토큰 만료 등의 인증 오류인 경우
        if (response.status === 401) {
            // 토큰 삭제 및 로그인 페이지로 리다이렉트
            localStorage.removeItem('token');
            localStorage.removeItem('userInfo');
            document.cookie = 'token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
            window.location.href = 'login.html';
            return null;
        }
        
        // 응답이 JSON인 경우 파싱
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        }
        
        // 텍스트 응답인 경우
        return await response.text();
    } catch (error) {
        console.error('API 요청 오류:', error);
        throw error;
    }
}

/**
 * 로그인 API 요청
 * @param {Object} loginData - 로그인 데이터 (studentId, name, password)
 * @returns {Promise} - 로그인 응답 Promise
 */
async function login(loginData) {
    console.log('로그인 데이터:', loginData);
    return apiRequest('/auth/login', 'POST', loginData);
}

/**
 * 컨테이너 목록 조회 API
 * @returns {Promise} - 컨테이너 목록 응답 Promise
 */
async function getContainers() {
    return apiRequest('/docker/status');
}

/**
 * 특정 컨테이너 정보 조회 API
 * @param {string} containerId - 컨테이너 ID
 * @returns {Promise} - 컨테이너 정보 응답 Promise
 */
async function getContainerInfo(containerId) {
    return apiRequest(`/docker/status?containerId=${containerId}`);
}

/**
 * 컨테이너 생성 API
 * @param {Object} containerData - 컨테이너 생성 데이터
 * @returns {Promise} - 컨테이너 생성 응답 Promise
 */
async function createContainer(containerData) {
    return apiRequest('/docker/custom/run', 'POST', containerData);
}

/**
 * 컨테이너 시작 API
 * @param {string} containerId - 컨테이너 ID
 * @returns {Promise} - 컨테이너 시작 응답 Promise
 */
async function startContainer(containerId) {
    return apiRequest(`/docker/start?containerId=${containerId}`, 'POST');
}

/**
 * 컨테이너 정지 API
 * @param {string} containerId - 컨테이너 ID
 * @returns {Promise} - 컨테이너 정지 응답 Promise
 */
async function stopContainer(containerId) {
    return apiRequest(`/docker/stop?containerId=${containerId}`, 'POST');
}

/**
 * 컨테이너 재시작 API
 * @param {string} containerId - 컨테이너 ID
 * @returns {Promise} - 컨테이너 재시작 응답 Promise
 */
async function restartContainer(containerId) {
    return apiRequest(`/docker/restart?containerId=${containerId}`, 'POST');
}

/**
 * 컨테이너 삭제 API
 * @param {string} containerId - 컨테이너 ID
 * @returns {Promise} - 컨테이너 삭제 응답 Promise
 */
async function removeContainer(containerId) {
    return apiRequest(`/docker/remove?containerId=${containerId}`, 'DELETE');
}

/**
 * 컨테이너 생성 API 호출 함수
 * @param {string} imageName - 컨테이너 이미지 이름
 * @param {number} containerPort - 컨테이너 포트
 * @param {Object} env - 환경 변수 객체
 * @param {Array} cmd - 실행 명령어 배열
 * @returns {Promise<string>} - 생성된 컨테이너 ID를 포함한 응답 문자열
 */
async function runContainer(imageName, containerPort, env = {}, cmd = []) {
    try {
        const response = await fetch('/docker/custom/run', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                imageName,
                containerPort,
                env,
                cmd,
            }),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.text();
    } catch (error) {
        console.error('컨테이너 생성 API 호출 오류:', error);
        throw error;
    }
}
