# Novavet - AI-Powered Veterinary Assistant

![NovaVet Logo](https://github.com/ElliottStarosta/2025_Java_Code_Review_ICS4U/blob/master/src/main/resources/static/images/icon-full.png?raw=true)
*[Space for your logo/video demo]*

## Introduction

Virtual Vet is an intelligent, AI-powered veterinary assistant that provides immediate pet healthcare guidance, emergency triage, and visual symptom analysis. Imagine having a veterinary expert in your pocket, available 24/7 to help you make informed decisions about your pet's health.



## Features

### Intelligent Chat Interface
- **Natural conversations** with an AI veterinary assistant
- **Typing indicators** and smooth animations for realistic interactions
- **Message editing & deletion** with conversation context maintenance
- **Multi-message responses** with proper formatting for medical information

### Visual Symptom Analysis
- **Upload pet injury/condition photos** for AI analysis
- **Multiple image support** with preview thumbnails
- **Instant analysis** of conditions, confidence levels, and urgency ratings
- **BLIP VQA model integration** for accurate visual question answering

### Emergency Features
- **Automatic location detection** for emergency services
- **Nearby veterinary clinic finder** with real-time mapping
- **Emergency triage guidance** for critical situations
- **One-touch emergency access** when seconds matter

### Smart Conversation Management
- **Session persistence** across conversations
- **Context-aware responses** based on conversation history
- **Structured medical formatting** with bullet points and organized information
- **Professional medical terminology** with clear explanations



## Technology Stack

- **Backend**: Java Spring Boot with Maven
- **Frontend**: Vaadin Flow for modern web UI
- **AI/ML**: Salesforce BLIP VQA model for image analysis (Python)
- **Geolocation**: HTML5 Geolocation API
- **Storage**: Local file system for uploads and model caching
- **Database**: H2 in-memory database (for development)



## Prerequisites

Before running the application, ensure you have:

- **Java JDK 17+** installed
- **Maven 3.6+** installed
- **Python 3.8+** with pip (for the VQA server component)
- **At least 4GB RAM** available for the AI models
- **Git** for cloning the repository



## How to Run the Application

### 1. Clone and Navigate to Project
```bash
git clone <your-repo-url>
cd vetbot
```

### 2. Set Up Python Environment
```bash
# Navigate to the Python VQA server directory
cd path/to/python/vqa/server

# Create a virtual environment (recommended)
python -m venv venv

# Activate the virtual environment
# On Windows:
venv\Scripts\activate
# On macOS/Linux:
source venv/bin/activate

# Install required Python dependencies
pip install -r requirements.txt
```

### 3. Run the Spring Boot Application
```bash
# Return to the main project directory
cd /path/to/vetbot

# Start the Spring Boot application
mvn spring-boot:run
```

### 4. Access the Application
Open your browser and navigate to:
```
http://localhost:8080
```

### 5. (Optional) Manual Start of Python VQA Server
If the automatic startup fails, you can manually start the Python server:
```bash
# Navigate to the Python server directory
cd path/to/python/vqa/server

# Activate the virtual environment if not already active
# On Windows:
venv\Scripts\activate
# On macOS/Linux:
source venv/bin/activate

# Start the Python VQA server
python main.py
```

### 6. Access Database Console (Development)
For development purposes, you can access the H2 database console at:
```
http://localhost:8080/h2-console
```
- JDBC URL: `jdbc:h2:mem:vetchat`
- Username: `sa`
- Password: (leave blank)



## Python VQA Server Setup Details

### Required Python Packages
The Python VQA server requires the following packages (typically in requirements.txt):

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

If you encounter issues with the Python dependencies:

1. **CUDA/GPU Support** (Optional for better performance):
```bash
# If you have an NVIDIA GPU, install CUDA-enabled PyTorch
pip install torch torchvision --extra-index-url https://download.pytorch.org/whl/cu113
```

2. **Common Installation Issues**:
```bash
# If you get permission errors, try:
pip install --user -r requirements.txt

# If you have conflicting packages, try:
pip install --ignore-installed -r requirements.txt

# For memory issues during installation:
pip --no-cache-dir install -r requirements.txt
```

3. **Verifying Installation**:
```bash
# Check if all packages were installed correctly
python -c "import torch; import transformers; import flask; print('All imports successful!')"
```



## Demo Video

[![Nova Demo](https://via.placeholder.com/800x450/2563eb/ffffff?text=Click+to+Watch+Demo+Video)](https://your-demo-video-link-here)
*Insert your screen recording or demo video here*



## Project Structure

```
vetbot/
├── src/main/java/com/virtualvet/
│   ├── config/          # Application configuration
│   ├── controller/      # REST API controllers
│   ├── service/         # Business logic services
│   ├── model/          # Data models and entities
│   ├── repository/      # Data access layer
│   ├── util/           # Utility classes
│   └── view/           # Vaadin UI components
├── python_server/       # Python VQA server (if separate)
│   ├── main.py         # Flask server implementation
│   ├── requirements.txt # Python dependencies
│   └── models/         # AI model files
├── src/main/resources/ 
│   ├── application.properties  # Application configuration
│   └── static/images/   # Static assets (icons, etc.)
├── uploads/            # User-uploaded images
├── model_cache/        # Cached AI models
└── target/            # Compiled classes and build artifacts
```



## Configuration

The application uses the following configuration (in `application.properties`):

```properties
# Server port
server.port=8080

# Database configuration (H2 in-memory)
spring.datasource.url=jdbc:h2:mem:vetchat
spring.datasource.username=sa
spring.datasource.password=

# VQA Python server configuration
vqa.python.script.path=path/to/python/script.py
vqa.server.port=5000
vqa.server.host=127.0.0.1

# File upload settings
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```



## Troubleshooting

### Common Issues:

1. **Python Not Found**: Ensure Python is in your PATH environment variable
2. **Pip Not Found**: Install pip with `python -m ensurepip --upgrade`
3. **Port already in use**: Change `server.port` in `application.properties`
4. **VQA server not starting**: Check Python installation and dependencies
5. **Image uploads failing**: Verify `uploads/` directory has write permissions
6. **Location services blocked**: Allow browser location access for emergency features
7. **H2 database connection issues**: Verify the JDBC URL is `jdbc:h2:mem:vetchat`
8. **Python package conflicts**: Use virtual environments to isolate dependencies

### Logs:
Check the console output for detailed error messages and server status. The application provides detailed startup logs and progress indicators.



## Support

For issues and questions:
1. Check the troubleshooting section above
2. Review console logs for error messages
3. Ensure all prerequisites are installed correctly
4. Verify the Python VQA server is running if using image analysis
5. Check that all Python dependencies are installed correctly


## License

This project is proprietary software. All rights reserved.



**Virtual Vet** - Making pet healthcare accessible and stress-free, one conversation at a time.


## API Endpoints

### Chat
- `POST /api/chat/start` → Start a new chat session
- `POST /api/chat/message` → Send a message (with optional images) to the AI assistant
- `GET  /api/chat/history/{sessionId}` → Retrieve chat history for a session
- `GET  /api/chat/health` → Health check for the chat service

### Animal Profile
- `GET  /api/profile/{sessionId}` → Get the pet profile for a session
- `PUT  /api/profile/{sessionId}` → Update the pet profile for a session

### Image Analysis
- `POST /api/analysis/image` → Analyze a pet image for conditions/symptoms
- `POST /api/analysis/validate` → Validate image format and metadata

### Emergency Services
- `POST /api/emergency/nearby-vets` → Find nearby emergency veterinary clinics
- `GET  /api/emergency/health` → Health check for emergency service
- `GET  /api/emergency/test` → Test endpoint for emergency controller



## Database Schema

The application uses an H2 in-memory database with the following main entities:
- Conversation sessions
- Message history
- User uploads
- Analysis results
