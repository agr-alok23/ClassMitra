import test from "node:test";
import assert from "node:assert/strict";
import {
  parseJwt,
  pemToDerBytes,
  parseAsn1Element,
  extractSpkiFromX509Der,
  checkRateLimit,
  callGroqApi
} from "../src/index.js";

test("JWT rejects malformed structure", () => {
  assert.equal(parseJwt("a.b"), null);
  assert.equal(parseJwt("!a.e30=.x"), null);
  assert.equal(parseJwt("e30=.e30=.e30="), null);
});

test("Google PEM certificates with a trailing newline decode", () => {
  const der = pemToDerBytes("-----BEGIN CERTIFICATE-----\nMAA=\n-----END CERTIFICATE-----\n");
  assert.deepEqual(Array.from(der), [0x30, 0x00]);
});

test("ASN.1 rejects truncated and non-canonical lengths", () => {
  assert.throws(() => parseAsn1Element(new Uint8Array([0x30]), 0), /header truncated/);
  assert.throws(() => parseAsn1Element(new Uint8Array([0x30, 0x82, 0x01, 0x00]), 0), /payload truncated/);
  assert.throws(() => parseAsn1Element(new Uint8Array([0x30, 0x82, 0x00, 0x01, 0x00]), 0), /non-minimal/);
});

test("X.509 rejects empty, wrong outer tag, and trailing data", () => {
  assert.throws(() => extractSpkiFromX509Der(new Uint8Array()), /empty/);
  assert.throws(() => extractSpkiFromX509Der(new Uint8Array([0x31, 0x00])), /outer SEQUENCE/);
  assert.throws(() => extractSpkiFromX509Der(new Uint8Array([0x30, 0x02, 0x02, 0x00, 0x00])), /trailing/);
});

test("in-memory rate limit blocks after configured requests", () => {
  const key = "test-rate-limit-" + Date.now();
  for (let i = 0; i < 30; i++) assert.equal(checkRateLimit(key, 30, 60000), true);
  assert.equal(checkRateLimit(key, 30, 60000), false);
});

test("Groq API rejects malformed success JSON", async () => {
  const previousFetch = globalThis.fetch;
  globalThis.fetch = async () => ({
    ok: true,
    json: async () => ({ choices: [] })
  });
  try {
    await assert.rejects(
      callGroqApi("test-placeholder", "openai/gpt-oss-20b", [{ role: "user", content: "test" }]),
      /Empty response/
    );
  } finally {
    globalThis.fetch = previousFetch;
  }
});

test("Groq API does not include provider error body", async () => {
  const previousFetch = globalThis.fetch;
  globalThis.fetch = async () => ({
    ok: false,
    status: 401,
    text: async () => "sensitive provider body"
  });
  try {
    await assert.rejects(
      callGroqApi("test-placeholder", "openai/gpt-oss-20b", [{ role: "user", content: "test" }]),
      /HTTP 401/
    );
  } finally {
    globalThis.fetch = previousFetch;
  }
});
