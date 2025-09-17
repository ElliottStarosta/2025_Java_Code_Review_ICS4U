# VetBot - Virtual Veterinary Assistant Architecture Diagram

This comprehensive Mermaid diagram shows the complete architecture of the VetBot application, including all components, their relationships, and data flow.

```mermaid
graph TB
    %% External Systems
    subgraph "External Systems"
        HF[🤖 Hugging Face API<br/>Image Classification]
        HC[HackClub AI API<br/>GPT-4o-mini]
        DB[(🗄️ H2 Database<br/>Conversations & Messages)]
        FS[📁 File System<br/>Image Storage]
    end

    %% Python VQA Service
    subgraph "Python VQA Service"
        PY[🐍 main.py<br/>Flask VQA Server]
        BLIP[🧠 BLIP VQA Model<br/>Salesforce/blip-vqa-base]
        OPT[⚡ OptimizedVeterinaryVQAService<br/>Image Analysis Engine]
        
        PY --> BLIP
        PY --> OPT
        BLIP --> OPT
    end

    %% Frontend Layer
    subgraph "Frontend Layer"
        WEB[🌐 Vaadin Web UI<br/>ChatView & MainLayout]
        HTML[📄 Static Resources<br/>CSS, Images, HTML]
        
        WEB --> HTML
    end

    %% Spring Boot Application
    subgraph "Spring Boot Application"
        %% Main Application
        APP[🚀 VetChatApplication<br/>Main Spring Boot App]
        PYMANAGER[🐍 PythonVQAServerManager<br/>Process Management]
        HEALTH[💓 VQAServerHealthMonitor<br/>Health Monitoring]
        
        APP --> PYMANAGER
        APP --> HEALTH
        
        %% Configuration Layer
        subgraph "Configuration Layer"
            AICONFIG[⚙️ AIServiceConfig<br/>AI Service Settings]
            DBCONFIG[⚙️ DatabaseConfig<br/>Database Configuration]
            APPCONFIG[⚙️ ApplicationConfig<br/>App Settings]
            SHELLCONFIG[⚙️ AppShellConfig<br/>Vaadin Shell Config]
        end
        
        %% Controller Layer
        subgraph "Controller Layer"
            CHATCTRL[🎯 ChatController<br/>Chat Operations]
            IMAGECTRL[🎯 ImageAnalysisController<br/>Image Processing]
            ANIMALCTRL[🎯 AnimalProfileController<br/>Profile Management]
            EMERGENCYCTRL[🎯 EmergencyController<br/>Emergency Handling]
        end
        
        %% Service Layer
        subgraph "Service Layer"
            CHATSVC[💬 ChatService<br/>Conversation Orchestration]
            AISVC[🤖 AIConversationService<br/>AI Response Generation]
            IMGSVC[🖼️ ImageAnalysisService<br/>Visual Analysis]
            ANIMALSVC[🐕 AnimalProfileService<br/>Profile Management]
            EMERGENCYSVC[🚨 EmergencyService<br/>Emergency Handling]
            CLEANUPSVC[🧹 SessionCleanupService<br/>Session Management]
        end
        
        %% Repository Layer
        subgraph "Repository Layer"
            CONVREPO[📚 ConversationRepository<br/>Conversation Data Access]
            MSGREPO[📚 MessageRepository<br/>Message Data Access]
            ANIMALREPO[📚 AnimalProfileRepository<br/>Profile Data Access]
        end
        
        %% Entity Layer
        subgraph "Entity Layer"
            CONV[💾 Conversation<br/>Session Management]
            MSG[💾 Message<br/>Chat Messages]
            ANIMAL[💾 AnimalProfile<br/>Pet Information]
            MSGIMG[💾 MessageImage<br/>Image Attachments]
        end
        
        %% DTO Layer
        subgraph "DTO Layer"
            CHATREQ[📦 ChatRequest<br/>User Input]
            CHATRESP[📦 ChatResponse<br/>AI Response]
            STRUCTRESP[📦 StructuredVetResponse<br/>Detailed Response]
            MSGDTO[📦 MessageDto<br/>Message Data]
            ANIMALDTO[📦 AnimalProfileDto<br/>Profile Data]
            SESSIONSTART[📦 SessionStartResponse<br/>Session Creation]
            HISTORYRESP[📦 ConversationHistoryResponse<br/>Chat History]
        end
        
        %% Model Layer
        subgraph "Model Layer"
            ANALYSIS[🔍 AnalysisResult<br/>Image Analysis Data]
            CONTEXT[🧠 ConversationContext<br/>Chat Context]
            VETLOC[📍 VetLocation<br/>Location Data]
        end
        
        %% Enum Layer
        subgraph "Enum Layer"
            MSGTYPE[📋 MessageType<br/>USER | BOT]
            URGENCY[📋 UrgencyLevel<br/>LOW | MEDIUM | HIGH | CRITICAL]
        end
        
        %% Utility Layer
        subgraph "Utility Layer"
            APICLIENT[🔧 ApiClient<br/>HTTP Requests]
            FILEUTILS[🔧 FileUtils<br/>File Operations]
            VALIDATION[🔧 ValidationUtils<br/>Input Validation]
        end
        
        %% Exception Layer
        subgraph "Exception Layer"
            EXCEPTION[⚠️ GlobalExceptionHandler<br/>Error Handling]
        end
        
        %% View Layer
        subgraph "View Layer"
            MAINVIEW[🖥️ MainLayout<br/>Application Shell]
            CHATVIEW[🖥️ ChatView<br/>Chat Interface]
        end
    end

    %% Data Flow Connections
    %% Frontend to Controllers
    WEB --> CHATCTRL
    WEB --> IMAGECTRL
    WEB --> ANIMALCTRL
    WEB --> EMERGENCYCTRL
    
    %% Controllers to Services
    CHATCTRL --> CHATSVC
    IMAGECTRL --> IMGSVC
    ANIMALCTRL --> ANIMALSVC
    EMERGENCYCTRL --> EMERGENCYSVC
    
    %% Service Dependencies
    CHATSVC --> AISVC
    CHATSVC --> IMGSVC
    CHATSVC --> ANIMALSVC
    CHATSVC --> EMERGENCYSVC
    CHATSVC --> CONVREPO
    CHATSVC --> MSGREPO
    CHATSVC --> ANIMALREPO
    
    AISVC --> AICONFIG
    AISVC --> APICLIENT
    
    IMGSVC --> AICONFIG
    IMGSVC --> APICLIENT
    IMGSVC --> FILEUTILS
    
    %% Repository to Entity
    CONVREPO --> CONV
    MSGREPO --> MSG
    ANIMALREPO --> ANIMAL
    
    %% Entity Relationships
    CONV --> MSG
    CONV --> ANIMAL
    MSG --> MSGIMG
    
    %% Service to Model
    CHATSVC --> CONTEXT
    IMGSVC --> ANALYSIS
    
    %% Entity to Enum
    MSG --> MSGTYPE
    CONV --> URGENCY
    ANALYSIS --> URGENCY
    
    %% Service to DTO
    CHATSVC --> CHATREQ
    CHATSVC --> CHATRESP
    CHATSVC --> STRUCTRESP
    CHATSVC --> MSGDTO
    CHATSVC --> ANIMALDTO
    CHATSVC --> SESSIONSTART
    CHATSVC --> HISTORYRESP
    
    %% External API Connections
    AISVC --> HC
    IMGSVC --> HF
    IMGSVC --> PY
    
    %% Database Connections
    CONVREPO --> DB
    MSGREPO --> DB
    ANIMALREPO --> DB
    
    %% File System Connections
    IMGSVC --> FS
    
    %% Configuration Dependencies
    APP --> AICONFIG
    APP --> DBCONFIG
    APP --> APPCONFIG
    APP --> SHELLCONFIG
    
    %% Exception Handling
    EXCEPTION --> CHATCTRL
    EXCEPTION --> IMAGECTRL
    EXCEPTION --> ANIMALCTRL
    EXCEPTION --> EMERGENCYCTRL
    
    %% Utility Usage
    CHATSVC --> VALIDATION
    IMGSVC --> VALIDATION
    
    %% View Connections
    WEB --> MAINVIEW
    WEB --> CHATVIEW
    
    %% Python Service Management
    PYMANAGER --> PY
    HEALTH --> PY

    %% Styling
    classDef external fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef python fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef frontend fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef spring fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef config fill:#fff8e1,stroke:#f57f17,stroke-width:2px
    classDef controller fill:#e3f2fd,stroke:#0277bd,stroke-width:2px
    classDef service fill:#f1f8e9,stroke:#33691e,stroke-width:2px
    classDef repository fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    classDef entity fill:#f9fbe7,stroke:#827717,stroke-width:2px
    classDef dto fill:#e0f2f1,stroke:#00695c,stroke-width:2px
    classDef model fill:#fff3e0,stroke:#ef6c00,stroke-width:2px
    classDef enum fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef utility fill:#e8eaf6,stroke:#283593,stroke-width:2px
    classDef exception fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef view fill:#e0f7fa,stroke:#006064,stroke-width:2px

    %% Apply styling
    class HF,HC,DB,FS external
    class PY,BLIP,OPT,PYMANAGER,HEALTH python
    class WEB,HTML frontend
    class APP spring
    class AICONFIG,DBCONFIG,APPCONFIG,SHELLCONFIG config
    class CHATCTRL,IMAGECTRL,ANIMALCTRL,EMERGENCYCTRL controller
    class CHATSVC,AISVC,IMGSVC,ANIMALSVC,EMERGENCYSVC,CLEANUPSVC service
    class CONVREPO,MSGREPO,ANIMALREPO repository
    class CONV,MSG,ANIMAL,MSGIMG entity
    class CHATREQ,CHATRESP,STRUCTRESP,MSGDTO,ANIMALDTO,SESSIONSTART,HISTORYRESP dto
    class ANALYSIS,CONTEXT,VETLOC model
    class MSGTYPE,URGENCY enum
    class APICLIENT,FILEUTILS,VALIDATION utility
    class EXCEPTION exception
    class MAINVIEW,CHATVIEW view
```

## Architecture Overview

### 🏗️ **System Architecture**

The VetBot application follows a **layered architecture pattern** with clear separation of concerns:

#### **1. External Systems Layer**
- **Hugging Face API**: Provides fallback image classification when local VQA fails
- **HackClub AI API**: Powers the GPT-4o-mini model for conversational AI responses
- **H2 Database**: Stores conversations, messages, and animal profiles
- **File System**: Manages uploaded pet images

#### **2. Python VQA Service**
- **Flask Server** (`main.py`): Standalone Python service for visual question answering
- **BLIP VQA Model**: Salesforce's BLIP model for comprehensive image analysis
- **OptimizedVeterinaryVQAService**: Custom veterinary-specific analysis engine

#### **3. Spring Boot Application**
- **Main Application**: Central orchestrator with process management
- **Configuration Layer**: Manages all application settings and configurations
- **Controller Layer**: RESTful API endpoints for client communication
- **Service Layer**: Business logic and orchestration
- **Repository Layer**: Data access abstraction
- **Entity Layer**: JPA entities representing database tables
- **DTO Layer**: Data transfer objects for API communication
- **Model Layer**: Domain models for complex data structures
- **Enum Layer**: Type-safe enumerations
- **Utility Layer**: Helper classes and utilities
- **Exception Layer**: Global error handling
- **View Layer**: Vaadin UI components

### 🔄 **Data Flow**

1. **User Interaction**: Frontend sends chat messages and images to controllers
2. **Request Processing**: Controllers delegate to appropriate services
3. **Image Analysis**: Images are processed by Python VQA service or Hugging Face API
4. **AI Response**: Chat service orchestrates AI conversation generation
5. **Data Persistence**: Conversations, messages, and profiles are stored in database
6. **Response Generation**: Structured responses are formatted and returned to frontend

### 🔗 **Key Relationships**

- **ChatService** is the central orchestrator, coordinating all other services
- **Conversation** entities contain **Message** entities and **AnimalProfile** entities
- **ImageAnalysisService** integrates with both Python VQA and external APIs
- **AIConversationService** generates structured veterinary responses
- **PythonVQAServerManager** manages the external Python service lifecycle

### 🎯 **Core Features**

- **Multi-modal Analysis**: Text + image processing for comprehensive veterinary consultation
- **Contextual AI**: Maintains conversation history and animal profiles
- **Emergency Detection**: Identifies critical situations requiring immediate care
- **Structured Responses**: Rich, organized veterinary advice with recommendations
- **Session Management**: Persistent conversations with cleanup and monitoring
- **Fallback Systems**: Multiple analysis methods for reliability

This architecture ensures **scalability**, **maintainability**, and **reliability** while providing comprehensive veterinary consultation capabilities.
