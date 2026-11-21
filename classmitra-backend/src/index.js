/**
 * Cloudflare Worker Backend for ClassMitra
 * Integrates Groq AI with Structural ASN.1 Cryptographic Firebase ID Token Verification.
 */

// In-memory cache for Google public keys used in Firebase JWT signature verification
let googleKeysCache = null;
let googleKeysExpiry = 0;

async function getGooglePublicKeys() {
  const now = Date.now();
  if (googleKeysCache && now < googleKeysExpiry) {
    return googleKeysCache;
  }
  return await fetchGooglePublicKeys(now);
}

async function fetchGooglePublicKeys(now = Date.now(), force = false) {
  if (!force && googleKeysCache && now < googleKeysExpiry) return googleKeysCache;
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 5000);
    let res;
    try {
      res = await fetch("https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com", {
        signal: controller.signal
      });
    } finally {
      clearTimeout(timeout);
    }
    if (!res.ok) return null;
    const keys = await res.json();
    if (!keys || typeof keys !== "object" || Array.isArray(keys)) return null;
    const validKeys = Object.keys(keys).length > 0;
    if (!validKeys) return null;
    const cacheControl = res.headers.get("cache-control");
    let maxAge = 3600;
    if (cacheControl) {
      const match = cacheControl.match(/max-age=(\d+)/);
      if (match) maxAge = Math.min(86400, Math.max(60, parseInt(match[1], 10)));
    }
    googleKeysCache = keys;
    googleKeysExpiry = now + maxAge * 1000;
    return googleKeysCache;
  } catch (err) {
    return null;
  }
}

function base64UrlDecode(str) {
  if (typeof str !== "string" || str.length === 0 || !/^[A-Za-z0-9_-]+$/.test(str)) {
    throw new Error("Invalid base64url segment");
  }
  let normalized = str.replace(/-/g, "+").replace(/_/g, "/");
  while (normalized.length % 4) normalized += "=";
  return atob(normalized);
}

function parseJwt(token) {
  try {
    if (typeof token !== "string" || token.length > 16384) return null;
    const parts = token.split('.');
    if (parts.length !== 3 || parts.some(part => part.length === 0)) return null;
    const headerText = base64UrlDecode(parts[0]);
    const payloadText = base64UrlDecode(parts[1]);
    base64UrlDecode(parts[2]);
    const header = JSON.parse(headerText);
    const payload = JSON.parse(payloadText);
    if (!header || typeof header !== "object" || Array.isArray(header)) return null;
    if (!payload || typeof payload !== "object" || Array.isArray(payload)) return null;
    return { header, payload, signature: parts[2], rawHeaderPayload: `${parts[0]}.${parts[1]}` };
  } catch (e) {
    return null;
  }
}

function pemToDerBytes(pem) {
  if (typeof pem !== "string") throw new Error("Invalid PEM certificate");
  const match = pem.trim().match(/^-----BEGIN CERTIFICATE-----\r?\n([A-Za-z0-9+/=\r\n]+)-----END CERTIFICATE-----$/);
  if (!match) throw new Error("Invalid PEM certificate");
  const encoded = match[1].replace(/\s/g, "");
  const raw = atob(encoded);
  const buffer = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) {
    buffer[i] = raw.charCodeAt(i);
  }
  return buffer;
}

function parseAsn1Element(bytes, offset) {
  if (!Number.isInteger(offset) || offset < 0 || offset >= bytes.length) {
    throw new Error("ASN.1 parse error: unexpected end of buffer");
  }
  if (offset + 2 > bytes.length) {
    throw new Error("ASN.1 parse error: header truncated");
  }
  const tag = bytes[offset];
  const lenByte = bytes[offset + 1];
  let headerLen = 2;
  let length = 0;

  if (lenByte & 0x80) {
    const numBytes = lenByte & 0x7f;
    if (numBytes === 0 || numBytes > 4 || offset + 2 + numBytes > bytes.length) {
      throw new Error("ASN.1 parse error: invalid length encoding");
    }
    if (bytes[offset + 2] === 0x00) {
      throw new Error("ASN.1 parse error: non-minimal length encoding");
    }
    headerLen = 2 + numBytes;
    for (let i = 0; i < numBytes; i++) {
      length = length * 256 + bytes[offset + 2 + i];
    }
    if (!Number.isSafeInteger(length) || length > 0x7fffffff) {
      throw new Error("ASN.1 parse error: invalid length");
    }
    if (numBytes === 1 && length < 128) {
      throw new Error("ASN.1 parse error: non-minimal length encoding");
    }
  } else {
    length = lenByte;
  }

  const totalLen = headerLen + length;
  if (!Number.isSafeInteger(totalLen) || offset + totalLen > bytes.length) {
    throw new Error("ASN.1 parse error: element payload truncated");
  }

  return { tag, length, headerLen, totalLen, offset, end: offset + totalLen };
}

/**
 * Structural ASN.1 DER parser that extracts the SubjectPublicKeyInfo (SPKI) sequence
 * from an X.509 certificate DER buffer per RFC 5280.
 * Returns a new, isolated ArrayBuffer copy containing only SPKI bytes.
 */
function extractSpkiFromX509Der(certDerBytes) {
  if (!(certDerBytes instanceof Uint8Array) && !(certDerBytes instanceof ArrayBuffer)) {
    throw new Error("Invalid X.509 certificate input");
  }
  const bytes = certDerBytes instanceof Uint8Array
    ? certDerBytes
    : new Uint8Array(certDerBytes);
  if (bytes.length === 0) throw new Error("Invalid X.509 certificate: empty input");

  // 1. Outer Certificate SEQUENCE (0x30)
  const certSeq = parseAsn1Element(bytes, 0);
  if (certSeq.tag !== 0x30) throw new Error("Invalid X.509 cert: expected outer SEQUENCE (0x30)");
  if (certSeq.end !== bytes.length) throw new Error("Invalid X.509 cert: trailing data after certificate");

  // 2. TBSCertificate SEQUENCE (0x30)
  let pos = certSeq.offset + certSeq.headerLen;
  const tbsSeq = parseAsn1Element(bytes, pos);
  if (tbsSeq.tag !== 0x30) throw new Error("Invalid X.509 cert: expected TBSCertificate SEQUENCE (0x30)");
  if (tbsSeq.end > certSeq.end) throw new Error("Invalid X.509 cert: TBSCertificate exceeds certificate boundary");

  // 3. Traverse elements inside TBSCertificate to reach SubjectPublicKeyInfo
  let tbsPos = tbsSeq.offset + tbsSeq.headerLen;
  if (tbsPos >= tbsSeq.end) throw new Error("Invalid X.509 cert: truncated TBSCertificate");

  // Check for explicit version [0] EXPLICIT
  if (bytes[tbsPos] === 0xa0) {
    const versionEl = parseAsn1Element(bytes, tbsPos);
    if (versionEl.end > tbsSeq.end) throw new Error("Invalid X.509 cert: version exceeds TBSCertificate boundary");
    tbsPos = versionEl.end;
  }

  // SerialNumber (INTEGER 0x02)
  const serialEl = parseAsn1Element(bytes, tbsPos);
  if (serialEl.tag !== 0x02 || serialEl.end > tbsSeq.end) throw new Error("Invalid X.509 cert: invalid serial number");
  tbsPos = serialEl.end;

  // Signature Algorithm (SEQUENCE 0x30)
  const sigAlgEl = parseAsn1Element(bytes, tbsPos);
  if (sigAlgEl.tag !== 0x30 || sigAlgEl.end > tbsSeq.end) throw new Error("Invalid X.509 cert: invalid signature algorithm");
  tbsPos = sigAlgEl.end;

  // Issuer Name (SEQUENCE 0x30)
  const issuerEl = parseAsn1Element(bytes, tbsPos);
  if (issuerEl.tag !== 0x30 || issuerEl.end > tbsSeq.end) throw new Error("Invalid X.509 cert: invalid issuer");
  tbsPos = issuerEl.end;

  // Validity (SEQUENCE 0x30)
  const validityEl = parseAsn1Element(bytes, tbsPos);
  if (validityEl.tag !== 0x30 || validityEl.end > tbsSeq.end) throw new Error("Invalid X.509 cert: invalid validity");
  tbsPos = validityEl.end;

  // Subject Name (SEQUENCE 0x30)
  const subjectEl = parseAsn1Element(bytes, tbsPos);
  if (subjectEl.tag !== 0x30 || subjectEl.end > tbsSeq.end) throw new Error("Invalid X.509 cert: invalid subject");
  tbsPos = subjectEl.end;
  if (tbsPos >= tbsSeq.end) throw new Error("Invalid X.509 cert: missing SubjectPublicKeyInfo");

  // SubjectPublicKeyInfo (SEQUENCE 0x30)
  const spkiEl = parseAsn1Element(bytes, tbsPos);
  if (spkiEl.tag !== 0x30) {
    throw new Error("Invalid X.509 cert: expected SubjectPublicKeyInfo SEQUENCE (0x30)");
  }

  if (spkiEl.end > tbsSeq.end) {
    throw new Error("Invalid X.509 cert: SubjectPublicKeyInfo extends beyond TBSCertificate boundary");
  }

  if (spkiEl.end > tbsSeq.end) {
    throw new Error("Invalid X.509 cert: SubjectPublicKeyInfo extends beyond TBSCertificate boundary");
  }

  let remainderPos = spkiEl.end;
  while (remainderPos < tbsSeq.end) {
    const optionalEl = parseAsn1Element(bytes, remainderPos);
    if (optionalEl.end > tbsSeq.end) throw new Error("Invalid X.509 cert: TBSCertificate field exceeds boundary");
    remainderPos = optionalEl.end;
  }
  if (remainderPos !== tbsSeq.end) throw new Error("Invalid X.509 cert: malformed TBSCertificate");

  let certPos = tbsSeq.end;
  const outerSigAlg = parseAsn1Element(bytes, certPos);
  if (outerSigAlg.tag !== 0x30 || outerSigAlg.end > certSeq.end) {
    throw new Error("Invalid X.509 cert: invalid outer signature algorithm");
  }
  certPos = outerSigAlg.end;
  const signatureValue = parseAsn1Element(bytes, certPos);
  if (signatureValue.tag !== 0x03 || signatureValue.end !== certSeq.end) {
    throw new Error("Invalid X.509 cert: malformed outer signature");
  }

  const spkiBytes = new Uint8Array(bytes.slice(spkiEl.offset, spkiEl.end));
  return spkiBytes.buffer;
}

async function verifyFirebaseToken(authHeader, projectId) {
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return { valid: false, error: "Missing or malformed Authorization header. Expected 'Bearer <token>'" };
  }

  const token = authHeader.substring(7).trim();
  const jwt = parseJwt(token);
  if (!jwt) {
    return { valid: false, error: "Invalid JWT structure" };
  }

  const { header, payload, signature, rawHeaderPayload } = jwt;
  const now = Math.floor(Date.now() / 1000);
  const iatToleranceSeconds = 300;
  const fixedProjectId = "classmitra-9d938";
  if (projectId !== fixedProjectId) {
    return { valid: false, error: "Invalid server Firebase project configuration" };
  }

  // 1. Verify algorithm and key ID
  if (header.alg !== "RS256") {
    return { valid: false, error: `Unsupported JWT algorithm: ${header.alg}` };
  }
  if (!header.kid || typeof header.kid !== 'string') {
    return { valid: false, error: "Missing or invalid key ID (kid) in token header" };
  }

  // Cryptographically verify before evaluating untrusted claims.
  let keys = await getGooglePublicKeys();
  if (!keys) {
    return { valid: false, error: "Unable to retrieve Google signing keys for verification" };
  }

  let pemCert = keys[header.kid];
  if (!pemCert) {
    keys = await fetchGooglePublicKeys(Date.now(), true);
    pemCert = keys && keys[header.kid];
  }
  if (!pemCert) {
    return { valid: false, error: `Unknown signing key ID: ${header.kid}` };
  }

  try {
    const certDerBytes = pemToDerBytes(pemCert);
    const spkiDerBuffer = extractSpkiFromX509Der(certDerBytes);

    const cryptoKey = await crypto.subtle.importKey(
      "spki",
      spkiDerBuffer,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"]
    );

    const encoder = new TextEncoder();
    const dataBuffer = encoder.encode(rawHeaderPayload);

    const sigStr = base64UrlDecode(signature);
    const sigBuffer = new Uint8Array(sigStr.length);
    for (let i = 0; i < sigStr.length; i++) {
      sigBuffer[i] = sigStr.charCodeAt(i);
    }

    const isSigValid = await crypto.subtle.verify(
      "RSASSA-PKCS1-v1_5",
      cryptoKey,
      sigBuffer,
      dataBuffer
    );

    if (!isSigValid) {
      return { valid: false, error: "Firebase ID token signature verification failed" };
    }
  } catch (e) {
    return { valid: false, error: "Cryptographic verification failed" };
  }

  // Claims are checked only after the signature has verified.
  if (!Number.isInteger(payload.exp) || payload.exp <= now) {
    return { valid: false, error: "Firebase ID token has expired" };
  }
  if (!Number.isInteger(payload.iat) || payload.iat > now + iatToleranceSeconds) {
    return { valid: false, error: "Firebase ID token issued in the future" };
  }
  if (payload.iat < 0 || payload.exp <= payload.iat) {
    return { valid: false, error: "Invalid token timestamps" };
  }

  const expectedIss = `https://securetoken.google.com/${fixedProjectId}`;
  if (payload.iss !== expectedIss) {
    return { valid: false, error: `Invalid issuer. Expected: ${expectedIss}` };
  }
  if (typeof payload.aud !== "string" || payload.aud !== fixedProjectId) {
    return { valid: false, error: `Invalid audience. Expected: ${fixedProjectId}` };
  }
  if (typeof payload.sub !== 'string' || payload.sub.trim() === '' ||
      payload.sub.length > 128 || payload.sub !== payload.sub.trim()) {
    return { valid: false, error: "Invalid subject (user ID) in token" };
  }

  return { valid: true, uid: payload.sub };
}

// In-memory rate limiting map per authenticated user ID (Isolate-local)
// LIMITATION DOCUMENTATION: This Map operates per Cloudflare Worker isolate.
// For strict multi-region global rate limiting across all Cloudflare edge locations,
// use Cloudflare Durable Objects or Cloudflare Workers KV.
const rateLimitMap = new Map();
function checkRateLimit(key, maxRequests = 30, windowMs = 60000) {
  const now = Date.now();
  const record = rateLimitMap.get(key) || { count: 0, startTime: now };
  if (now - record.startTime > windowMs) {
    record.count = 1;
    record.startTime = now;
  } else {
    record.count++;
  }
  rateLimitMap.set(key, record);
  return record.count <= maxRequests;
}

export default {
  async fetch(request, env, ctx) {
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    const path = url.pathname;

    // Active default model fallback
    const textModel = env.GROQ_TEXT_MODEL || "openai/gpt-oss-20b";
    const visionModel = env.GROQ_VISION_MODEL || "disabled";
    const isVisionEnabled = env.ENABLE_VISION === "true" && visionModel !== "disabled";

    // GET /health
    if (request.method === "GET" && path === "/health") {
      return new Response(
        JSON.stringify({
          status: "ok",
          service: "ClassMitra Cloudflare Backend",
          timestamp: new Date().toISOString(),
          configuredModels: {
            textModel: textModel,
            visionModel: visionModel,
            visionEnabled: isVisionEnabled
          }
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (request.method !== "POST") {
      return new Response(
        JSON.stringify({ error: "Method Not Allowed" }),
        { status: 405, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!env.GROQ_API_KEY) {
      return new Response(
        JSON.stringify({ error: "Groq API Key is not configured on the server." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Authenticate Firebase ID Token
    const authHeader = request.headers.get("Authorization");
    const projectId = env.FIREBASE_PROJECT_ID || "classmitra-9d938";
    const authResult = await verifyFirebaseToken(authHeader, projectId);

    if (!authResult.valid) {
      return new Response(
        JSON.stringify({ error: `Authentication Error: ${authResult.error}` }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Rate Limiting Keyed by Authenticated User ID (Isolate-local limitation documented)
    const userKey = authResult.uid;
    if (!checkRateLimit(userKey, 30, 60000)) {
      return new Response(
        JSON.stringify({ error: "Rate limit exceeded. Please wait 60 seconds before making more requests." }),
        { status: 429, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    let body;
    try {
      const contentLength = request.headers.get("content-length");
      if (contentLength && Number(contentLength) > 1024 * 1024) {
        return new Response(
          JSON.stringify({ error: "Request body too large" }),
          { status: 413, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
      body = await request.json();
    } catch (e) {
      return new Response(
        JSON.stringify({ error: "Invalid JSON request body" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    try {
      if (path === "/chat") {
        const { prompt, image } = body;
        if (!prompt || typeof prompt !== 'string' || prompt.trim() === '') {
          return new Response(
            JSON.stringify({ error: "Prompt cannot be empty" }),
            { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }
        if (prompt.length > 20000) {
          return new Response(
            JSON.stringify({ error: "Prompt is too large" }),
            { status: 413, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }

        if (image) {
          if (typeof image !== "string" || image.length > 7000000) {
            return new Response(
              JSON.stringify({ error: "Image payload is too large" }),
              { status: 413, headers: { ...corsHeaders, "Content-Type": "application/json" } }
            );
          }
          if (!isVisionEnabled) {
            return new Response(
              JSON.stringify({ error: "Image vision analysis is currently unavailable on this Groq model configuration. Please submit text prompts." }),
              { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
            );
          }
        }

        const systemMessage = {
          role: "system",
          content: "You are Mitra, a friendly, highly intelligent AI learning companion for ClassMitra. Help students understand concepts clearly, format your answers with clean Markdown (headers, bold text, bullet points, tables), and explain step-by-step."
        };

        let messages = [systemMessage];
        let targetModel = textModel;

        if (image && isVisionEnabled) {
          targetModel = visionModel;
          messages.push({
            role: "user",
            content: [
              { type: "text", text: prompt },
              { type: "image_url", image_url: { url: image.startsWith("data:") ? image : `data:image/jpeg;base64,${image}` } }
            ]
          });
        } else {
          messages.push({ role: "user", content: prompt });
        }

        const groqResponse = await callGroqApi(env.GROQ_API_KEY, targetModel, messages, 0.7, 2048);
        return new Response(
          JSON.stringify({ response: groqResponse }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );

      } else if (path === "/summary") {
        const { text } = body;
        if (!text || typeof text !== 'string' || text.trim() === '') {
          return new Response(
            JSON.stringify({ error: "Content text to summarize cannot be empty" }),
            { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }

        const safeText = text.length > 30000 ? text.substring(0, 30000) + "\n\n[Content truncated for length]" : text;
        const messages = [
          {
            role: "system",
            content: "You are Mitra AI summarizer. Provide a clean, structured summary of the educational content using clear Markdown headings, key bullet points, and key terms."
          },
          {
            role: "user",
            content: `Please summarize this educational text:\n\n${safeText}`
          }
        ];

        const groqResponse = await callGroqApi(env.GROQ_API_KEY, textModel, messages, 0.5, 2048);
        return new Response(
          JSON.stringify({ response: groqResponse }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );

      } else if (path === "/quiz") {
        const { text } = body;
        if (!text || typeof text !== 'string' || text.trim() === '') {
          return new Response(
            JSON.stringify({ error: "Topic or text for quiz generation cannot be empty" }),
            { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }

        const safeText = text.length > 20000 ? text.substring(0, 20000) : text;
        const messages = [
          {
            role: "system",
            content: "You are an educational quiz creator. Generate 5 multiple choice questions (MCQs) based on the provided material.\nOUTPUT ONLY A VALID JSON ARRAY. NO MARKDOWN WRAPPERS, NO COMMENTARY.\nFormat:\n[\n  {\n    \"question\": \"Question text?\",\n    \"options\": [\"Opt A\", \"Opt B\", \"Opt C\", \"Opt D\"],\n    \"correctIndex\": 0\n  }\n]"
          },
          {
            role: "user",
            content: `Generate 5 MCQs from this material:\n\n${safeText}`
          }
        ];

        const groqResponse = await callGroqApi(env.GROQ_API_KEY, textModel, messages, 0.4, 1500);
        const quiz = parseQuizResponse(groqResponse);
        if (!quiz) {
          return new Response(
            JSON.stringify({ error: "The AI model returned an invalid quiz format." }),
            { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }
        return new Response(
          JSON.stringify({ response: JSON.stringify(quiz) }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );

      } else if (path === "/flashcards") {
        const { text } = body;
        if (!text || typeof text !== 'string' || text.trim() === '') {
          return new Response(
            JSON.stringify({ error: "Topic or text for flashcards generation cannot be empty" }),
            { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }

        const safeText = text.length > 20000 ? text.substring(0, 20000) : text;
        const messages = [
          {
            role: "system",
            content: "You are an educational flashcard creator. Generate 5 revision flashcards based on the provided material.\nOUTPUT ONLY A VALID JSON ARRAY. NO MARKDOWN WRAPPERS, NO COMMENTARY.\nFormat:\n[\n  {\n    \"question\": \"Question or term\",\n    \"answer\": \"Detailed answer or explanation\"\n  }\n]"
          },
          {
            role: "user",
            content: `Generate 5 flashcards from this material:\n\n${safeText}`
          }
        ];

        const groqResponse = await callGroqApi(env.GROQ_API_KEY, textModel, messages, 0.4, 1500);
        const flashcards = parseFlashcardResponse(groqResponse);
        if (!flashcards) {
          return new Response(
            JSON.stringify({ error: "The AI model returned an invalid flashcard format." }),
            { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
          );
        }
        return new Response(
          JSON.stringify({ response: JSON.stringify(flashcards) }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );

      } else {
        return new Response(
          JSON.stringify({ error: "Endpoint not found" }),
          { status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    } catch (err) {
      return new Response(
        JSON.stringify({ error: "AI request could not be completed." }),
        { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }
  }
};

function extractJsonArray(raw) {
  if (typeof raw !== "string") return null;
  const trimmed = raw.trim();
  const fenced = trimmed.match(/^```(?:json)?\s*([\s\S]*?)\s*```$/i);
  const candidate = (fenced ? fenced[1] : trimmed).trim();
  if (candidate.length > 100000) return null;
  try {
    const parsed = JSON.parse(candidate);
    return Array.isArray(parsed) ? parsed : null;
  } catch (err) {
    return null;
  }
}

function parseQuizResponse(raw) {
  const quiz = extractJsonArray(raw);
  if (!quiz || quiz.length === 0 || quiz.length > 10) return null;
  for (const item of quiz) {
    if (!item || typeof item !== "object" || Array.isArray(item)) return null;
    if (typeof item.question !== "string" || item.question.trim().length === 0) return null;
    if (!Array.isArray(item.options) || item.options.length !== 4) return null;
    if (!item.options.every(option => typeof option === "string" && option.trim().length > 0)) return null;
    if (!Number.isInteger(item.correctIndex) || item.correctIndex < 0 || item.correctIndex > 3) return null;
  }
  return quiz;
}

function parseFlashcardResponse(raw) {
  const cards = extractJsonArray(raw);
  if (!cards || cards.length === 0 || cards.length > 20) return null;
  for (const item of cards) {
    if (!item || typeof item !== "object" || Array.isArray(item)) return null;
    if (typeof item.question !== "string" || item.question.trim().length === 0) return null;
    if (typeof item.answer !== "string" || item.answer.trim().length === 0) return null;
  }
  return cards;
}

async function callGroqApi(apiKey, model, messages, temperature = 0.7, maxTokens = 2048) {
  const payload = {
    model: model,
    messages: messages,
    temperature: temperature,
    max_tokens: maxTokens
  };

  let res;
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 30000);
    try {
      res = await fetch("https://api.groq.com/openai/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiKey}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload),
        signal: controller.signal
      });
    } finally {
      clearTimeout(timeout);
    }
  } catch (err) {
    throw new Error("Groq API request timed out or failed. Please try again.");
  }

  if (!res.ok) {
    // Do not log or return provider response bodies: they may contain sensitive input or vendor metadata.
    if (res.status === 429) {
      throw new Error("Groq API rate limit reached. Please try again in a few seconds.");
    }
    throw new Error(`Groq API returned error HTTP ${res.status}.`);
  }

  let data;
  try {
    data = await res.json();
  } catch (err) {
    throw new Error("Groq API returned an invalid response.");
  }
  if (data && Array.isArray(data.choices) && data.choices.length > 0 &&
      data.choices[0] && data.choices[0].message &&
      typeof data.choices[0].message.content === "string" &&
      data.choices[0].message.content.trim().length > 0) {
    return data.choices[0].message.content;
  }
  throw new Error("Empty response received from Groq model.");
}

export {
  parseJwt,
  pemToDerBytes,
  parseAsn1Element,
  extractSpkiFromX509Der,
  verifyFirebaseToken,
  checkRateLimit,
  callGroqApi
};
