/**
 * 컨테이너 상세 정보 페이지 관련 함수
 */

document.addEventListener('DOMContentLoaded', function() {
    // 로그인 상태 확인
    checkAuth();
    
    // URL에서 컨테이너 ID 가져오기
    const containerId = getUrlParam('id');
    
    if (!containerId) {
        // ID가 없으면 메인 페이지로 리다이렉트
        redirectTo('index.html');
        return;
    }
    
    // 컨테이너 정보 로드
    loadContainerDetails(containerId);
    
    // 이벤트 리스너 설정
    document.getElementById('logoutBtn').addEventListener('click', logout);

    const actions = [
        { id: 'startContainer', action: startContainer, label: '<i class="bi bi-play-fill"></i> 시작' },
        { id: 'stopContainer', action: stopContainer, label: '<i class="bi bi-stop-fill"></i> 정지' },
        { id: 'restartContainer', action: restartContainer, label: '<i class="bi bi-arrow-clockwise"></i> 재시작' },
        { id: 'removeContainer', action: confirmAndRemoveContainer, label: '<i class="bi bi-trash"></i> 삭제' },
        { id: 'downloadFiles', action: downloadContainerFiles, label: '<i class="bi bi-download"></i> 다운로드' }
    ];

    actions.forEach(({ id, action, label }) => {
        const button = document.getElementById(id);
        if (button) {
            button.addEventListener('click', async function() {
                button.disabled = true;
                const originalText = button.innerHTML;
                button.innerHTML = `${label} 중...`;
                
                try {
                    await action(containerId);
                    loadContainerDetails(containerId);
                } catch (error) {
                    console.error(`${id} 실행 중 오류 발생:`, error);
                } finally {
                    button.disabled = false;
                    button.innerHTML = label;
                }
            });
        }
    });
});

/**
 * 컨테이너 상세 정보 불러오기 함수
 * @param {string} containerId - 컨테이너 ID
 */
async function loadContainerDetails(containerId) {
    try {
        const containerInfo = (await getContainerInfo(containerId))?.[0];
        
        if (!containerInfo) {
            showMessage('containerMessage', '컨테이너 정보를 찾을 수 없습니다.');
            setTimeout(() => redirectTo('index.html'), 2000);
            return;
        }
        
        console.log('Received container info:', containerInfo);
        console.log('Updating DOM elements with container details.');

        // Ensure all required DOM elements exist before updating them
        const containerImageElement = document.getElementById('containerImage');
        const containerIdElement = document.getElementById('containerId');
        const containerStatusElement = document.getElementById('containerStatus');
        const containerStartedElement = document.getElementById('containerStarted');
        const containerPortsElement = document.getElementById('containerPorts');

        if (!containerImageElement || !containerIdElement || !containerStatusElement || !containerStartedElement || !containerPortsElement) {
            console.error('One or more DOM elements for displaying container details are missing.');
            return;
        }

        // 컨테이너 정보 표시
        containerImageElement.textContent = containerInfo.image || '-';
        containerIdElement.textContent = containerInfo.containerId || '-';
        containerStatusElement.textContent = containerInfo.status;
        containerStatusElement.className = `badge ${getStatusClass(containerInfo.status)}`;
        containerStartedElement.textContent = formatDate(containerInfo.startedAt);
        containerPortsElement.textContent = `${containerInfo.ports.containerPort} (컨테니어 내부 포트) ↔ ${containerInfo.ports.hostPort} (외부 접근 포트)`;
        
        // 웹 터미널 URL 설정 (ttydHostPort 사용)
        const terminalUrl = `http://localhost:${containerInfo.ports.ttydHostPort}`;
        const terminalBtn = document.getElementById('openTerminal');
        if (terminalBtn) {
            terminalBtn.href = terminalUrl;
            terminalBtn.disabled = containerInfo.status.toLowerCase() !== 'running';
        }
        
        // 컨테이너 터미널 URL 표시
        const urlElement = document.getElementById('containerUrl');
        if (urlElement) {
            urlElement.value = terminalUrl;
        }
        
        // 메시지 요소가 있으면 표시
        const messageElement = document.getElementById('containerMessage');
        if (messageElement) {
            messageElement.style.display = 'none';
        }

        // 문서 로드 시도
        loadContainerDocument(containerInfo.image);
        
    } catch (error) {
        console.error('컨테이너 상세 정보 로딩 오류:', error);
        showMessage('containerMessage', '컨테이너 정보를 불러오는데 실패했습니다.');
    }
}

/**
 * 컨테이너 상태에 따른 클래스 반환 함수
 * @param {string} status - 컨테이너 상태
 * @returns {string} - 상태에 해당하는 부트스트랩 클래스
 */
function getStatusClass(status) {
    switch(status.toLowerCase()) {
        case 'running':
            return 'bg-success';
        case 'paused':
        case 'stopped':
            return 'bg-warning';
        case 'exited':
            return 'bg-secondary';
        case 'created':
            return 'bg-info';
        default:
            return 'bg-secondary';
    }
}


/**
 * 컨테이너 삭제 확인 함수
 * @param {string} containerId - 컨테이너 ID
 */
function confirmAndRemoveContainer(containerId) {
    return new Promise((resolve, reject) => {
        if (confirm('정말로 이 컨테이너를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
            removeContainer(containerId)
                .then(() => {
                    showMessage('containerMessage', '컨테이너가 삭제되었습니다.', false);
                    setTimeout(() => redirectTo('index.html'), 2000);
                    resolve();
                })
                .catch(error => {
                    console.error('컨테이너 삭제 오류:', error);
                    showMessage('containerMessage', '컨테이너 삭제에 실패했습니다.');
                    reject(error);
                });
        } else {
            resolve(); // User canceled the confirmation
        }
    });
}

/**
 * URL 복사 함수
 * @param {string} elementId - 복사할 텍스트가 있는 요소의 ID
 */
function copyToClipboard(elementId) {
    const element = document.getElementById(elementId);
    element.select();
    document.execCommand('copy');
    
    // 복사 완료 알림
    const copyBtn = document.getElementById('copyUrlBtn');
    const originalText = copyBtn.innerHTML;
    copyBtn.innerHTML = '<i class="bi bi-check2"></i> 복사됨';
    
    setTimeout(() => {
        copyBtn.innerHTML = originalText;
    }, 2000);
}
