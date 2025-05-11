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
    document.getElementById('startContainer').addEventListener('click', () => startContainer(containerId));
    document.getElementById('stopContainer').addEventListener('click', () => stopContainer(containerId));
    document.getElementById('restartContainer').addEventListener('click', () => restartContainer(containerId));
    document.getElementById('removeContainer').addEventListener('click', () => confirmAndRemoveContainer(containerId));
});

/**
 * 컨테이너 상세 정보 불러오기 함수
 * @param {string} containerId - 컨테이너 ID
 */
async function loadContainerDetails(containerId) {
    try {
        const containerInfo = await getContainerInfo(containerId);
        
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
        containerImageElement.textContent = containerInfo.image;
        containerIdElement.textContent = containerInfo.containerId;
        containerStatusElement.textContent = containerInfo.status;
        containerStatusElement.className = `badge ${getStatusClass(containerInfo.status)}`;
        containerStartedElement.textContent = formatDate(containerInfo.startedAt);
        containerPortsElement.textContent = `${containerInfo.ports.containerPort} (컨테니어 내부 포트) ↔ ${containerInfo.ports.hostPort} (외부 접근 포트)`;
        
        // 터미널 URL 설정
        const terminalUrl = `http://localhost:${containerInfo.ports.hostPort}`;
        const terminalBtn = document.getElementById('openTerminal');
        if (terminalBtn) {
            terminalBtn.href = terminalUrl;
            terminalBtn.disabled = containerInfo.status.toLowerCase() !== 'running';
        }
        
        // 컨테이너 URL 표시
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
 * 컨테이너 문서 로드 함수
 * @param {string} imageName - 컨테이너 이미지 이름
 */
async function loadContainerDocument(imageName) {
    try {
        // 이미지 이름으로 템플릿 찾기
        const templateName = Object.keys(containerTemplates).find(key => 
            containerTemplates[key].image === imageName || 
            imageName.includes(containerTemplates[key].image.split(':')[0])
        );
        
        const docCard = document.getElementById('documentCard');
        
        // 템플릿을 찾고 docPath가 있는 경우에만 문서 로드
        if (templateName && containerTemplates[templateName].docPath) {
            const docPath = containerTemplates[templateName].docPath;
            
            // 문서 파일 가져오기
            const response = await fetch(docPath);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const markdown = await response.text();
            
            // 마크다운을 HTML로 변환하여 표시
            const documentContent = document.getElementById('documentContent');
            if (documentContent) {
                documentContent.innerHTML = marked.parse(markdown);
                
                // 코드 블록에 스타일 적용
                document.querySelectorAll('pre code').forEach((block) => {
                    block.className = 'p-2 bg-light';
                    block.style.display = 'block';
                    block.style.overflow = 'auto';
                });

                // 마크다운 콘텐츠 내 이미지 크기 제한
                document.querySelectorAll('#documentContent img').forEach((img) => {
                    img.style.maxWidth = '100%';
                    img.style.height = 'auto';
                });
                
                // 문서 카드 표시
                docCard.style.display = 'block';
            }
        } else {
            // 문서가 없는 경우 카드 숨기기
            docCard.style.display = 'none';
        }
    } catch (error) {
        console.error('문서 로드 오류:', error);
        document.getElementById('documentCard').style.display = 'none';
    }
}

/**
 * 컨테이너 삭제 확인 함수
 * @param {string} containerId - 컨테이너 ID
 */
function confirmAndRemoveContainer(containerId) {
    if (confirm('정말로 이 컨테이너를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
        removeContainer(containerId)
            .then(() => {
                showMessage('containerMessage', '컨테이너가 삭제되었습니다.', false);
                setTimeout(() => redirectTo('index.html'), 2000);
            })
            .catch(error => {
                console.error('컨테이너 삭제 오류:', error);
                showMessage('containerMessage', '컨테이너 삭제에 실패했습니다.');
            });
    }
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
