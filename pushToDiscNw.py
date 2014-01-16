import urllib2

# in chrome execute document.cookie in the js console.
# update event id in url below
cookieVal = '__utmz=172297526.1356938963.3.1.utmccn=(direct)|utmcsr=(direct)|utmcmd=(none); __utma=172297526.21001595.1338514192.1356938963.1362378686.4; __utmc=172297526; bb2_screener_=1362379362+174.31.230.107; punbb_cookie=a%3A2%3A%7Bi%3A0%3Bs%3A4%3A%223699%22%3Bi%3A1%3Bs%3A32%3A%2204d7508101248f24d10397bb81d4003a%22%3B%7D; __utmb=172297526'
headers = { 'Cookie' : cookieVal, 'User-Agent' : 'DLev scheduling program' }
data = open('postData.txt', 'r').read()
url = 'http://www.discnw.org/ale/admin/ClubEventGameAdmin.html?clubEventId=669'
req = urllib2.Request(url, data, headers)
req.port = 80
response = urllib2.urlopen(req)
