# Dockerfile

# 1. Python 환경 설정 (3.10 이상 권장)
FROM python:3.12-slim

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 환경 변수 설정
ENV PYTHONUNBUFFERED True

# 4. 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 5. Flask 앱 파일 복사
COPY app.py .

# 6. 서버 실행 명령어 (Gunicorn 사용)
# Cloud Run은 PORT 환경 변수를 제공합니다.
CMD exec gunicorn --bind :$PORT --workers 1 --threads 8 app:app