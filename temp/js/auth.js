/**
 * 인증 및 토큰 관리 함수
 */

/**
 * 로그인 폼 이벤트 리스너 설정
 */
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }

    // 이미 로그인되어 있는 경우 메인 페이지로 리다이렉트
    if (isLoggedIn() && window.location.pathname.includes('login.html')) {
        redirectTo('index.html');
    }

    // 토큰이 없거나 만료된 경우 로그인 페이지로 리다이렉트
    if (!isLoggedIn() && !window.location.pathname.includes('login.html')) {
        redirectTo('login.html');
    }

    // 로그아웃 버튼 이벤트 리스너 설정
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }

    // 사용자 정보 표시
    displayUserInfo();
});

/**
 * 로그인 처리 함수
 * @param {Event} event - 폼 제출 이벤트
 */
async function handleLogin(event) {
    event.preventDefault();
    
    const studentId = document.getElementById('studentId').value;
    const name = document.getElementById('name').value;
    const password = document.getElementById('password').value;
    const messageElement = document.getElementById('loginMessage');
    
    if (!studentId || !name || !password) {
        showMessage('loginMessage', '모든 필드를 입력해주세요.');
        return;
    }
    
    try {
        // 로그인 버튼 비활성화
        const submitButton = document.querySelector('#loginForm button[type="submit"]');
        submitButton.disabled = true;
        submitButton.innerHTML = '로그인 중...';
        
        const loginData = {
            studentId: studentId,
            name: name,
            passwrd: password
        };
        
        const response = await login(loginData);
        
        // 로그인 성공 (서버 응답 확인)
        if (response === 'Login successful' || (response && response.token)) {
            // 사용자 정보 저장
            const userInfo = {
                studentId: studentId,
                name: name
            };
            
            // 토큰이 응답에 포함된 경우 저장 (쿠키와 함께 백업용으로 저장)
            if (response && response.token) {
                localStorage.setItem('token', response.token);
            }
            
            localStorage.setItem('userInfo', JSON.stringify(userInfo));
            
            // 메인 페이지로 리다이렉트
            redirectTo('index.html');
        } else {
            // 로그인 실패
            showMessage('loginMessage', '로그인에 실패했습니다. 학번과 비밀번호를 확인해주세요.');
        }
    } catch (error) {
        console.error('로그인 오류:', error);
        showMessage('loginMessage', '로그인 중 오류가 발생했습니다. 나중에 다시 시도해주세요.');
    } finally {
        // 로그인 버튼 활성화
        const submitButton = document.querySelector('#loginForm button[type="submit"]');
        submitButton.disabled = false;
        submitButton.innerHTML = '로그인';
    }
}

/**
 * 로그아웃 처리 함수
 */
function handleLogout() {
    // 토큰 및 사용자 정보 삭제 (localStorage)
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    
    // 쿠키에서 토큰 삭제
    document.cookie = 'token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    
    // 로그인 페이지로 리다이렉트
    redirectTo('login.html');
}

/**
 * 로그아웃 함수 (HTML에서 직접 호출용)
 */
function logout() {
    handleLogout();
}

/**
 * 사용자 정보 표시 함수
 */
function displayUserInfo() {
    const userInfoElement = document.getElementById('userInfo') || document.getElementById('studentInfo');
    if (userInfoElement) {
        const userInfo = getUserInfo();
        if (userInfo) {
            userInfoElement.textContent = `학번: ${userInfo.studentId}`;
        }
    }
}

/**
 * 쿠키에서 특정 이름의 값을 가져오는 함수
 * @param {string} name - 쿠키 이름
 * @returns {string|null} - 쿠키 값 또는 null
 */
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}
