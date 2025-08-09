#!/bin/bash

echo "Waiting for Ollama to be ready..."
until curl -s http://localhost:11434/api/tags > /dev/null 2>&1; do
    sleep 5
    echo "Waiting for Ollama..."
done

echo "Ollama is ready. Pulling models..."

# Pull the main model
ollama pull llama3.1:8b

# Pull the fallback model
ollama pull mistral-nemo

# Optional: Pull additional models for better performance
# ollama pull qwen2.5-coder:7b

echo "Models pulled successfully!"