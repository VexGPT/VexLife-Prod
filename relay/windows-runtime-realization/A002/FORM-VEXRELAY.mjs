import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

function fail(message) { console.error(message); process.exit(2); }
const [contextPathArg, sdkRootArg, outputDirectoryArg] = process.argv.slice(2);
if (!contextPathArg || !sdkRootArg || !outputDirectoryArg) fail('FORM_ARGS_REQUIRED');

const contextPath = path.resolve(contextPathArg);
const sdkRoot = path.resolve(sdkRootArg);
const outputDirectory = path.resolve(outputDirectoryArg);
const context = JSON.parse(fs.readFileSync(contextPath, 'utf8'));

const contractPath = path.join(sdkRoot, 'docs', 'private-continuity', 'vexrelay', 'current-contract.json');
const contract = JSON.parse(fs.readFileSync(contractPath, 'utf8'));
if (contract.contractRef !== context.sdk.contractRef) fail(`CURRENT_RELAY_CONTRACT_REF_MISMATCH:${contract.contractRef}`);

const seed = structuredClone(context.seed);
const now = new Date();
seed.execution.formedAt = now.toISOString();
seed.execution.expiresAtOrNull = new Date(now.getTime() + Number(context.ttlMs)).toISOString();

const formationModule = path.join(sdkRoot, 'lib', 'vexlocalbridge', 'vexrelay-task-archive-carrier-formation.mjs');
const windowsRunnerModule = path.join(sdkRoot, 'lib', 'vexlocalbridge', 'windows-runner.js');
const profilePath = path.join(sdkRoot, 'docs', 'private-continuity', 'vexlocalbridge', 'profiles', 'windows-victor-vextreme-sdk.json');

const { formVexRelayTaskArchiveCarrier } = await import(pathToFileURL(formationModule).href);
const windowsRunnerImport = await import(pathToFileURL(windowsRunnerModule).href);
const windowsRunner = windowsRunnerImport.default ?? windowsRunnerImport;
const profile = JSON.parse(fs.readFileSync(profilePath, 'utf8'));
const executables = windowsRunner.resolveProfileExecutables(profile);
const archiveAdapter = windowsRunner.archiveAdapter(executables);

const result = await formVexRelayTaskArchiveCarrier({
  recipe: context.recipe,
  seed,
  stage: context.stage,
  outerTaskTemplate: context.outerTaskTemplate,
  authorityEnvelope: context.authorityEnvelope,
  outputDirectory,
  outputStem: context.outputStem,
  ttlMs: Number(context.ttlMs),
  archiveAdapter,
});

console.log('VEXRELAY_FORMED\t' + JSON.stringify({
  outerZip: result.outer.finalZip,
  outerSha256: result.outer.digest,
  innerTaskRef: result.inner.manifest.taskRef,
  innerAttemptRef: result.inner.manifest.attemptRef,
  innerPackageRef: result.inner.manifest.packageRef,
  innerSha256: result.inner.sha256,
  stageGate: result.inner.manifest.completionPolicy.stageGateOrNull,
}));
