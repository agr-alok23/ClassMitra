# ClassMitra – AI Learning Companion

An AI-powered Android learning companion that transforms study materials into interactive learning experiences.

## Features
- AI Chat Assistant
- PDF Summarization
- AI Quiz Generation
- Flashcards
- Firebase Authentication
- Student & Teacher Modules

## Tech Stack
- Java
- Android Studio
- Firebase Authentication
- Groq API
- Cloudflare Workers

## Architecture
Android Application → Cloudflare Worker → Groq AI API

The backend verifies Firebase ID tokens and securely communicates with Groq using server-side credentials.

## Security
- Firebase Authentication
- Firebase ID Token verification
- API secrets stored as Cloudflare Worker secrets
- Sensitive configuration files excluded from Git

## Project Status
Currently functional with AI Chat, PDF Summarization, Quiz Generation, and Flashcard Generation.

## Author
Developed as an academic project.
