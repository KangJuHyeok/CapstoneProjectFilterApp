<h1>📔 Film Diary: 감성 필름 ,스토리 기록, AI 필름 앱</h1>

<h3>🌟 프로젝트 소개 (Introduction)</h3
</br>
Film Diary는 필름을 사용하여 기존 이미지에 적용 혹은 실시간 카메라 촬영을 할 수 있는 모바일 애플리케이션 입니다. 또한 사용자의 일상 기록(일기)을 기반으로 AI(Gemini)가 최적의 필름 필터를 추천해주는 기능과 프롬프트를 이용하여 AI에게 필름 필터를 요청할 수 있는 기능이 있습니다. [CGE(C/C++ Native) 라이브러리를 필터링과 Google Gemini API를 통한 지능형 추천 시스템을 통합하여 개발되었습니다.]
</br>
</br>

<h2>🚀 주요 기능 (Key Features)</h2>
</br>
<h3>1. 🧠 AI 지능형 필터 추천 시스템</h3>
</br>
일기 감성 분석: 사용자가 작성한 일기 내용을 Gemini 2.5 Flash LLM이 분석하여, 글의 분위기에 가장 적합한 필터 1개를 구조화된 JSON 응답 형태로 추출하여 추천합니다.
</br>
</br>

<h3>2. ✨ 프롬프트 기반 필터 생성 (AI Image Editing)</h3>
</br>
나만의 필름 주문: 사용자 사진과 텍스트 프롬프트(예: "90년대의 따뜻한 빛 필터")를 입력하면, Gemini 2.5 Flash Image (nano-banana) 모델이 해당 프롬프트에 맞는 AI 필터 효과를 사진에 직접 적용하여 결과물을 생성합니다.
</br>
</br>

<h3>3. 📸  로컬 이미지 필름 처리</h3>
</br>
CGE 로컬 필터링: CGE 라이브러리를 활용하여 20가지 이상의 필터를 사진에 적용할 수 있습니다.
</br>
</br>

<h3>4. 📝 스토리 및 갤러리 관리</h3>
</br>
스토리 피드: 날짜별 검색 및 최신순 정렬이 가능한 일기 피드를 제공 및 작성할 수 있습니다.
</br>
이미지 갤러리: SQLite DB를 활용하여 촬영 또는 외부에서 추가된 필터링된 사진을 앱 내에서 관리합니다.
</br>
</br>

<h2>🛠️ 기술 스택 (Tech Stack & Architecture)</h2>
</br>
<img width="971" height="488" alt="image" src="https://github.com/user-attachments/assets/7eb750bc-5fd3-4de5-a60f-14aa6ba51298" />

<h5>주요 API, 라이브러리</h5>
<a href="https://docs.cloud.google.com/vertex-ai/generative-ai/docs/models/gemini/2-5-flash?hl=ko">LLM (gemini 2.5 flash)</a> 
</br>
<a href="https://github.com/wysaid/android-gpuimage-plus">Filter : https://github.com/wysaid/android-gpuimage-plus</a>
</br>
<a href="https://github.com/natario1/CameraView">Camera : https://github.com/natario1/CameraView</a>
