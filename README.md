# MyMemo
간단한 메모앱입니다

- Android Jetpack - room database를 사용하여 메모 저장  
- CollapsingToolbarLayout을 사용하여 스크롤 정도에 따른 레이아웃 구현  
- Firebase - Authentication을 사용하여 구글 로그인 기능 제공  
- Firebase - Storage를 사용하여 메모 클라우드 백업 기능 구현  
- MediaStore 기능을 사용하여 로컬 백업 기능 구현 (안드로이드 버전에 따른 대응)  
- 코루틴을 사용하여 메모 클라우드 백업시 기능 최적화
- 앱 내의 설정 저장은 SharedPreference 기능을 사용하여 구현  

앱에 사용 된 사용된 폰트는 "Gmarket Sans"(http://company.gmarket.co.kr/company/about/company/company--font.asp) 입니다  
백업 및 복원시 나오는 로딩 애니메이션은 (https://lottiefiles.com/whjxzhwwli) 입니다
<br>

### changelog 
***ver 1.3***  
>DiffUtil을 사용하여 리사이클러뷰 업데이트하도록 변경

***ver 1.3.1***  
>MemoViewModel이 context를 참조하지 않도록 AndroidViewModel 대신 ViewModel을 상속받도록 변경  
>Application 클래스를 상속받는 App 클래스를 새로 생성해 전역 context(applicationContext)로 사용

***ver 1.4***  
>시작화면(리스트) 타이틀을 CollapsingToolbarLayout으로 변경  
>메모 삭제버튼을 제거하고 스와이프로 제거하도록  
>스낵바 커스텀 레이아웃으로 변경

***ver 1.5***
>메모 검색 기능 추가

***ver 1.6***
>라벨을 통한 분류 기능 추가(SharedPreference 사용)
>메모 수정 화면에서 현재 시간 추가하는 버튼 추가  
>프래그먼트 이동 애니메이션 추가  
>DiffUtil.ItemCallback과 ListAdapter 사용하여 리사이클러뷰 업데이트하도록 변경

***ver 1.6.3***
>drawer 메뉴에서 선택된 항목 강조 표시  
>drawer 메뉴에서 상태표시줄 완전 투명하게 변경  
>앱 내에서 클릭이벤트 발생시 ripple 효과 추가

***ver 1.7***
>메모 로컬 백업 기능 추가

***ver 1.8***
>메모 클라우드 백업 기능 추가 (Firebase - Auth, Storage)  
>라벨 기능을 SharedPreference를 사용하지 않는 방식으로 변경  
> => 라벨과 메모 데이터베이스를 별도로 관리하다보니 메모를 복원하거나 가져오기 했을시 라벨이 사라지는 문제 대응

***ver 1.8.7***
>백업이나 복원 완료시 진동 피드백 on/off 기능 추가  
>백업이나 복원 중 뒤로가기 및 다른 작업 터치 방지 기능 추가  
>메모 검색 기능에 Flow-debounce 기능 사용  
> =>메모 검색시 검색어 변경이 0.35초 동안 없을시 검색 실행  

***ver 1.9***
>메모 입력후 홈버튼을 눌러 나갔을 때 자동 저장 기능 추가  
> => onStop에서 자동 저장 기능 구현

***ver 1.9.1***
>시간 추가 버튼 설정 추가 -> (연도 / 월 / 시간) 설정 가능

***ver 1.9.2***
>메모 수정 화면(프래그먼트)에서 EditText의 힌트를 제목/메모 에서 제목/내용으로 변경 

***ver 1.9.3***
>스마트폰 언어 변경시 기존  SimpleTimeFormat().parse()할 때 오류 생기는 문제 해결  
> => 메모 시간 저장시 `Locale.getDefault().language`로 언어 설정도 같이 저장 후 불러오도록 수정

***ver 1.9.5***
>안드로이드 버전 업그레이드에 따른 저장소 이용정책 변경에 따른 업데이트
> => MediaStore 기능을 사용하여 로컬 백업 기능 구현  
> => 미해결 문제점1 : 메모 백업 파일이 복사본과 같이 원본이 아닐시 복원 안됨 (백업 위치를 고정하여 복사본 생성 확률을 줄이는 방식으로 임시 대응)
