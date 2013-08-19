
curl  -o allUS.csv.new 'http://www.seis.sc.edu/ears/stationLatLonBox.txt?minLat=23&maxLat=50&minLon=-126&maxLon=-60'

curl -o TAresult.csv.new 'http://www.seis.sc.edu/ears_dev/stationList.txt?netdbid=62&gaussian=2.5'
