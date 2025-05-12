/**
 * 새 컨테이너 생성 페이지 관련 함수
 */

document.addEventListener('DOMContentLoaded', function() {
    // 로그인 상태 확인
    checkAuth();
    
    // 이벤트 리스너 설정
    document.getElementById('logoutBtn').addEventListener('click', logout);
    
    // 탭 설정
    setupTabs();
    
    // 템플릿 카드 렌더링
    renderTemplateCards();
    
    // 환경 변수 삭제 버튼 설정
    setupRemoveEnvButtons();
    
    // 컨테이너 폼 설정
    setupContainerForm();
});

/**
 * 탭 설정 함수
 */
function setupTabs() {
    const templateTab = document.getElementById('template-tab');
    const manualTab = document.getElementById('manual-tab');
    
    if (templateTab && manualTab) {
        // 템플릿 탭 클릭 이벤트
        templateTab.addEventListener('click', () => {
            document.getElementById('template').classList.add('show', 'active');
            document.getElementById('manual').classList.remove('show', 'active');
        });
        
        // 수동 설정 탭 클릭 이벤트
        manualTab.addEventListener('click', () => {
            document.getElementById('template').classList.remove('show', 'active');
            document.getElementById('manual').classList.add('show', 'active');
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
            const command = document.getElementById('cmd').value;
            
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
            
            // cmd 처리: 값이 있으면 공백으로 분리하고, 없으면 빈 배열로 설정
            containerData.cmd = command ? command.split(' ').filter(Boolean) : [];
            
            // Ensure `env` is always included in the request, even if empty
            if (!containerData.env) {
                containerData.env = {};
            }
            
            try {
                console.log('컨테이너 생성 요청 데이터:', containerData);
                const submitBtn = containerForm.querySelector('button[type="submit"]');
                submitBtn.disabled = true;
                submitBtn.innerHTML = '생성 중...';
                
                // 컨테이너 생성
                const response = await createContainer(containerData);
                
                // 생성 성공
                showMessage('formMessage', '컨테이너가 성공적으로 생성되었습니다.', false);
                
                // 컨테이너 생성 성공 후 index.html로 이동
                setTimeout(() => redirectTo('index.html'), 2000);
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
                addEnvRow();
            });
        }
        
        // 템플릿 선택 이벤트 리스너
        const templateSelect = document.getElementById('templateSelect');
        if (templateSelect) {
            templateSelect.addEventListener('change', () => {
                const value = templateSelect.value;
                if (value !== 'custom') {
                    // 템플릿 값 적용
                    const template = containerTemplates[value];
                    if (template) {
                        selectTemplate(value);
                    }
                }
            });
        }
    }
}

/**
 * 환경 변수 행 추가 함수
 */
function addEnvRow() {
    const envContainer = document.getElementById('environmentVars');
    if (!envContainer) return;
    
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
}

/**
 * 환경 변수 삭제 버튼 설정 함수
 */
function setupRemoveEnvButtons() {
    document.querySelectorAll('.remove-env').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.target.closest('.env-row').remove();
        });
    });
}

/**
 * 템플릿 카드 렌더링 함수
 */
function renderTemplateCards() {
    const templateCardsContainer = document.getElementById('templateCards');
    
    if (!templateCardsContainer) return;
    
    // 기존 컨텐츠 초기화
    templateCardsContainer.innerHTML = '';
    
    // 각 템플릿에 대한 카드 생성
    for (const [templateId, templateData] of Object.entries(containerTemplates)) {
        const colDiv = document.createElement('div');
        colDiv.className = 'col';
        
        // 템플릿 이름을 보기 좋게 변환 (첫 글자 대문자로)
        const templateName = templateId.charAt(0).toUpperCase() + templateId.slice(1);
        
        colDiv.innerHTML = `
            <div class="card h-100">
                <div class="card-body">
                    <h5 class="card-title">${templateName}</h5>
                    <p class="card-text">${templateData.image}</p>
                    <button class="btn btn-primary btn-sm template-select" data-template="${templateId}">선택</button>
                </div>
            </div>
        `;
        
        templateCardsContainer.appendChild(colDiv);
    }
    
    // 템플릿 선택 버튼 이벤트 리스너 설정
    document.querySelectorAll('.template-select').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const template = e.target.getAttribute('data-template');
            selectTemplate(template);
        });
    });
}

/**
 * 템플릿 선택 함수
 * @param {string} templateId - 템플릿 ID
 */
function selectTemplate(templateId) {
    // 탭 전환
    const manualTab = document.getElementById('manual-tab');
    if (manualTab) {
        const tabInstance = new bootstrap.Tab(manualTab);
        tabInstance.show();
    }
    
    // 템플릿 데이터 가져오기
    const templateData = containerTemplates[templateId];
    
    if (templateData) {
        // 이미지 이름 설정
        const imageNameInput = document.getElementById('imageName');
        if (imageNameInput) imageNameInput.value = templateData.image || '';
        
        // 포트 설정
        const containerPortInput = document.getElementById('containerPort');
        if (containerPortInput) containerPortInput.value = templateData.port || '';
        
        // 명령어 설정
        const cmdInput = document.getElementById('cmd');
        if (cmdInput) cmdInput.value = templateData.cmd || '';
        
        // 환경 변수 설정
        setupEnvironmentVariables(templateData.env);
    }
}

/**
 * 템플릿의 환경 변수 설정 함수
 * @param {Object} envVars - 환경 변수 객체
 */
function setupEnvironmentVariables(envVars) {
    const envContainer = document.getElementById('envVars');
    if (!envContainer) return;
    
    // 기존 환경 변수 행 모두 삭제
    envContainer.innerHTML = '';
    
    // 템플릿에 환경 변수가 있는 경우에만 설정
    if (envVars && Object.keys(envVars).length > 0) {
        // 환경 변수 행 추가
        Object.entries(envVars).forEach(([key, value]) => {
            const envDiv = document.createElement('div');
            envDiv.className = 'row env-row mb-2';
            envDiv.innerHTML = `
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
            envContainer.appendChild(envDiv);
        });
        
        // 환경 변수 삭제 버튼 설정 갱신
        setupRemoveEnvButtons();
    } else {
        // 기본 빈 환경 변수 행 추가
        addEnvRow();
    }
}
