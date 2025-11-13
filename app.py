import os
import base64
import json
import time
from flask import Flask, request, jsonify
from google import genai
from google.genai import types
from io import BytesIO
from PIL import Image as PILImage # PIL.Image를 PILImage로 임포트하여 혼동을 줄임
from google.genai.errors import APIError
# API 응답에서 Base64 이미지 데이터를 추출하기 위해 google.genai.types.Part 사용
from google.genai.types import Part

app = Flask(__name__)

# --------------------------------------------------------------------
# 1️⃣ API 설정 
# --------------------------------------------------------------------
PROJECT_ID = "myfilterapp2025" 
LOCATION = "us-central1"

# --------------------------------------------------------------------
# 2️⃣ 클라이언트 초기화 및 프롬프트 처리 함수
# --------------------------------------------------------------------
gemini_client = None
IMAGEN_INIT_SUCCESS = False 

try:
    # 1. Gemini 클라이언트 초기화

    GEMINI_API_KEY_SERVER = os.environ.get("GEMINI_API_KEY") 
    gemini_client = genai.Client(api_key=GEMINI_API_KEY_SERVER)
    print("✅ Gemini LLM 클라이언트 초기화 성공")
    
    # Gemini 클라이언트가 성공적으로 초기화되면 Imagen Tooling (gemini-2.5-flash-image) 사용 가능하다고 간주
    IMAGEN_INIT_SUCCESS = True

except Exception as e:
    print(f"🚨 클라이언트 초기화 중 치명적인 오류: {e}")


# --------------------------------------------------------------------
# 3. 이미지 필터 생성 엔드포인트 구현 (Gemini 2.5 Flash Image 모델 사용)
# --------------------------------------------------------------------

@app.route('/generate_filter_image', methods=['POST'])
def generate_filter_image():
    #모델명을 gemini-2.5-flash-image로 변경 (Nano Banana 모델)
    IMAGE_MODEL_NAME = 'gemini-2.5-flash-image' 
    
    if not IMAGEN_INIT_SUCCESS: 
        return jsonify({'error': 'Gemini 클라이언트가 초기화되지 않았습니다.'}), 503

    # 1. 입력 데이터 (Multipart Form Data) 처리
    if 'image' not in request.files:
        return jsonify({"error": "이미지 파일 ('image')이 필요합니다."}), 400
        
    uploaded_file = request.files['image']
    prompt = request.form.get('prompt', '') 
    
    if not prompt:
        # 프롬프트가 없을 경우 기본 텍스트 사용
        prompt = "사진의 분위기를 더욱 풍부하게 만드는 감성적인 필터를 적용해줘."

    try:
        # 2. 이미지 전처리: PIL Image 객체 생성
        image_bytes = uploaded_file.read()
        original_pil = PILImage.open(BytesIO(image_bytes))
        
        # 이미지 크기 조정 (선택 사항: 1024x1024로 고정하여 API 처리 효율 증대)
        MAX_SIZE = 1024
        original_pil.thumbnail((MAX_SIZE, MAX_SIZE))
        
        # 3. Gemini 모델이 사용할 이미지 Part 객체 생성 (PIL 객체 직접 전달)
        # Gemini API는 PIL Image 객체를 contents 리스트에 직접 전달하여 처리
        image_part = original_pil
        
        
        # 4. Gemini에게 필터링을 지시하는 프롬프트 구성 
        final_prompt_for_gemini = (
            f"이 이미지를 참조하여, 다음 스타일과 색감으로 사진 필터를 적용해줘: '{prompt}'. "
            f"**절대 사진의 구성, 피사체, 기존 요소는 변경하지 말고** 색상, 조명, 질감만 수정하여 요청한 필터 효과를 구현해야 해. "
            f"결과는 Base64 인코딩된 이미지 하나로 반환해줘."
        )
        
        # 5. Gemini API 호출 (이미지 Part와 텍스트 프롬프트를 함께 전달)
        print(f"📸 {IMAGE_MODEL_NAME} 호출 중 (프롬프트: {prompt[:50]}...)")
        
        # 모델 이름을 gemini-2.5-flash-image로 변경하여 이미지 생성 기능을 사용
        response = gemini_client.models.generate_content(
            model=IMAGE_MODEL_NAME, 
            contents=[image_part, final_prompt_for_gemini], 
        )
        
        # 6. 응답 처리 및 이미지 추출
        filtered_image_data_base64 = None
        
        # 이미지 생성 모델의 응답은 보통 첫 번째 Part에 Base64 이미지 데이터가 포함됨
        if response.candidates and response.candidates[0].content and response.candidates[0].content.parts:
            # 첫 번째 Part를 확인
            generated_part: Part = response.candidates[0].content.parts[0]
            
            # Base64 인라인 데이터가 있는지 확인
            if generated_part.inline_data:
                
                raw_data = generated_part.inline_data.data
                
                if isinstance(raw_data, bytes):
                    # 데이터가 바이트(bytes)인 경우, Base64로 인코딩하고 문자열로 변환합니다.
                    filtered_image_data_base64 = base64.b64encode(raw_data).decode('utf-8')
                elif isinstance(raw_data, str):
                    # 이미 문자열인 경우 그대로 사용합니다.
                    filtered_image_data_base64 = raw_data
                else:
                    raise TypeError("반환된 데이터가 bytes 또는 str 타입이 아닙니다.")


        if not filtered_image_data_base64:
            # 이미지 데이터가 없을 경우 텍스트 응답을 출력하여 디버깅에 활용
            print(f"🚨 {IMAGE_MODEL_NAME}에서 이미지 데이터를 찾을 수 없습니다. 응답 텍스트: {response.text[:100]}...")
            return jsonify({"error": f"이미지 생성 실패: 모델이 이미지를 반환하지 않았습니다. 응답: {response.text[:50]}"}), 500

        
        # 7. Base64 인코딩된 이미지 데이터 직접 반환 (안드로이드 클라이언트 요구 사항)
        return jsonify({
            "status": "success",
            "prompt": final_prompt_for_gemini,
            "filtered_image_data": filtered_image_data_base64 # Base64 데이터 직접 반환
        })
            
    except APIError as e:
        print(f"🚨 Gemini API 호출 오류: {e}")
        return jsonify({"error": f"Gemini API 호출 실패: {e.message}"}), 500
    except Exception as e:
        print(f"🚨 이미지 편집 오류 발생: {e}")
        return jsonify({"error": f"이미지 편집 최종 실패: {e}", "details": str(e)}), 500


# --------------------------------------------------------------------
# 4. 일기 필터 추천 엔드포인트 구현 (Gemini LLM 사용)
# --------------------------------------------------------------------
# 이 부분은 변경 없이 그대로 유지합니다.
@app.route('/analyze_filter_recommendation', methods=['POST'])
def analyze_filter_image():
    if not gemini_client:
        return jsonify({'error': 'Gemini LLM 클라이언트가 설정되지 않아 추천 기능을 사용할 수 없습니다.'}), 500
        
    try:
        data = request.json
        text = data.get('text', '')
        filter_list = data.get('filter_list', [])

        filter_options = "\n".join([
            f"- 이름: {f.get('filterName')} / 설명: {f.get('filterDescription')} / 타입: {f.get('filterType')}"
            for f in filter_list
        ])

        full_prompt = f"""
        당신은 일기 내용에 가장 적합한 필터를 추천하는 감성 필터 전문가입니다.
        [일기 내용]: {text}
        [추천 가능한 필터 목록]: {filter_options}
        [요청]: 위 일기 내용의 분위기를 가장 잘 표현하는 필터 하나를 선택하고,
        그 필터의 '이름'과 '타입'을 다음 JSON 형식으로만 반환해 주세요.
        JSON 형식: {{"recommendedFilterName": "선택된 필터 이름", "recommendedFilterType": "선택된 필터 타입"}}
        """

        response = gemini_client.models.generate_content(
            model='gemini-2.5-flash', # Gemini 2.5 Flash 모델 사용
            contents=full_prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=types.Schema(
                    type=types.Type.OBJECT,
                    properties={
                        "recommendedFilterName": types.Schema(type=types.Type.STRING),
                        "recommendedFilterType": types.Schema(type=types.Type.STRING)
                    },
                    required=["recommendedFilterName", "recommendedFilterType"]
                )
            )
        )
        
        return jsonify(json.loads(response.text))

    except Exception as e:
        print(f"🚨 오류 발생: {e}")
        return jsonify({'error': f'서버 처리 중 오류 발생: {e}'}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)