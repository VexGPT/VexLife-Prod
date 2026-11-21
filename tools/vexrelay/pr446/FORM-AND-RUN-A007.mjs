#!/usr/bin/env node
// [VXG RealForever]
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { pathToFileURL } from 'node:url';
import { spawnSync } from 'node:child_process';

const [configPath,sdkRoot,outputDirectory]=process.argv.slice(2);
if(!configPath||!sdkRoot||!outputDirectory) throw new Error('USAGE: FORM-AND-RUN-A007.mjs <config> <sdkRoot> <outputDirectory>');
const cfg=JSON.parse(fs.readFileSync(configPath,'utf8'));
const contract=JSON.parse(fs.readFileSync(path.join(sdkRoot,'docs/private-continuity/vexrelay/current-contract.json'),'utf8'));
if(contract.contractRef!==cfg.sdk.contractRef) throw new Error(`CURRENT_CONTRACT_REF_MISMATCH:${contract.contractRef}`);
if(contract.currentRuntimeVersion!==cfg.sdk.runtimeVersion) throw new Error(`CURRENT_RUNTIME_VERSION_MISMATCH:${contract.currentRuntimeVersion}`);

const formation=await import(pathToFileURL(path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs')).href);
const native=(await import(pathToFileURL(path.join(sdkRoot,'lib/vexlocalbridge/vexrelay-task-archive.js')).href)).default;
const suffix=`${new Date().toISOString().replace(/[-:.TZ]/g,'')}-${crypto.randomBytes(6).toString('hex')}`;
const now=new Date();
const replacements={
  '{{SUFFIX}}':suffix,
  '{{NOW}}':now.toISOString(),
  '{{EXPIRY}}':new Date(now.getTime()+cfg.route.ttlMs).toISOString(),
  '{{NATIVE_PAYLOAD_CLASS}}':native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,
  '{{NATIVE_PAYLOAD_RUNTIME}}':native.VEXRELAY_ARCHIVE_RUNTIME,
};
const materialize=(value)=>{
  if(Array.isArray(value)) return value.map(materialize);
  if(value&&typeof value==='object') return Object.fromEntries(Object.entries(value).map(([k,v])=>[k,materialize(v)]));
  if(typeof value!=='string') return value;
  let out=value;
  for(const [from,to] of Object.entries(replacements)) out=out.split(from).join(to);
  return out;
};

const result=await formation.formVexRelayTaskArchiveCarrier({
  recipe:cfg.route.recipe,
  seed:materialize(cfg.seedTemplate),
  stage:materialize(cfg.stageTemplate),
  outerTaskTemplate:materialize(cfg.outerTaskTemplate),
  authorityEnvelope:materialize(cfg.outerAuthorityTemplate),
  outputDirectory,
  outputStem:cfg.route.outputStem,
  ttlMs:cfg.route.ttlMs,
});
console.log(`VEXRELAY_OUTER_ZIP=${result.outer.finalZip}`);
console.log(`VEXRELAY_OUTER_SHA256=${result.outer.digest}`);
const launcher=path.join(sdkRoot,'lib/vexlocalbridge/templates/RUN-VEX-TASK.command');
const run=spawnSync('/bin/bash',[launcher,result.outer.finalZip],{stdio:'inherit',env:process.env});
if(run.error) throw run.error;
process.exit(run.status??1);
