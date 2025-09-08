-- Drop in correct order (children first, then parents)
DROP TABLE IF EXISTS message_images;
DROP TABLE IF EXISTS animal_profiles;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS conversations;

-- Create conversations table
CREATE TABLE conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_urgency_level VARCHAR(50) NOT NULL DEFAULT 'LOW'
);

-- Create messages table
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    message_type VARCHAR(10) NOT NULL CHECK (message_type IN ('USER', 'BOT')),
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    image_url VARCHAR(500),
    urgency_level VARCHAR(50),
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- Create animal_profiles table
CREATE TABLE animal_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    animal_type VARCHAR(100),
    breed VARCHAR(100),
    age INTEGER CHECK (age >= 0 AND age <= 30),
    weight DECIMAL(5,2) CHECK (weight > 0 AND weight <= 500),
    symptoms TEXT,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- Create message_images table
CREATE TABLE message_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    image_order INTEGER,
    image_url VARCHAR(500) NOT NULL,
    analysis_result TEXT,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_conversations_session_id ON conversations(session_id);
CREATE INDEX idx_conversations_last_activity ON conversations(last_activity);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_timestamp ON messages(timestamp);
CREATE INDEX idx_messages_type ON messages(message_type);
CREATE INDEX idx_animal_profiles_conversation_id ON animal_profiles(conversation_id);
CREATE INDEX idx_animal_profiles_animal_type ON animal_profiles(animal_type);
CREATE INDEX idx_message_images_message_id ON message_images(message_id);
