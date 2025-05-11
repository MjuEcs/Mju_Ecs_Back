/**
 * 컨테이너 관리 기능 함수
 */

document.addEventListener('DOMContentLoaded', function() {
    // 로그인 상태 확인
    checkAuth();
    
    // 현재 페이지 확인
    const currentPage = window.location.pathname;
    
    // 메인 페이지 (컨테이너 목록)
    if (currentPage.includes('index.html') || currentPage.endsWith('/')) {
        loadContainers();
        
        // 새로고침 버튼 이벤트 리스너 설정
        const refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', loadContainers);
        }
    }
});

/**
 * 컨테이너 목록 불러오기 함수
 */
async function loadContainers() {
    try {
        const containerList = document.getElementById('containerList');
        const containerCount = document.getElementById('containerCount'); // Get the container count element
        if (!containerList) return;

        // 로딩 표시
        containerList.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"><span class="visually-hidden">Loading...</span></div></div>';

        // 컨테이너 목록 조회
        const containers = await getContainers();

        // 컨테이너가 없는 경우
        if (!containers || containers.length === 0) {
            containerList.innerHTML = '<div class="alert alert-info">생성된 컨테이너가 없습니다. 새 컨테이너를 추가해보세요.</div>';
            if (containerCount) containerCount.textContent = '0'; // Update count to 0
            return;
        }

        // 컨테이너 목록 표시
        let html = '';
        containers.forEach(container => {
            const statusClass = getStatusClass(container.status);
            const formattedDate = formatDate(container.startedAt);

            html += `
                <div class="col-md-6 col-lg-4 mb-4">
                    <div class="card h-100">
                        <div class="card-header d-flex justify-content-between align-items-center">
                            <span class="badge ${statusClass}">${container.status}</span>
                            <small>${formattedDate}</small>
                        </div>
                        <div class="card-body">
                            <h5 class="card-title text-truncate" title="${container.image}">${container.image}</h5>
                            <p class="card-text small">ID: ${container.containerId.substring(0, 12)}...</p>
                            <p class="card-text">포트: ${container.ports.containerPort} → ${container.ports.hostPort}</p>
                            <a href="container.html?id=${container.containerId}" class="btn btn-primary stretched-link">상세 보기</a>
                        </div>
                    </div>
                </div>
            `;
        });

        containerList.innerHTML = html;

        // Update container count
        if (containerCount) containerCount.textContent = containers.length.toString();

        // 통계 업데이트
        updateContainerStats(containers);

    } catch (error) {
        console.error('컨테이너 목록 로딩 오류:', error);
        const containerList = document.getElementById('containerList');
        if (containerList) {
            containerList.innerHTML = '<div class="alert alert-danger">컨테이너 목록을 불러오는데 실패했습니다.</div>';
        }
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
 * 컨테이너 통계 업데이트 함수
 * @param {Array} containers - 컨테이너 목록
 */
function updateContainerStats(containers) {
    const totalElement = document.getElementById('totalContainers');
    const runningElement = document.getElementById('runningContainers');
    const stoppedElement = document.getElementById('stoppedContainers');
    
    if (totalElement && runningElement && stoppedElement) {
        const total = containers.length;
        const running = containers.filter(c => c.status.toLowerCase() === 'running').length;
        const stopped = total - running;
        
        totalElement.textContent = total;
        runningElement.textContent = running;
        stoppedElement.textContent = stopped;
    }
}

/**
 * 컨테이너 목록 불러오기 함수
 */
async function loadContainers() {
    try {
        const containerList = document.getElementById('containerList');
        const containerCount = document.getElementById('containerCount');
        if (!containerList) return;

        // 로딩 표시
        containerList.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"><span class="visually-hidden">Loading...</span></div></div>';

        // 컨테이너 목록 조회
        const containers = await getContainers();

        // 컨테이너가 없는 경우
        if (!containers || containers.length === 0) {
            containerList.innerHTML = '<div class="alert alert-info">생성된 컨테이너가 없습니다. 새 컨테이너를 추가해보세요.</div>';
            if (containerCount) containerCount.textContent = '0';
            return;
        }

        // 컨테이너 목록 표시
        let html = '';
        containers.forEach(container => {
            const statusClass = getStatusClass(container.status);
            const formattedDate = formatDate(container.startedAt);

            html += `
                <div class="col-md-6 col-lg-4 mb-4">
                    <div class="card h-100">
                        <div class="card-header d-flex justify-content-between align-items-center">
                            <span class="badge ${statusClass}">${container.status}</span>
                            <small>${formattedDate}</small>
                        </div>
                        <div class="card-body">
                            <h5 class="card-title text-truncate" title="${container.image}">${container.image}</h5>
                            <p class="card-text small">ID: ${container.containerId.substring(0, 12)}...</p>
                            <p class="card-text">포트: ${container.ports.containerPort} ↔ ${container.ports.hostPort}</p>
                            <a href="container.html?id=${container.containerId}" class="btn btn-primary stretched-link">상세 보기</a>
                        </div>
                    </div>
                </div>
            `;
        });

        containerList.innerHTML = html;

        // Update container count
        if (containerCount) containerCount.textContent = containers.length.toString();

        // 통계 업데이트
        updateContainerStats(containers);

    } catch (error) {
        console.error('컨테이너 목록 로딩 오류:', error);
        const containerList = document.getElementById('containerList');
        if (containerList) {
            containerList.innerHTML = '<div class="alert alert-danger">컨테이너 목록을 불러오는데 실패했습니다.</div>';
        }
    }
}

/**
 * 컨테이너 통계 업데이트 함수
 * @param {Array} containers - 컨테이너 목록
 */
function updateContainerStats(containers) {
    const totalElement = document.getElementById('totalContainers');
    const runningElement = document.getElementById('runningContainers');
    const stoppedElement = document.getElementById('stoppedContainers');
    
    if (totalElement && runningElement && stoppedElement) {
        const total = containers.length;
        const running = containers.filter(c => c.status.toLowerCase() === 'running').length;
        const stopped = total - running;
        
        totalElement.textContent = total;
        runningElement.textContent = running;
        stoppedElement.textContent = stopped;
    }
}
