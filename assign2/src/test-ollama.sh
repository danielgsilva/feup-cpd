#!/bin/bash

echo "Testing Ollama connection..."

# Test if Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "ERROR: Ollama is not running or not accessible on port 11434"
    echo "Please start Ollama with: docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama14 ollama/ollama"
    exit 1
fi

echo "Ollama is running!"

# Test if llama3 model is available
echo "Checking available models..."
curl -s http://localhost:11434/api/tags

echo -e "\nTesting llama3 model..."
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama3",
    "prompt": "Hello! What day is today?",
    "stream": false
  }'

echo -e "\nOllama test complete!"


