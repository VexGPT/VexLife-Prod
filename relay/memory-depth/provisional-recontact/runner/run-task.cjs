'use strict';

const crypto=require('node:crypto');
const fs=require('node:fs');
const path=require('node:path');

function sha256File(file){return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');}

const sdkRoot=path.resolve(process.argv[2]);
const taskZip=path.resolve(process.argv[3]);
const expected=process.argv[4];
const downloads=path.resolve(process.argv[5]);
if(sha256File(taskZip)!==expected) throw new Error('formed task digest mismatch before execution');

const cli=require(path.join(sdkRoot,'lib/vexlocalbridge/cli.js'));
const profilePath=path.join(sdkRoot,'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vextreme-sdk.json');

(async()=>{
  const profile=cli.loadProfile(profilePath);
  const capsuleRoot=cli.expectedCapsuleRoot(taskZip,expected,profile.zipRelayCapsule);
  const result=await cli.runTaskPackage({packagePath:taskZip,profilePath,capsuleRoot,revealTerminal:false});
  if(!result.resultZip || !fs.existsSync(result.resultZip)) throw new Error('canonical VexLocalBridge RETURN missing');
  const returnName=path.basename(result.resultZip);
  const dest=path.join(downloads,returnName);
  if(path.resolve(result.resultZip)!==path.resolve(dest)) fs.copyFileSync(result.resultZip,dest);
  const digest=sha256File(dest);
  process.stdout.write('\nVEXLIFE_RETURN='+dest+'\n');
  process.stdout.write('VEXLIFE_RETURN_SHA256='+digest+'\n');
  process.stdout.write('VEXLIFE_DISPOSITION='+(result.disposition||'UNKNOWN')+'\n');
})().catch(error=>{
  process.stderr.write((error.stack||error.message)+'\n');
  process.exitCode=1;
});
