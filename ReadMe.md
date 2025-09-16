
<img src="https://github.com/ElliottStarosta/2025_Java_Code_Review_ICS4U/blob/master/src/main/resources/static/images/icon-full.png?raw=true" alt="NovaVet Logo" width="400"/>


## Table of Contents

* [Introduction](#introduction)
* [Features](#features)

  * [Intelligent Chat Interface](#intelligent-chat-interface)
  * [Visual Symptom Analysis](#visual-symptom-analysis)
  * [Emergency Features](#emergency-features)
  * [Smart Conversation Management](#smart-conversation-management)
* [Technology Stack](#technology-stack)
* [Prerequisites](#prerequisites)
* [How to Run the Application](#how-to-run-the-application)
* [Python VQA Server Setup Details](#python-vqa-server-setup-details)
* [Demo Video](#demo-video)
* [Project Structure](#project-structure)
* [Configuration](#configuration)
* [Troubleshooting](#troubleshooting)
* [Support](#support)
* [License](#license)
* [API Endpoints](#api-endpoints)
* [Database Schema](#database-schema)

---

## Introduction

**Virtual Vet** is an intelligent, AI-powered veterinary assistant that provides immediate pet healthcare guidance, emergency triage, and visual symptom analysis. Imagine having a veterinary expert in your pocket, available 24/7 to help you make informed decisions about your pet's health.

---

## Features

### Intelligent Chat Interface

* **Natural conversations** with an AI veterinary assistant
* **Typing indicators** and smooth animations for realistic interactions
* **Message editing & deletion** with conversation context maintenance
* **Multi-message responses** with proper formatting for medical information

### Visual Symptom Analysis

* **Upload pet injury/condition photos** for AI analysis
* **Multiple image support** with preview thumbnails
* **Instant analysis** of conditions, confidence levels, and urgency ratings
* **BLIP VQA model integration** for accurate visual question answering

### Emergency Features

* **Automatic location detection** for emergency services
* **Nearby veterinary clinic finder** with real-time mapping
* **Emergency triage guidance** for critical situations
* **One-touch emergency access** when seconds matter

### Smart Conversation Management

* **Session persistence** across conversations
* **Context-aware responses** based on conversation history
* **Structured medical formatting** with bullet points and organized information
* **Professional medical terminology** with clear explanations

---

## Technology Stack

* **Backend**: Java Spring Boot with Maven
* **Frontend**: Vaadin Flow for modern web UI
* **AI/ML**: Salesforce BLIP VQA model for image analysis (Python)
* **Geolocation**: HTML5 Geolocation API
* **Storage**: Local file system for uploads and model caching
* **Database**: H2 in-memory database (for development)

---

## Prerequisites

Before running the application, ensure you have:

* **Java JDK 17+** installed
* **Maven 3.6+** installed
* **Python 3.8+** with pip (for the VQA server component)
* **At least 4GB RAM** available for the AI models
* **Git** for cloning the repository

---

## How to Run the Application

### 1. Clone and Navigate to Project

```bash
git clone <your-repo-url>
cd vetbot
```

### 2. Set Up Python Environment

```bash
cd path/to/python/vqa/server
python -m venv venv

# Activate the virtual environment
# On Windows:
venv\Scripts\activate
# On macOS/Linux:
source venv/bin/activate

pip install -r requirements.txt
```

### 3. Run the Spring Boot Application

```bash
cd /path/to/vetbot
mvn spring-boot:run
```

### 4. Access the Application

```
http://localhost:8080
```

### 5. (Optional) Manual Start of Python VQA Server

```bash
cd path/to/python/vqa/server

# Activate venv if not already active
venv\Scripts\activate  # Windows
source venv/bin/activate  # macOS/Linux

python main.py
```

### 6. Access Database Console (Development)

```
http://localhost:8080/h2-console
```

* JDBC URL: `jdbc:h2:mem:vetchat`
* Username: `sa`
* Password: (leave blank)

---

## Python VQA Server Setup Details

### Required Python Packages

```txt
torch>=1.9.0
torchvision>=0.10.0
transformers>=4.12.0
Pillow>=8.3.0
flask>=2.0.0
flask-cors>=3.0.0
numpy>=1.21.0
requests>=2.25.0
```

### Installation Troubleshooting

```bash
# CUDA/GPU Support (optional)
pip install torch torchvision --extra-index-url https://download.pytorch.org/whl/cu113

# Permission errors
pip install --user -r requirements.txt

# Conflicts
pip install --ignore-installed -r requirements.txt

# Memory issues
pip --no-cache-dir install -r requirements.txt
```

Verify installation:

```bash
python -c "import torch; import transformers; import flask; print('All imports successful!')"
```

---

## Demo Video

![Image](https://github.com/user-attachments/assets/36c84d4f-a08d-4686-9178-d1a93682f772)---

## Project Structure

```
vetbot/
├── src/main/java/com/virtualvet/
│   ├── config/          # Application configuration
│   ├── controller/      # REST API controllers
│   ├── service/         # Business logic services
│   ├── model/           # Data models and entities
│   ├── repository/      # Data access layer
│   ├── util/            # Utility classes
│   └── view/            # Vaadin UI components
├── python_server/       # Python VQA server (if separate)
│   ├── main.py          # Flask server implementation
│   ├── requirements.txt # Python dependencies
│   └── models/          # AI model files
├── src/main/resources/ 
│   ├── application.properties  # App config
│   └── static/images/   # Static assets
├── uploads/             # User-uploaded images
├── model_cache/         # Cached AI models
└── target/              # Build artifacts
```

---

## Configuration

```properties
server.port=8080
spring.datasource.url=jdbc:h2:mem:vetchat
spring.datasource.username=sa
spring.datasource.password=

vqa.python.script.path=path/to/python/script.py
vqa.server.port=5000
vqa.server.host=127.0.0.1

spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## Troubleshooting

### Common Issues

1. **Python Not Found**: Ensure Python is in PATH
2. **Pip Not Found**: Install pip with `python -m ensurepip --upgrade`
3. **Port in Use**: Change `server.port` in `application.properties`
4. **VQA server not starting**: Check Python and dependencies
5. **Image uploads failing**: Ensure `uploads/` directory has write permissions
6. **Location blocked**: Allow browser location access
7. **H2 issues**: Verify JDBC URL `jdbc:h2:mem:vetchat`
8. **Package conflicts**: Use virtual environments

Logs are available in console output.

---

## Support

For issues:

1. Check troubleshooting
2. Review logs
3. Verify prerequisites
4. Ensure Python VQA server is running
5. Verify dependencies

---

## API Endpoints

### Chat

* `POST /api/chat/start`
* `POST /api/chat/message`
* `GET  /api/chat/history/{sessionId}`
* `GET  /api/chat/health`

### Animal Profile

* `GET  /api/profile/{sessionId}`
* `PUT  /api/profile/{sessionId}`

### Image Analysis

* `POST /api/analysis/image`
* `POST /api/analysis/validate`

### Emergency Services

* `POST /api/emergency/nearby-vets`
* `GET  /api/emergency/health`
* `GET  /api/emergency/test`

---

## License

This project is proprietary software. All rights reserved.

**Virtual Vet** — Making pet healthcare accessible and stress-free, one conversation at a time.

