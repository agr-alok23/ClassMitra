# ClassMitra Cloudflare Worker Backend (Groq AI Integration)

This Cloudflare Worker acts as the secure backend proxy for ClassMitra, routing AI requests from the Android app to **Groq AI** using the Groq OpenAI-compatible Chat Completions API.

## Features
- **Secure API Key Handling**: Your Groq API Key is stored safely on Cloudflare as an environment secret (`GROQ_API_KEY`).
- **Firebase Authentication**: Validates Firebase ID tokens (`Authorization: Bearer <token>`) passed from ClassMitra.
- **Endpoints**:
  - `GET /health` - Health check & model status.
  - `POST /chat` - AI Chatbot (supports text & vision multimodal inputs).
  - `POST /summary` - Document / notes summarization.
  - `POST /quiz` - MCQ Quiz generation (returns JSON array).
  - `POST /flashcards` - Flashcards generation (returns JSON array).
- **Free-Tier Protection**: Rate limiting and payload length safety limits.

---

## Local Development Setup

1. Install Node.js (v18+) and Wrangler CLI:
   ```bash
   npm install
   ```

2. Create a `.dev.vars` file for local development:
   ```bash
   cp .dev.vars.example .dev.vars
   ```
   Edit `.dev.vars` and add your Groq API Key:
   ```env
   GROQ_API_KEY=gsk_your_groq_api_key_here
   ```

3. Run the local development server:
   ```bash
   npx wrangler dev
   ```
   The local backend will run at `http://localhost:8787` (or `http://10.0.2.2:8787` from Android Emulator).

---

## Deployment to Cloudflare Workers

1. Login to Cloudflare via Wrangler:
   ```bash
   npx wrangler login
   ```

2. Add your Groq API Key secret to Cloudflare:
   ```bash
   npx wrangler secret put GROQ_API_KEY
   ```
   (Paste your Groq API Key starting with `gsk_...` when prompted).

3. Deploy the worker:
   ```bash
   npx wrangler deploy
   ```
   Wrangler will output your production URL, for example: `https://classmitra-backend.<your-subdomain>.workers.dev`.
