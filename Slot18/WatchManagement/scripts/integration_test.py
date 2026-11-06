#!/usr/bin/env python3
"""Start four real JVMs and verify HTTP CRUD/Saga. SQL mode uses dedicated test databases only."""
import argparse, json, os, socket, subprocess, time, urllib.request, urllib.error
from pathlib import Path
from datetime import datetime
from zoneinfo import ZoneInfo
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--sql',action='store_true');p.add_argument('--port',type=int,default=18080);args=p.parse_args()
base=args.port; processes={}; logs=[]; checks=0
runtime=ROOT/'.runtime';runtime.mkdir(exist_ok=True)
env=os.environ.copy();env.update(JWT_SECRET='integration-only-jwt-key-at-least-32-bytes-long', INTERNAL_API_KEY='integration-only-internal-key', SAGA_FAULT_INJECTION='true', ACCOUNT_SERVICE_URL=f'http://localhost:{base+1}',BRAND_SERVICE_URL=f'http://localhost:{base+2}',WATCH_SERVICE_URL=f'http://localhost:{base+3}')
if args.sql:
 if not env.get('DB_PASSWORD'):
  values=dict(line.split('=',1) for line in (runtime/'sql-test.env').read_text().splitlines())
  env['DB_PASSWORD']=values['MSSQL_SA_PASSWORD'];env['DB_PORT']='11433'
 env['DB_HOST']=env.get('DB_HOST','localhost')
mods={'account':('MSAccount_de181019','ms-account',1),'brand':('MSBrand_de181019','ms-brand',2),'watch':('MSWatch_de181019','ms-watch',3),'gateway':('APIGateway_de181019','api-gateway',0)}
def launch(name):
 folder,artifact,offset=mods[name];log=open(runtime/f'{name}-integration.log','w');logs.append(log)
 cmd=['java','-Xmx256m','-jar',str(ROOT/folder/'target'/f'{artifact}-1.0.0.jar'),f'--server.port={base+offset}']
 if not args.sql and name!='gateway':cmd+=['--spring.profiles.active=test']
 if args.sql:cmd+=['--spring.jpa.show-sql=false','--saga.compensation.backoff-ms=10']
 processes[name]=subprocess.Popen(cmd,env=env,stdout=log,stderr=subprocess.STDOUT)
def wait(name):
 port=base+mods[name][2]
 for _ in range(180):
  if processes[name].poll() is not None:raise RuntimeError(f'{name} exited; read .runtime/{name}-integration.log')
  try:
   with socket.create_connection(('localhost',port),timeout=.2):return
  except OSError:time.sleep(.5)
 raise RuntimeError(f'{name} startup timed out')
def call(method,path,body=None,token=None,headers=None,port=None):
 h={'Content-Type':'application/json'}
 if token:h['Authorization']='Bearer '+token
 h.update(headers or {})
 req=urllib.request.Request(f'http://localhost:{port or base}'+path,data=None if body is None else json.dumps(body).encode(),headers=h,method=method)
 try:r=urllib.request.urlopen(req,timeout=65)
 except urllib.error.HTTPError as e:r=e
 raw=r.read();data=json.loads(raw) if raw else None
 return r.status,data,dict(r.headers)
def expect(method,path,status,body=None,token=None,headers=None,port=None):
 global checks
 r=call(method,path,body,token,headers,port)
 assert r[0]==status,(method,path,status,r)
 checks+=1;return r

def stop(name):
 proc=processes.get(name)
 if proc and proc.poll() is None:
  proc.terminate()
  try:proc.wait(15)
  except subprocess.TimeoutExpired:proc.kill();proc.wait()
try:
 for name in mods:launch(name)
 for name in mods:wait(name)
 for _ in range(60):
  try:
   r=call('GET','/api/watches')
   if r[0]==200 and len(r[1]['data'])>=10:break
  except (OSError,ValueError):pass
  time.sleep(.5)
 watches=expect('GET','/api/watches',200)[1]['data']
 assert all('waterResistance' in w and 'categoryName' in w and 'waterpMaterial' not in w for w in watches)
 expected=[('Submariner Date',1,1,'9550.00',50),('Speedmaster Moonwatch Professional',2,2,'7200.00',75),('Presage Cocktail Time',3,3,'425.00',200),('G-Shock DW-5600E-1V',4,4,'69.95',500),('Carrera Calibre Heuer 02',2,5,'5900.00',60),('Seamaster Diver 300M',1,2,'5400.00',120),('Prospex SPB143',1,3,'1200.00',150),('Daytona',2,1,'14550.00',30),('F-91W',4,4,'19.99',1000),("IWC Pilot's Watch Mark XVIII",5,4,'4500.00',80)]
 from decimal import Decimal
 for i,(name,cat,brand,price,stock) in enumerate(expected,1):
  w=next(x for x in watches if x['watchId']==i);assert (w['name'],w['categoryId'],w['brandId'],Decimal(str(w['price'])),w['stock'])==(name,cat,brand,Decimal(price),stock)
 def login(user):return expect('POST','/api/auth/login',200,{'username':user,'password':'@1'})[1]['data']
 auth=login('adminsys');admin=auth['accessToken'];staff=login('staff02')['accessToken'];member=login('member01')['accessToken']
 # Clean only watches created by an earlier interrupted run of this script.
 for w in watches:
  if w['watchId'] > 10 and w['name'] in ('Test Watch 01','12345678901'):
   expect('DELETE',f"/api/watches/{w['watchId']}",200,token=admin)
 watches=expect('GET','/api/watches',200)[1]['data'];assert len(watches)==10
 expect('POST','/api/auth/login',401,{'username':'adminsys','password':'wrong'})
 expect('POST','/api/auth/login',403,{'username':'member02','password':'@1'})
 expect('POST','/api/auth/login',403,{'username':'staff01','password':'@1'})
 expect('POST','/api/auth/login',400,{'username':'adminsys'})
 expect('POST','/api/auth/refresh',401,{'refreshToken':admin})
 expect('GET','/api/brands',401,token=auth['refreshToken'])
 expect('POST','/api/auth/refresh',200,{'refreshToken':auth['refreshToken']})
 today=datetime.now(ZoneInfo('Asia/Ho_Chi_Minh')).date().isoformat()
 body=dict(name='Test Watch 01',categoryId=1,brandId=1,caseMaterial='Steel',strapMaterial='Leather',waterResistance='100 meters',price=100,releaseDate=today,stock=0)
 expect('POST','/api/watches',401,body,headers={'X-User-Role':'1'})
 for token in [staff,member]:
  for method,path in [('POST','/api/watches'),('PUT','/api/watches/1'),('DELETE','/api/watches/1')]:expect(method,path,403,body,token,{'X-User-Role':'1'})
 expect('POST','/api/watches',401,body,headers={'X-User-Role':'1'},port=base+3)
 expect('PUT','/internal/brands/1/watches/99999',401,port=base+2)
 expect('PUT','/internal/brands/1/watches/99999',404,token=admin)
 expect('GET','/api/sagas',403,token=member)
 for overrides in [{'price':99.99},{'name':'1234567890'},{'releaseDate':'2000-01-01'},{'stock':-1},{'categoryId':999},{'price':100.001},{'name':'x'*256}]:expect('POST','/api/watches',400,body|overrides,admin)
 for field in body:expect('POST','/api/watches',400,{k:v for k,v in body.items() if k!=field},admin)
 def brand_ids(brand):return expect('GET',f'/api/brands/{brand}/watches',200,token=admin)[1]['data']['watchIds']
 assert [len(brand_ids(i)) for i in range(1,6)]==[2,2,2,3,1]
 def state(r):
  sid=next(v for k,v in r[2].items() if k.lower()=='x-saga-id')
  return expect('GET','/api/sagas/'+sid,200,token=admin)[1]['data']
 def snapshot():return expect('GET','/api/watches',200)[1]['data']
 def check_rollback(r,expected_steps):
  s=state(r);assert s['status']=='COMPENSATED',s
  steps=[x['stepName'] for x in s['steps'] if x['phase']=='COMPENSATE' and x['result']=='SUCCESS'];assert steps==expected_steps,steps
 r=expect('POST','/api/watches',201,body|{'name':'12345678901'},admin);wid=r[1]['data']['watchId'];assert snapshot()[0]['watchId']==wid and wid in brand_ids(1);assert state(r)['status']=='COMPLETED'
 original=snapshot()
 r=expect('POST','/api/watches',400,body|{'brandId':999},admin);check_rollback(r,['UNASSIGN_BRAND','DELETE_WATCH']);assert snapshot()==original
 r=expect('POST','/api/watches',500,body,admin,{'X-Saga-Fail-After-Step':'2'});check_rollback(r,['UNASSIGN_BRAND','DELETE_WATCH']);assert snapshot()==original
 r=expect('PUT',f'/api/watches/{wid}',200,body|{'brandId':2},admin);assert wid not in brand_ids(1) and wid in brand_ids(2)
 before=snapshot()
 r=expect('PUT',f'/api/watches/{wid}',400,body|{'brandId':999},admin);check_rollback(r,['ASSIGN_BRAND_BACK','RESTORE_WATCH']);assert snapshot()==before and wid in brand_ids(2)
 r=expect('PUT',f'/api/watches/{wid}',500,body|{'brandId':3},admin,{'X-Saga-Fail-After-Step':'2'});check_rollback(r,['ASSIGN_BRAND_BACK','RESTORE_WATCH']);assert snapshot()==before and wid not in brand_ids(3)
 r=expect('DELETE',f'/api/watches/{wid}',500,token=admin,headers={'X-Saga-Fail-After-Step':'1'});check_rollback(r,['ASSIGN_BRAND_BACK']);assert snapshot()==before and wid in brand_ids(2)
 # Network failure leaves a durable pending compensation; block further writes and recover.
 stop('brand')
 r=expect('POST','/api/watches',503,body,admin);s=state(r);assert s['status']=='COMPENSATING';pending=s['sagaId']
 expect('POST','/api/watches',409,body,admin)
 launch('brand');wait('brand')
 expect('POST',f'/api/sagas/{pending}/recover',200,token=admin);assert snapshot()==before
 expect('POST',f'/api/sagas/{pending}/recover',200,token=admin)
 r=expect('DELETE',f'/api/watches/{wid}',200,token=admin,headers={'X-Saga-Fail-After-Step':'2'});assert state(r)['status']=='COMPLETED';assert wid not in brand_ids(2)
 expect('DELETE',f'/api/watches/{wid}',404,token=admin)
 expect('PUT','/api/watches/999999',404,body,admin)
 assert len(snapshot())==10
 # Seed restart must preserve sequence IDs and never re-add deleted rows.
 if args.sql:
  stop('watch');launch('watch');wait('watch');assert len(snapshot())==10
 if os.getenv('RUN_POSTMAN') == '1':
  subprocess.run(['npx','--yes','newman','run',str(ROOT/'WatchManagement_Saga.postman_collection.json'),'--env-var',f'baseUrl=http://localhost:{base}','--bail'],check=True,env=env)
 print(f'PASS: {checks} HTTP assertions; seed, auth, validation, CRUD, compensation, outage/recovery, restart ({"SQL Server" if args.sql else "H2"}).',flush=True)
finally:
 for name in reversed(mods):stop(name)
 for log in logs:log.close()
