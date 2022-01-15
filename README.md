# KNU-CapstoneDesign-20212-Team10
- 경북대학교 종합설계프로젝트1 10팀입니다.
- 진행기간: 2021.9.1~2021.12.20

```
             TEAM : JONGSIP
컴퓨터학부 2017114915 김민수
컴퓨터학부 2017114846 김두영
컴퓨터학부 2017111547 박지민
컴퓨터학부 2017113223 이승우
```
<br/>

## Title  << 카페 어디가? >>

<img src="https://github.com/KIMTHE/KNU-CapstoneDesign-20212-Team10/blob/main/android/app/src/main/res/mipmap-xxxhdpi/ic_jongsip.png?raw=true" width="20%" height="15%" alt="TitleLogo"></img>

###### 카페 관련 유틸리티 어플리케이션

---------------------------------------------
## [목차]
- [💡개발배경](#개발배경)
- [📚Task 관리](#Task-관리)
- [💻주요 기능](#주요-기능)
- [👩‍💻기술스택](#기술스택)
- [🖥프로젝트 결과](#프로젝트-결과)
- 📽[데모영상](#데모영상)
--------------------------------------------

## 💡개발배경

- [최종보고서 참고](https://github.com/KIMTHE/KNU-CapstoneDesign-20212-Team10/blob/main/document/%EC%82%B0%ED%95%99%ED%98%91%EB%A0%A5%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8_%EA%B2%B0%EA%B3%BC%EB%B3%B4%EA%B3%A0%EC%84%9C_%EC%B9%B4%ED%8E%98%20%EC%9C%A0%ED%8B%B8%EB%A6%AC%ED%8B%B0%20%EC%84%9C%EB%B9%84%EC%8A%A4.hwp)
- 카페들은 점점 늘어나고 있는 추세지만 사람들은 카페가 정확이 어디에 있는지 알기 어려움
- 해당 카페를 방문하기 전까지는 무슨 메뉴가 있는지 알기 어려움
- 카페 방문시 WIFI에 접속할 때 직접 WIFI 비밀번호를 입력해야 하는 불편한 점이 있음
- 이러한 불편한 점들을 개선할 수 있는 유틸리티 애플리케이션 개발
<br/>

## 📚Task 관리

###### 관련 링크
> - [git convention](https://github.com/KIMTHE/KNU-CapstoneDesign-20212-Team10/wiki/git-convention)
> - [회의록](https://github.com/KIMTHE/KNU-CapstoneDesign-20212-Team10/tree/main/document/%ED%9A%8C%EC%9D%98%EB%A1%9D)
> - [WBS(일정)](https://docs.google.com/spreadsheets/d/1YH-_gQ2qo4Aq0yc0xtIM3LOHUwDtytY-z27IbBne7KM/edit?usp=drive_web&ouid=112937991178410089043)
> - [issue 관리](https://github.com/KIMTHE/KNU-CapstoneDesign-20212-Team10/issues)
<br/>

## 💻주요 기능
#### 1️⃣지도를 통해 주변 카페를 확인 할 수 있다.
  * 카카오 검색 API의 검색 조건으로는 키워드, 카테고리 그룹(카페), 현재 위치정보 등을 입력하여, 가까운 15개의 카페의 정보(위도, 경도, 카페 상세페이지 URL, 카페 이름)를 마커로 표시한다. 
  * 위치정보 사용에 동의했다면 getLatLng 함수에서는 LocationManager을 이용하여 현재 위치를 업데이트하고, 해당 위치 정보를 이용하여, 현재 위치로 지도 화면을 이동한다.
  * 마커를 클릭하면 해당 마커의 위치에 해당되는 카페의 이름이 나오게 되고, 여기서 클릭할 경우 카페메뉴화면으로 이동하게 된다.
  * [인트로 및 맵뷰 메뉴확인.mp4](https://github.com/KIMTHE/KNU-CapstoneDesign-20212-Team10/blob/main/document/%EC%8B%9C%EC%97%B0%EC%98%81%EC%83%81/%EC%9D%B8%ED%8A%B8%EB%A1%9C%20%EB%B0%8F%20%EB%A7%B5%EB%B7%B0%20%EB%A9%94%EB%89%B4%ED%99%95%EC%9D%B8.mp4)

|지도 화면|
|:-----:|
|<img height=400 src="https://user-images.githubusercontent.com/29460783/149613093-217370fc-cf61-4e9c-850c-2fab75c05228.png">|
</br>


#### 2️⃣카페의 메뉴를 확인 할 수 있다.
   * 상세정보 url에 ‘/main/v’을 붙이면, 카페의 json데이터가 포함된 url이 완성되며, 이 url에서 get을 한 후, JSONObject 라이브러리를 이용하여 json객체를 만든 후, 메뉴이름,가격,이미지 url이 포함된 객체의 배열을 반환한다.
   * 반환된 배열을 이용하여, 커스텀 어댑터(CafeMenuAdapter)를 설정해서 리스트 뷰에 메뉴정보 리스트를 보여준다.

|메뉴 화면|
|:-----:|
|<img height=400 src="https://user-images.githubusercontent.com/29460783/149613129-b7560c3d-f22a-41bc-9eed-f6f3dcedb691.png">|
</br>


#### 3️⃣촬영으로 카페 wifi를 자동으로 연결할 수 있다.
   * 카메라 버튼을 클릭한 후, wifi ID,PW가 보이도록 안내문을 촬영하면, 촬영된 사진이 임시파일로 저장되고 비트맵으로 가공된다.
   * 구글 클라우드 서버로부터 인식 결과를 받으면, 로그인 유틸리티 함수에서 ‘ID,id,pw,PW,아이디,비밀번호’ 등의 키워드로 wifi의 ID,PW을 추출한다.
   * 위의 과정에서 골라낸 WIFI의 ID, PW 정보를 이용하여 wifiManager를 통해 WIFI연결을 시도한다.
   * ID, PW 정보를 이용하여 WIFI 연결을 시도할 때 카페이름, ID, PW의 텍스트가 적혀있는 다이얼로그를 표시하여, 해당 정보가 올바른지 확인하고, firebase에 저장한다.
   * [ocr wifi 연결.mp4](https://github.com/KIMTHE/KNU-CapstoneDesign-20212-Team10/blob/main/document/%EC%8B%9C%EC%97%B0%EC%98%81%EC%83%81/%EC%99%80%EC%9D%B4%ED%8C%8C%EC%9D%B4%20%EC%97%B0%EA%B2%B0.mp4)

|ocr wifi 화면|
|:-----:|
|<img height=400 src="https://user-images.githubusercontent.com/29460783/149613159-caecc815-fbe3-482d-ad66-e83d19cfc4df.png">|
</br>


#### 4️⃣다른 사용자가 등록한 카페의 wifi정보를 이용하여 쉽게 연결할 수 있다.
   * firebase에 해당 카페의 URL을 이용하여 해당하는 wifi 데이터가 있는지 확인한다. 만약 해당 데이터가 있다면, WIFI연결하기 버튼이 보이게 된다.
   * 버튼을 클릭하면 파이어베이스에서 해당 카페 와이파이의 ID, PW를 가져와서 바로 연결할 수 있다. 
   * [firebase wifi 연결.mp4](https://github.com/KIMTHE/KNU-CapstoneDesign-20212-Team10/blob/main/document/%EC%8B%9C%EC%97%B0%EC%98%81%EC%83%81/%ED%8C%8C%EC%9D%B4%EC%96%B4%EB%B2%A0%EC%9D%B4%EC%8A%A4%20%EC%99%80%EC%9D%B4%ED%8C%8C%EC%9D%B4%20%EC%97%B0%EA%B2%B0.mp4)

|firebase wifi 화면|
|:-----:|
|<img height=100 src="https://user-images.githubusercontent.com/29460783/149613169-1c5114c3-5d22-43f4-9309-259356cdf916.png">|
<br/>


## 👩‍💻기술스택

![image](https://user-images.githubusercontent.com/29460783/149612935-51580beb-773c-4924-98de-03b1164d4453.png)

<br/>

## 🖥프로젝트 결과

- [playstore 링크](https://play.google.com/store/apps/details?id=com.jongsip.cafe)

<br/>

## 📽발표영상

[![Video Label](http://img.youtube.com/vi/O7a8P42V19o/0.jpg)](https://www.youtube.com/watch?v=O7a8P42V19o&ab_channel=KIMms)
