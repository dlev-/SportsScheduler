import urllib2

# in chrome execute document.cookie in the js console.
# update event id in url below
cookieVal = ''
headers = { 'Cookie' : cookieVal, 'User-Agent' : 'DLev scheduling program' }
data = open('postData.txt', 'r').read()
url = 'http://www.discnw.org/ale/admin/ClubEventGameAdmin.html?clubEventId=669'
req = urllib2.Request(url, data, headers)
req.port = 80
response = urllib2.urlopen(req)
