import { appendFileSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = process.cwd();
const actionFile = resolve(
  projectRoot,
  '.github/actions/detect-changed-scope-with-summary/action.yml',
);

const requiredByScope = {
  frontend: ['scripts/runtime-*.mjs'],
  backend: ['scripts/run-maven.mjs'],
  security: ['scripts/security-scan.mjs'],
};

const forbiddenPatterns = new Set(['scripts/**']);

function appendStepSummary(lines) {
  const summaryPath = process.env.GITHUB_STEP_SUMMARY;
  if (!summaryPath) {
    return;
  }
  try {
    const content = `${lines.join('\n')}\n`;
    appendFileSync(summaryPath, content, 'utf8');
  } catch {
    // Ignore summary write errors to avoid masking guard result.
  }
}

export function parseScopePatterns(actionYmlContent) {
  const scopes = {
    frontend: [],
    backend: [],
    security: [],
  };

  let currentScope = null;
  for (const rawLine of actionYmlContent.split(/\r?\n/)) {
    const scopeMatch = rawLine.match(/^\s*(frontend|backend|security):\s*$/);
    if (scopeMatch) {
      currentScope = scopeMatch[1];
      continue;
    }

    if (!currentScope) {
      continue;
    }

    const patternMatch = rawLine.match(/^\s*-\s*['"]?([^'"]+)['"]?\s*$/);
    if (patternMatch) {
      scopes[currentScope].push(patternMatch[1].trim());
      continue;
    }

    if (/^\s*[A-Za-z0-9_-]+:\s*$/.test(rawLine)) {
      currentScope = null;
    }
  }

  return scopes;
}

export function validateScopes(scopes) {
  const errors = [];

  for (const [scope, patterns] of Object.entries(scopes)) {
    for (const pattern of patterns) {
      if (forbiddenPatterns.has(pattern)) {
        errors.push(
          `[forbidden] ${scope} contains disallowed pattern "${pattern}"`,
        );
      }
    }
  }

  for (const [scope, requiredPatterns] of Object.entries(requiredByScope)) {
    for (const requiredPattern of requiredPatterns) {
      if (!scopes[scope].includes(requiredPattern)) {
        errors.push(
          `[missing] ${scope} must include required pattern "${requiredPattern}"`,
        );
      }
    }
  }

  return errors;
}

export function runGuard(actionYmlContent) {
  const scopes = parseScopePatterns(actionYmlContent);
  return validateScopes(scopes);
}

function main() {
  const content = readFileSync(actionFile, 'utf8');
  const errors = runGuard(content);

  if (errors.length > 0) {
    console.error('ci-scope-guard failed:');
    for (const error of errors) {
      console.error(`- ${error}`);
    }
    appendStepSummary([
      '### CI Scope Guard',
      '',
      '- status: failed',
      '',
      'Detected issues:',
      ...errors.map((error) => `- ${error}`),
    ]);
    process.exit(1);
  }

  console.log('ci-scope-guard passed');
  appendStepSummary([
    '### CI Scope Guard',
    '',
    '- status: passed',
  ]);
}

const isDirectRun = process.argv[1] === fileURLToPath(import.meta.url);
if (isDirectRun) {
  main();
}
