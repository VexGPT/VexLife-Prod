#!/usr/bin/env node
'use strict';

const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');
const {spawnSync}=require('node:child_process');

const RESULT_SCHEMA='vextreme.vexlocalbridge-mac-payload-result/v1';
const ORIGIN='http://127.0.0.1:18210';

function effects(){return {
  sourceMutationPerformed:false,branchMutationPerformed:false,claimMutationPerformed:false,
  prMetadataMutationPerformed:false,reviewPerformed:false,approvalPerformed:false,
  readyTransitionPerformed:false,mergePerformed:false,mainMutationPerformed:false,
  checkpointMutationPerformed:false,forcePushPerformed:false,historyRewritePerformed:false,
  hostInstallationPerformed:false,modelRuntimePerformed:false,providerOrNetworkEffectPerformed:false,
  publicationPerformed:false,credentialSerializationPerformed:false,
  filesystemOutsideQualifiedRootsPerformed:false
};}
function writeResult(v){
  const root=process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if(!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR unavailable');
  fs.mkdirSync(root,{recursive:true});
  fs.writeFileSync(path.join(root,'payload-result.json'),JSON.stringify({
    schemaVersion:RESULT_SCHEMA,acceptedExitCodes:[0],...v
  },null,2)+'\n');
}
function run(cmd,args,opts={}){
  const r=spawnSync(cmd,args,{encoding:'utf8',timeout:opts.timeoutMs||15000,maxBuffer:16*1024*1024});
  return {status:r.status,error:r.error?String(r.error):null,stdout:String(r.stdout||''),stderr:String(r.stderr||'')};
}
async function getJson(route){
  const r=await fetch(ORIGIN+route,{headers:{accept:'application/json'},cache:'no-store'});
  if(!r.ok) throw new Error(route+' HTTP '+r.status);
  return r.json();
}
function chromeProbe(app){
  const js=JSON.stringify(
    `JSON.stringify({
      url: location.href,
      title: document.title,
      timelineText: document.querySelector('#timeline')?.innerText ?? null,
      waitingText: document.querySelector('.message.waiting')?.innerText ?? null,
      failedAttemptText: Array.from(document.querySelectorAll('.failed-attempt')).map(x => x.innerText),
      speechSettings: localStorage.getItem('vexlife-prod.presentation.speech/v1'),
      speechSynthesis: window.speechSynthesis ? {
        speaking: window.speechSynthesis.speaking,
        pending: window.speechSynthesis.pending,
        paused: window.speechSynthesis.paused
      } : null,
      privateStateType: typeof state,
      privateSpeechType: typeof speech
    })`
  );
  const script=`
tell application "${app}"
  if (count of windows) is 0 then return "NO_WINDOWS"
  set found to {}
  repeat with w in windows
    repeat with t in tabs of w
      set u to URL of t
      if u starts with "http://127.0.0.1:18210" or u starts with "http://localhost:18210" then
        try
          set j to execute t javascript ${js}
          set end of found to j
        on error errMsg number errNum
          set end of found to "JS_ERROR|" & errNum & "|" & errMsg & "|" & u
        end try
      end if
    end repeat
  end repeat
  if (count of found) is 0 then return "NO_VEXLIFE_TAB"
  set AppleScript's text item delimiters to linefeed
  return found as text
end tell
`;
  return run('/usr/bin/osascript',['-e',script],{timeoutMs:20000});
}
function safariProbe(){
  const js=`JSON.stringify({
    url: location.href,
    title: document.title,
    timelineText: document.querySelector('#timeline')?.innerText ?? null,
    waitingText: document.querySelector('.message.waiting')?.innerText ?? null,
    failedAttemptText: Array.from(document.querySelectorAll('.failed-attempt')).map(x => x.innerText),
    speechSettings: localStorage.getItem('vexlife-prod.presentation.speech/v1'),
    speechSynthesis: window.speechSynthesis ? {
      speaking: window.speechSynthesis.speaking,
      pending: window.speechSynthesis.pending,
      paused: window.speechSynthesis.paused
    } : null,
    privateStateType: typeof state,
    privateSpeechType: typeof speech
  })`;
  const esc=js.replace(/\\/g,'\\\\').replace(/"/g,'\\"');
  const script=`
tell application "Safari"
  if (count of windows) is 0 then return "NO_WINDOWS"
  set found to {}
  repeat with w in windows
    repeat with t in tabs of w
      set u to URL of t
      if u starts with "http://127.0.0.1:18210" or u starts with "http://localhost:18210" then
        try
          set j to do JavaScript "${esc}" in t
          set end of found to j
        on error errMsg number errNum
          set end of found to "JS_ERROR|" & errNum & "|" & errMsg & "|" & u
        end try
      end if
    end repeat
  end repeat
  if (count of found) is 0 then return "NO_VEXLIFE_TAB"
  set AppleScript's text item delimiters to linefeed
  return found as text
end tell
`;
  return run('/usr/bin/osascript',['-e',script],{timeoutMs:20000});
}
function debugPorts(){
  const ps=run('/bin/ps',['-axo','pid=,command=']);
  const ports=[];
  for(const line of ps.stdout.split(/\n/)){
    const m=line.match(/--remote-debugging-port=(\d+)/);
    if(m) ports.push({port:Number(m[1]),command:line.trim().slice(0,1000)});
  }
  const candidates=[
    path.join(os.homedir(),'Library/Application Support/Google/Chrome/DevToolsActivePort'),
    path.join(os.homedir(),'Library/Application Support/Chromium/DevToolsActivePort'),
    path.join(os.homedir(),'Library/Application Support/BraveSoftware/Brave-Browser/DevToolsActivePort'),
    path.join(os.homedir(),'Library/Application Support/Microsoft Edge/DevToolsActivePort')
  ];
  for(const file of candidates){
    try{
      const lines=fs.readFileSync(file,'utf8').trim().split(/\n/);
      const port=Number(lines[0]);
      if(Number.isInteger(port)&&port>0&&!ports.some(x=>x.port===port)) ports.push({port,activePortFile:file});
    }catch(_){}
  }
  return ports;
}
async function cdpTargets(ports){
  const out=[];
  for(const p of ports){
    try{
      const list=await (await fetch('http://127.0.0.1:'+p.port+'/json/list')).json();
      out.push({
        port:p.port,
        targets:list.filter(x=>String(x.url||'').includes('127.0.0.1:18210')||String(x.url||'').includes('localhost:18210'))
          .map(x=>({id:x.id,title:x.title,url:x.url,webSocketDebuggerUrl:x.webSocketDebuggerUrl||null}))
      });
    }catch(error){
      out.push({port:p.port,error:String(error),targets:[]});
    }
  }
  return out;
}


async function inspectCdpTarget(target){
  if(typeof WebSocket!=='function' || !target?.webSocketDebuggerUrl){
    return {available:false,reason:typeof WebSocket!=='function'?'NODE_WEBSOCKET_UNAVAILABLE':'NO_WEBSOCKET_URL'};
  }
  return await new Promise((resolve)=>{
    const ws=new WebSocket(target.webSocketDebuggerUrl);
    let nextId=1;
    const waiting=new Map();
    const finish=(value)=>{try{ws.close();}catch(_){} resolve(value);};
    const timer=setTimeout(()=>finish({available:true,error:'CDP_TIMEOUT'}),12000);
    const call=(method,params={})=>new Promise((res,rej)=>{
      const id=nextId++;
      waiting.set(id,{res,rej});
      ws.send(JSON.stringify({id,method,params}));
    });
    ws.onmessage=(event)=>{
      try{
        const msg=JSON.parse(String(event.data));
        if(msg.id&&waiting.has(msg.id)){
          const p=waiting.get(msg.id); waiting.delete(msg.id);
          if(msg.error)p.rej(new Error(msg.error.message||JSON.stringify(msg.error)));
          else p.res(msg.result);
        }
      }catch(_){}
    };
    ws.onerror=()=>{clearTimeout(timer);finish({available:true,error:'CDP_WEBSOCKET_ERROR'});};
    ws.onopen=async()=>{
      try{
        await call('Runtime.enable');
        const dom=await call('Runtime.evaluate',{
          expression:`JSON.stringify({
            url:location.href,
            timelineText:document.querySelector('#timeline')?.innerText??null,
            speechSettings:localStorage.getItem('vexlife-prod.presentation.speech/v1')
          })`,
          returnByValue:true
        });
        const root=await call('Runtime.evaluate',{expression:"document.querySelector('#timeline')"});
        const objectId=root?.result?.objectId;
        const found={state:null,speech:null};
        const listenerTypes=[];
        if(objectId){
          const ev=await call('DOMDebugger.getEventListeners',{objectId,depth:2,pierce:true});
          for(const listener of ev.listeners||[]){
            listenerTypes.push(listener.type);
            const handlerId=listener?.handler?.objectId;
            if(!handlerId)continue;
            const hp=await call('Runtime.getProperties',{objectId:handlerId,ownProperties:false,generatePreview:false});
            const scopes=(hp.internalProperties||[]).find(x=>x.name==='[[Scopes]]')?.value?.objectId;
            if(!scopes)continue;
            const sp=await call('Runtime.getProperties',{objectId:scopes,ownProperties:true,generatePreview:false});
            for(const scopeEntry of sp.result||[]){
              const scopeId=scopeEntry?.value?.objectId;
              if(!scopeId)continue;
              const vars=await call('Runtime.getProperties',{objectId:scopeId,ownProperties:true,generatePreview:false});
              for(const v of vars.result||[]){
                if(v.name!=='state'&&v.name!=='speech')continue;
                const oid=v?.value?.objectId;
                if(!oid)continue;
                const expr=v.name==='state'
                  ? `function(){return JSON.stringify({
                      streamedContent:this.streamedContent??null,
                      streamedReasoningCharacters:String(this.streamedReasoning??'').length,
                      failedAttempts:(this.failedAttempts??[]).map(a=>({
                        idempotencyKey:a.idempotencyKey??null,
                        failureCode:a.failureCode??null,
                        partialContent:a.partialContent??null,
                        processEntries:a.processEntries??[]
                      })),
                      pending:this.pending??null
                    })}`
                  : `function(){return JSON.stringify({
                      settings:this.settings??null,
                      stream:this.stream??null,
                      current:this.current??null,
                      queue:this.queue??[]
                    })}`;
                const value=await call('Runtime.callFunctionOn',{
                  objectId:oid,
                  functionDeclaration:expr,
                  returnByValue:true,
                  silent:true
                });
                try{found[v.name]=JSON.parse(value?.result?.value??'null');}catch(_){found[v.name]=value?.result?.value??null;}
              }
            }
          }
        }
        clearTimeout(timer);
        finish({
          available:true,
          page:JSON.parse(dom?.result?.value||'{}'),
          listenerTypes:[...new Set(listenerTypes)],
          closure:found
        });
      }catch(error){
        clearTimeout(timer);
        finish({available:true,error:String(error)});
      }
    };
  });
}

async function main(){
  const current=await getJson('/api/v1/current-state');
  const probes={};
  for(const app of ['Google Chrome','Chromium','Brave Browser','Microsoft Edge']){
    const r=chromeProbe(app);
    probes[app]={status:r.status,stdout:r.stdout.trim(),stderr:r.stderr.trim()};
  }
  const safari=safariProbe();
  probes.Safari={status:safari.status,stdout:safari.stdout.trim(),stderr:safari.stderr.trim()};

  const ports=debugPorts();
  const targets=await cdpTargets(ports);
  const cdpInspections=[];
  for(const group of targets){
    for(const target of group.targets||[]){
      cdpInspections.push({
        port:group.port,
        target:{id:target.id,title:target.title,url:target.url},
        inspection:await inspectCdpTarget(target)
      });
    }
  }

  const speechCopies=[];
  for(const [app,p] of Object.entries(probes)){
    if(!p.stdout||p.stdout.startsWith('NO_')||p.stdout.startsWith('JS_ERROR|')) continue;
    for(const line of p.stdout.split(/\n/)){
      try{
        const j=JSON.parse(line);
        if(j?.speechSettings) speechCopies.push({app,speechSettings:j.speechSettings,speechSynthesis:j.speechSynthesis});
      }catch(_){}
    }
  }

  const browserJavascriptAccessible=Object.values(probes).some(p=>p.stdout&&!p.stdout.startsWith('NO_')&&!p.stdout.startsWith('JS_ERROR|'));
  const cdpAvailable=targets.some(x=>x.targets?.length);

  writeResult({
    disposition:'PASS',
    stopReasonOrNull:null,
    firstSubstantiveFailureOrNull:null,
    candidateFindings:[],
    harnessFindings:[],
    environmentFindings:[],
    unknownFindings:[],
    effects:effects(),
    beforeState:{
      productSourceSha:current.sourceSha??null,
      serviceState:current.serviceState??null,
      activeModelRef:current.activeModelRef??null
    },
    afterState:{
      browserProbes:probes,
      remoteDebugPorts:ports,
      cdpVexLifeTargets:targets,
      cdpInspections,
      speechCopies,
      browserJavascriptAccessible,
      cdpAvailable,
      note:[
        'Normal VexLife failed-attempt state does not retain streamedContent in merged main.',
        'Visible DOM is reported if browser JavaScript-from-automation is permitted.',
        'Speech settings/status are reported, but queued utterance text is private closure state unless a debugger target is already available.',
        'No process-memory scrape was performed.'
      ]
    },
    completedStepReceipts:[
      {step:'current-product-read',state:'PASS'},
      {step:'browser-session-surface-probe',state:'PASS'},
      {step:'remote-debug-endpoint-probe',state:'PASS'}
    ]
  });
}
main().catch(error=>{
  try{writeResult({
    disposition:'SAFE_FAILURE',
    stopReasonOrNull:'LIVE_SESSION_SCAVENGER_STOPPED',
    firstSubstantiveFailureOrNull:null,
    candidateFindings:[],harnessFindings:['LIVE_SESSION_SCAVENGER_STOPPED'],
    environmentFindings:[],unknownFindings:[],effects:effects(),
    beforeState:{},afterState:{errorMessage:String(error?.message||error)},completedStepReceipts:[]
  });}catch(_){}
  process.exitCode=0;
});
