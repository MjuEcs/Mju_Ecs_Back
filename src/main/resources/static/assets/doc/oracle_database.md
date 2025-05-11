### 변수 설명 (설명에서 사용할 변수는 $표시가 붙습니다 자신이 설정한 또는 설정하고 싶은것을 넣으면 됩니다)
- `ORACLE_PASSWORD` : 컨테이너 생성시에 만든 관리자 비밀번호  
- `USER_NAME` : 유저 이름  
- `USER_PASSWORD` : 유저 이름  
- `ACCESS_IP` : 접근 ip  
- `ACCESS_PORT` : 접근 포트  


### 유저 생성
터미널에서 관리자 권한으로 sqlplus 로그인
```bash
bash-4.4$ sqlplus sys/$ORACLE_PASSWORD as SYSDBA
```

#### 유저 생성및 권한 부여

oracle database 11 버전인 경우
```sql
SQL> CREATE USER $user_name IDENTIFIED BY $user_password;
GRANT CREATE SESSION TO $user_name;
GRANT CONNECT, RESOURCE TO $user_name;
GRANT CREATE MATERIALIZED VIEW TO $user_name;
ALTER USER $user_name DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;
```

oracle database 18, 21 버전인 경우
```sql
SQL> ALTER SESSION SET "_ORACLE_SCRIPT"=true;
CREATE USER $user_name IDENTIFIED BY $user_password;
GRANT CREATE SESSION TO $user_name;
GRANT CONNECT, RESOURCE TO $user_name;
GRANT CREATE MATERIALIZED VIEW TO $user_name;
ALTER USER $user_name DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;
```

### 연결
oracle 에서 기본제공하는 클라인언트인 sqldeveloper 에서 연결할 때를 예시  
![](assets/images/oracle_database.png)
