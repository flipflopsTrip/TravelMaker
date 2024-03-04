# 기능명세서

### 1. 회원가입
- 카카오 소셜 가입
```
카카오 이메일(varchar 100)
프로필 사진(varchar1000)
이름(varchar 50)
성별 (varchar 50)
연령대 (int)
생일 (Date ⇒ YYYY-MM-DD)
```

### 2. 로그인
- 카카오 소셜 로그인
```
[ Server ]
기능 1 : 로그인시 Access Token, Refresh Token 발급
기능 2 : Refresh Token DB에 저장
기능 3 : User 객체 + Flag 값(현재 어느 페이지로 리다이렉트로 갈지)
Flag : 0 (메인-코스전), 1 (메인-코스후), 2(메인-여행중)
선택 기능 1 : Refresh Token Redis 캐싱 처리 

[ Client ] 
기능 1 : 로그인시 반환되는  Access Token, Refresh Token Local Stroage 에 저장 및 상태관리 Store에 저장
```

### 3. navbar

```
[ 비로그인 시 ]

메인페이지 클릭 시 ⇒ 메인페이지 이동

마이페이지 클릭 시 ⇒ 로그인 페이지 이동

여행코스짜기 클릭 시 ⇒ 로그인 페이지 이동

[ 로그인 시 ]

각자 해당 페이지로 이동
```

### 4. 메인


- 코스 전
```
[ 공통 ] 

1. 상단 이모티콘
2. 상태 메세지
3. 하단 추천 여행지 목록 (무한 스크롤)
4. 여행지 정보 클릭시 (화면 회전하며) 상세 내용 보여주기
5. 여행지 정보 좋아요 기능(DB에 저장)

[ 후기 작성 필요시 ]

1. 후기 작성 알림 활성화 (토스식: 일정시간 이후 사라짐)
```

- 코스 후
```
코스 후 페이지를 어느 시점부터 보여줄까?
⇒ 2주전부터

1. 상단 이모티콘
2. 상태 메세지 (D-day, 여행지/ ex) ㅇㅇ까지 D-20)
3. 예정 날씨 (출발날짜 기준)
    1. 이모티콘으로 날씨 표현
    2. 프론트에서
4. 코스 상세페이지 버튼
```

- 여행 중
```
1. 상단 이모티콘
2. 날씨 (출발날짜 기준)
    1. 이모티콘으로 날씨 표현
    2. 프론트에서
3. 당일 코스 상세 + 터치하면 텍스트 + 슬라이드로 코스 보기
```

### 5. 코스

- 날짜, 교통수단 선택
```
최대 3일만 선택가능

1. 캘린더 라이브러리
2. 동일 날짜 중복 일정 못 만들게 캘린더 날짜 비활성화(선택)
3. 이동수단 선택버튼 3개(자동차, 대중교통, 뚜벅이)
```

- 도 선택
```
1. 8개의 도 선택 버튼 증 한개 클릭 ⇒ 도 선택하기
```
