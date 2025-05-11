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
    
    // 컨테이너 상세 페이지
    else if (currentPage.includes('container.html')) {
        const containerId = getUrlParam('id');
        if (containerId) {
            loadContainerDetails(containerId);
            
            // 컨트롤 버튼 이벤트 리스너 설정
            setupContainerControls(containerId);
        } else {
            redirectTo('index.html');
        }
    }
    
    // 컨테이너 생성 페이지
    else if (currentPage.includes('new_container.html')) {
        setupContainerForm();
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
        
        // 컨테이너 정보 표시
        document.getElementById('containerImage').textContent = containerInfo.image;
        document.getElementById('containerId').textContent = containerInfo.containerId;
        document.getElementById('containerStatus').textContent = containerInfo.status;
        document.getElementById('containerStatus').className = `badge ${getStatusClass(containerInfo.status)}`;
        document.getElementById('containerStarted').textContent = formatDate(containerInfo.startedAt);
        document.getElementById('containerPorts').textContent = `${containerInfo.ports.containerPort} → ${containerInfo.ports.hostPort}`;
        
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
 * 컨테이너 제어 버튼 설정 함수
 * @param {string} containerId - 컨테이너 ID
 */
function setupContainerControls(containerId) {
    // 시작 버튼
    const startBtn = document.getElementById('startContainer');
    if (startBtn) {
        startBtn.addEventListener('click', async () => {
            try {
                await startContainer(containerId);
                showMessage('containerMessage', '컨테이너가 시작되었습니다.', false);
                loadContainerDetails(containerId);
            } catch (error) {
                showMessage('containerMessage', '컨테이너 시작에 실패했습니다.');
            }
        });
    }
    
    // 정지 버튼
    const stopBtn = document.getElementById('stopContainer');
    if (stopBtn) {
        stopBtn.addEventListener('click', async () => {
            try {
                await stopContainer(containerId);
                showMessage('containerMessage', '컨테이너가 정지되었습니다.', false);
                loadContainerDetails(containerId);
            } catch (error) {
                showMessage('containerMessage', '컨테이너 정지에 실패했습니다.');
            }
        });
    }
    
    // 재시작 버튼
    const restartBtn = document.getElementById('restartContainer');
    if (restartBtn) {
        restartBtn.addEventListener('click', async () => {
            try {
                await restartContainer(containerId);
                showMessage('containerMessage', '컨테이너가 재시작되었습니다.', false);
                loadContainerDetails(containerId);
            } catch (error) {
                showMessage('containerMessage', '컨테이너 재시작에 실패했습니다.');
            }
        });
    }
    
    // 삭제 버튼
    const removeBtn = document.getElementById('removeContainer');
    if (removeBtn) {
        removeBtn.addEventListener('click', async () => {
            if (confirm('정말로 이 컨테이너를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
                try {
                    await removeContainer(containerId);
                    showMessage('containerMessage', '컨테이너가 삭제되었습니다.', false);
                    setTimeout(() => redirectTo('index.html'), 2000);
                } catch (error) {
                    showMessage('containerMessage', '컨테이너 삭제에 실패했습니다.');
                }
            }
        });
    }
}

/**
 * 컨테이너 생성 폼 설정 함수
 */
function setupContainerForm() {
    const containerForm = document.getElementById('containerForm');
    if (containerForm) {
        // 폼 제출 이벤트 리스너
        containerForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            
            // 폼 데이터 수집
            const imageName = document.getElementById('imageName').value;
            // 이미지 이름이 비어있는지 확인
            if (!imageName) {
                showMessage('formMessage', '이미지 이름은 필수입니다.');
                return;
            }
            
            // 포트는 입력 값이 있는 경우만 포함
            const containerPortInput = document.getElementById('containerPort').value;
            
            // 환경 변수 수집 - 키와 값이 둘 다 있는 경우만 포함
            const envVars = {};
            document.querySelectorAll('.env-row').forEach(row => {
                const key = row.querySelector('.env-key').value;
                const value = row.querySelector('.env-value').value;
                if (key && value) {
                    envVars[key] = value;
                }
            });
            
            // 명령어 처리 - 값이 있는 경우만 포함
            const command = document.getElementById('command').value;
            
            // 컨테이너 생성 요청 데이터 - 필수 값인 imageName만 기본 포함
            const containerData = {
                imageName
            };
            
            // 선택적 필드는 값이 있을 때만 추가
            if (containerPortInput) {
                containerData.containerPort = parseInt(containerPortInput);
            }
            
            if (Object.keys(envVars).length > 0) {
                containerData.env = envVars;
            }
            
            if (command) {
                containerData.cmd = command.split(' ').filter(Boolean);
            }
            
            try {
                const submitBtn = containerForm.querySelector('button[type="submit"]');
                submitBtn.disabled = true;
                submitBtn.innerHTML = '생성 중...';
                
                // 컨테이너 생성
                const response = await createContainer(containerData);
                
                // 생성 성공
                showMessage('formMessage', '컨테이너가 성공적으로 생성되었습니다.', false);
                
                // 응답에서 컨테이너 ID 추출
                const containerId = response.split(',')[0].split(':')[1].trim();
                
                // 컨테이너 상세 페이지로 이동
                setTimeout(() => redirectTo(`container.html?id=${containerId}`), 2000);
            } catch (error) {
                console.error('컨테이너 생성 오류:', error);
                showMessage('formMessage', '컨테이너 생성에 실패했습니다.');
                
                const submitBtn = containerForm.querySelector('button[type="submit"]');
                submitBtn.disabled = false;
                submitBtn.innerHTML = '컨테이너 생성';
            }
        });
        
        // 환경 변수 추가 버튼
        const addEnvBtn = document.getElementById('addEnvVar');
        if (addEnvBtn) {
            addEnvBtn.addEventListener('click', () => {
                const envContainer = document.getElementById('environmentVars');
                const newRow = document.createElement('div');
                newRow.className = 'row env-row mb-2';
                newRow.innerHTML = `
                    <div class="col-5">
                        <input type="text" class="form-control env-key" placeholder="키">
                    </div>
                    <div class="col-5">
                        <input type="text" class="form-control env-value" placeholder="값">
                    </div>
                    <div class="col-2">
                        <button type="button" class="btn btn-danger remove-env">삭제</button>
                    </div>
                `;
                envContainer.appendChild(newRow);
                
                // 삭제 버튼 이벤트 리스너
                newRow.querySelector('.remove-env').addEventListener('click', () => {
                    envContainer.removeChild(newRow);
                });
            });
        }
        
        // 템플릿 선택 이벤트 리스너
        const templateSelect = document.getElementById('templateSelect');
        if (templateSelect) {
            templateSelect.addEventListener('change', () => {
                const value = templateSelect.value;
                if (value !== 'custom') {
                    // 템플릿 값 적용
                    const templates = containerTemplates;
                    const template = templates[value];
                    if (template) {
                        // 이미지는 필수값
                        document.getElementById('imageName').value = template.image;
                        
                        // 포트는 옵션이지만 있으면 설정 (기본값: "")
                        document.getElementById('containerPort').value = template.port || "";
                        
                        // 명령어도 옵션이지만 있으면 설정 (기본값: "")
                        document.getElementById('command').value = template.cmd || "";
                        
                        // 환경 변수 설정
                        const envContainer = document.getElementById('environmentVars');
                        envContainer.innerHTML = '';
                        
                        // env가 있는 경우에만 처리
                        if (template.env) {
                            for (const [key, value] of Object.entries(template.env)) {
                                const newRow = document.createElement('div');
                                newRow.className = 'row env-row mb-2';
                                newRow.innerHTML = `
                                    <div class="col-5">
                                        <input type="text" class="form-control env-key" value="${key}" placeholder="키">
                                    </div>
                                    <div class="col-5">
                                        <input type="text" class="form-control env-value" value="${value}" placeholder="값">
                                    </div>
                                    <div class="col-2">
                                        <button type="button" class="btn btn-danger remove-env">삭제</button>
                                    </div>
                                `;
                                envContainer.appendChild(newRow);
                                
                                // 삭제 버튼 이벤트 리스너
                                newRow.querySelector('.remove-env').addEventListener('click', () => {
                                    envContainer.removeChild(newRow);
                                });
                            }
                        }
                    }
                }
            });
        }
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
