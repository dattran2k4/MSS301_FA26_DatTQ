import base64, hashlib, hmac, json, time, urllib.error, urllib.request
from datetime import datetime, timedelta

secret=b'fu-cinema-booking-system-secret-key-2026-mss301'
def token(uid,role):
    enc=lambda x:base64.urlsafe_b64encode(json.dumps(x,separators=(',',':')).encode()).rstrip(b'=').decode()
    part=enc({'alg':'HS256','typ':'JWT'})+'.'+enc({'sub':f'user{uid}@example.com','uid':uid,'role':role,'iat':int(time.time()),'exp':int(time.time())+3600})
    return part+'.'+base64.urlsafe_b64encode(hmac.new(secret,part.encode(),hashlib.sha256).digest()).rstrip(b'=').decode()
admin=token(0,'ADMIN'); cus=token(1,'CUSTOMER'); cus2=token(2,'CUSTOMER')
results=[]
def call(name,method,path,expected,body=None,jwt=None,headers=None):
    h={'Content-Type':'application/json',**(headers or {})}
    if jwt:h['Authorization']='Bearer '+jwt
    req=urllib.request.Request('http://localhost:9000'+path,method=method,headers=h,data=json.dumps(body).encode() if body is not None else None)
    try:
        with urllib.request.urlopen(req,timeout=10) as r:status=r.status;raw=r.read()
    except urllib.error.HTTPError as e:status=e.code;raw=e.read()
    data=json.loads(raw) if raw else None
    results.append({'case':name,'expected':expected,'actual':status,'pass':status==expected})
    return data
stamp=str(int(time.time()))
future=(datetime.now()+timedelta(days=7)).strftime('%Y-%m-%d')+'T19:00:00'
call('public genres','GET','/api/genres',200)
call('rooms require token','GET','/api/rooms',401)
call('customer cannot create genre','POST','/api/genres',403,{'genreName':'Test'},cus)
genre=call('create genre','POST','/api/genres',201,{'genreName':'Smoke '+stamp},admin)
gid=genre['genreId']
call('duplicate genre','POST','/api/genres',409,{'genreName':'Smoke '+stamp},admin)
room=call('create room','POST','/api/rooms',201,{'roomName':'Smoke '+stamp,'roomType':'STANDARD','seatRows':5,'seatsPerRow':8,'roomStatus':'ACTIVE'},admin)
rid=room['roomId']
movie=call('create movie','POST','/api/movies',201,{'title':'Smoke Test','durationMinutes':120,'ageRating':'P','genreId':gid,'movieStatus':'NOW_SHOWING'},admin)
mid=movie['movieId']
call('invalid genre reference','POST','/api/movies',404,{'title':'Missing Genre','durationMinutes':120,'ageRating':'P','genreId':'000000000000000000000000','movieStatus':'NOW_SHOWING'},admin)
show=call('create showtime','POST','/api/showtimes',201,{'movieId':mid,'roomId':rid,'startTime':future,'ticketPrice':95000},admin)
sid=show['showtimeId']
call('reject overlap','POST','/api/showtimes',409,{'movieId':mid,'roomId':rid,'startTime':future,'ticketPrice':95000},admin)
call('seat map empty','GET',f'/api/bookings/showtimes/{sid}/seats',200)
booking=call('book 2 seats','POST','/api/bookings',201,{'items':[{'showtimeId':sid,'seatCode':'A1'},{'showtimeId':sid,'seatCode':'A2'}]},cus)
bid=booking['bookingId'] if booking and 'bookingId' in booking else 0
call('reject taken seat','POST','/api/bookings',409,{'items':[{'showtimeId':sid,'seatCode':'A1'}]},cus2)
call('reject invalid seat','POST','/api/bookings',400,{'items':[{'showtimeId':sid,'seatCode':'F1'}]},cus2)
call('other customer cannot view','GET',f'/api/bookings/{bid}',403,jwt=cus2)
call('customer history','GET','/api/bookings/my',200,jwt=cus)
call('admin report','GET','/api/bookings/report?startDate='+datetime.now().strftime('%Y-%m-%d')+'&endDate='+datetime.now().strftime('%Y-%m-%d'),200,jwt=admin)
call('customer cannot report','GET','/api/bookings/report?startDate=2026-01-01&endDate=2026-12-31',403,jwt=cus)
call('cancel booking','PUT',f'/api/bookings/{bid}/cancel',200,jwt=cus)
call('rebook released seat','POST','/api/bookings',201,{'items':[{'showtimeId':sid,'seatCode':'A1'}]},cus2)
call('reject spoofed role','POST','/api/rooms',403,{'roomName':'Spoof','roomType':'STANDARD','seatRows':1,'seatsPerRow':1,'roomStatus':'ACTIVE'},cus,{'X-User-Role':'ADMIN'})
print(json.dumps({'pass':sum(x['pass'] for x in results),'total':len(results),'cases':results},indent=2))
