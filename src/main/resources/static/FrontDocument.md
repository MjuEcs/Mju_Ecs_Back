### 프로젝트 개요
mju_ecs는 명지대 학생들을 대상으로 컨테이너 컴퓨팅 자원을 빌려주는 서비스 입니다.    
aws  의 컨테이너 서비스인 ecs 서비스와 일정 부분 비슷하지만 무료 유료 부분, 규모에서 차이가 있습니다  

### 기술 스택 및 구현 우선순위, 구조

HTML, CSS, JavaScript, Bootstrap 으로 프레임워크 없는 웹페이지를 구성합니다  
인증 시스템 (로그인/토큰 관리)   
메인 페이지 컨테이너 목록 및 상태 표시 `index.html`  
컨테이너 추가 페이지 `new_container.html`  
컨테이너 상태 페이지 `container.html`  

├── css
│   ├── container.css
│   ├── login.css
│   └── style.css
├── js
│   ├── api.js
│   ├── auth.js
│   ├── container.js
│   ├── container_templates.js
│   ├── containers.js
│   ├── new_container.js
│   └── utils.js
├── index.html
├── login.html
├── container.html
└── new_container.html

- containers.js:
	- 메인 페이지(index.html)의 컨테이너 목록 관련 기능
	- 컨테이너 통계 업데이트 기능
- container.js:
	- 컨테이너 상세 정보 페이지(container.html) 관련 기능
	- 컨테이너 상세 정보 표시 기능
	- 컨테이너 제어(시작, 중지, 재시작, 삭제) 기능
	- 문서 표시 기능
- new_container.js:
	- 새 컨테이너 생성 페이지(new_container.html) 관련 기능
	- 템플릿 선택 및 표시 기능
	- 컨테이너 생성 폼 처리 기능

css 는 최소로 사용하고 bootstrap 을 사용

### 페이지 구성  
`index.html` 로그인 완료및 최초 페이지 : 최초로 보이는 페이지  
첫번째 열 : 우측 상단 사용자 학번 정보파랑색 배경의 mju_ecs 서비스 글자  
두번째 열 : 현재 사용자의 컨테이너 개수 및 상태 박스 카드 형태 (좌측 정렬), 우측정렬 + 버튼 컨테이너 추가,   최우측에는 상태 업데이트 버튼
중앙 : 사용자 공지
하단 : 서비스 정보 및 푸터

`login.html` 로그인시 보이는 페이지
학번, 이름, 비밀번호를 기입
이름은 향후 제거될 기능이므로 api 요청할때만 보내기

`new_container.html` : 컨테이너 추가 페이지 미리 정의해둔 template 또는 menual 방식으로 추가 가능함
템플릿 선택 옵션  
수동 설정 폼(이미지 이름, 포트, 환경변수, 명령어)  
/docker/custom/run API 호출 구현  

`container.html` : 컨테이너의 상태와 정보를 볼 수 있음, 
컨테이너에 접근 가능한 url 이 있음, 버튼을 누르면 새탭에서 해당하는 컨테너의 터미널 세션이 열림  
컨테이너 ID, 상태, 이미지, 시작 시간 등 표시  
컨테이너 제어 버튼(시작, 정지, 재시작, 삭제)  
컨테이너 접근 URL 및 터미널 열기 버튼  

### 요청 API

#### 로그인 처리
```json
요청 PATH : /auth/login
{
    "studentId": "0000000", // 실제 학번
    "name": "신년기",        // 이름 (이후에는 의존성 제거할 거임 로그인처리에만 사용하고 실제 사용은 x)
    "passwrd": "00000000"   // 비밀번호
}


응답
Login successful
```


이러한 방식으로 토큰이 세팅 됩니다

|       |        |           |     |                               |       |       |
| ----- | ------ | --------- | --- | ----------------------------- | ----- | ----- |
| token | ...... | localhost | /   | Sat, 05 Feb 2028 14:38:39 GMT | false | false |
명지대학교 포털사이트 로그인이 가능한 학생을 기반으로 로그인이 가능한 인원인 경우 서버에서 학번만 db 에 저장하고 token 을 발행 만약 토큰만료 시 다시 로그인 수행으로 진행 프론트에서는 토큰 여부를 가지고 수행할 수 있음

#### 생성
`POST /docker/custom/run` 으로 
```json
{
    "imageName" : "ubuntu:22.04",
    "containerPort" : 22,
    "env" : {
        "envkey1" : "envvalue1",
        "envkey2" : "envvalue2"
    },
    "cmd" : [ "sleep" , "infinity"]
}
```

보내면 
```
containerId: 738fd10e06c994bda393900b55dc272f7464971d76e92f6b20c7ba937b53970e, ttydUrl: http://localhost:15419
```

이렇게 응답이 오고 컨테이너 id 와 cmd 가 보여집니다
#### 상태
GET `/docker/status` 보낼시 사용자의 컨테이너 정보가 json 으로 옵니다
```json
[
    {
        "ports": {
            "hostPort": 19194,
            "containerPort": 22
        },
        "status": "running",
        "containerId": "738fd10e06c994bda393900b55dc272f7464971d76e92f6b20c7ba937b53970e",
        "image": "ubuntu:22.04",
        "startedAt": "2025-05-11T14:42:44.035153498Z"
    },
    {
        "ports": {
            "hostPort": 14998,
            "containerPort": 22
        },
        "status": "running",
        "containerId": "e511095f8a8a814ba15105e1b63a38bbfb62cdb755f2c65c1e23e49bc7397b73",
        "image": "ubuntu:22.04",
        "startedAt": "2025-05-11T14:44:29.501959144Z"
    }
]
```
만약
GET `/docker/status?containerId=738fd10e06c994bda393900b55dc272f7464971d76e92f6b20c7ba937b53970e`
이렇게 인수로 id 를 보내면 
```json
{
    "ports": {
        "hostPort": 19194,
        "containerPort": 22
    },
    "status": "running",
    "containerId": "738fd10e06c994bda393900b55dc272f7464971d76e92f6b20c7ba937b53970e",
    "image": "ubuntu:22.04",
    "startedAt": "2025-05-11T14:42:44.035153498Z"
}
```
해당하는 id 컨테이너 정보만 들어옵니다


#### 정지, 시작, 다시시작, 삭제
4개는 
인수로 containerId 가 필요하고
각각
POST `/docker/stop?containerId=1733658b61d30717d2083467ba656abd68b29bfa7d075ed629497d557403b7cd`
POST `/docker/start?containerId=1733658b61d30717d2083467ba656abd68b29bfa7d075ed629497d557403b7cd`
POST `/docker/restart?containerId=1733658b61d30717d2083467ba656abd68b29bfa7d075ed629497d557403b7cd`
DEL `/docker/remove?containerId=1733658b61d30717d2083467ba656abd68b29bfa7d075ed629497d557403b7cd`

이런식 입니다

