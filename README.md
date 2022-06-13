# MyMemo
간단한 메모앱입니다  
사용된 폰트는 "Gmarket Sans"입니다. (http://company.gmarket.co.kr/company/about/company/company--font.asp)
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
>태그별 분류 기능  
>메모 수정 화면에서 현재 시간 추가하는 버튼 추가  
>프래그먼트 이동 애니메이션 추가  
>DiffUtil.ItemCallback과 ListAdapter 사용하여 리사이클러뷰 업데이트하도록 변경

***ver 1.6.3***
>drawer 메뉴에서 선택된 항목 강조 표시  
>drawer 메뉴에서 상태표시줄 완전 투명하게 변경  
>앱 내에서 클릭이벤트 발생시 ripple 효과 추가
