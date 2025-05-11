/**
 * 유틸리티 함수 모음
 */

/**
 * 날짜를 포맷팅하는 함수
 * @param {string} dateString - ISO 형식의 날짜 문자열
 * @returns {string} - 포맷팅된 날짜 (YYYY-MM-DD HH:MM:SS)
 */
function formatDate(dateString) {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

/**
 * 에러 메시지를 표시하는 함수
 * @param {string} elementId - 메시지를 표시할 요소의 ID
 * @param {string} message - 표시할 메시지
 * @param {boolean} isError - 에러 메시지 여부 (기본값: true)
 */
function showMessage(elementId, message, isError = true) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.className = isError ? 'text-danger' : 'text-success';
        
        // 3초 후 메시지 삭제
        setTimeout(() => {
            element.textContent = '';
            element.className = '';
        }, 3000);
    }
}

/**
 * URL 파라미터를 가져오는 함수
 * @param {string} paramName - 가져올 파라미터 이름
 * @returns {string|null} - 파라미터 값 또는 null
 */
function getUrlParam(paramName) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(paramName);
}

/**
 * 페이지 리다이렉트 함수
 * @param {string} url - 리다이렉트할 URL
 */
function redirectTo(url) {
    window.location.href = url;
}

/**
 * 로그인 상태 확인 함수
 * @returns {boolean} - 로그인 여부
 */
function isLoggedIn() {
    // 쿠키 또는 localStorage에 토큰이 있는지 확인
    return !!getCookie('token') || !!localStorage.getItem('token');
}

/**
 * 로그인 체크 및 리다이렉트 함수
 * 로그인되지 않은 상태에서 보호된 페이지 접근 시 로그인 페이지로 리다이렉트
 */
function checkAuth() {
    if (!isLoggedIn()) {
        redirectTo('login.html');
    }
}

/**
 * 사용자 정보 가져오기
 * @returns {Object|null} - 사용자 정보 객체 또는 null
 */
function getUserInfo() {
    const userInfoString = localStorage.getItem('userInfo');
    return userInfoString ? JSON.parse(userInfoString) : null;
}

/**
 * 공지사항 Markdown 내용을 로드하는 함수
 * @param {string} elementId - Markdown 내용을 표시할 요소의 ID (기본값: gistContent)
 */
function loadNoticeContent(elementId = 'gistContent') {
    const contentElement = document.getElementById(elementId);

    if (contentElement) {
        // 로딩 표시
        contentElement.innerHTML = '<p>공지사항을 불러오는 중...</p>';

        // Markdown 파일 경로
        const noticePath = 'assets/notice.md';

        // Fetch 요청
        fetch(noticePath)
            .then(response => {
                if (!response.ok) {
                    throw new Error('네트워크 응답이 올바르지 않습니다.');
                }
                return response.text();
            })
            .then(markdown => {
                // Markdown 내용을 HTML로 변환하여 표시
                contentElement.innerHTML = marked.parse(markdown);
            })
            .catch(error => {
                // 오류 메시지 표시
                contentElement.innerHTML = '<p class="text-danger">공지사항을 불러오는 데 실패했습니다.</p>';
                console.error('Markdown 로드 오류:', error);
            });
    }
}
