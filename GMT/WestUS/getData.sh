
curl  -o allWestCoast.csv.new 'http://www.seis.sc.edu/ears/stationLatLonBox.txt?minLat=30&maxLat=50&minLon=-126&maxLon=-114'

curl -o TAresults.csv.new 'http://www.seis.sc.edu/ears_dev/stationList.txt?netdbid=62&gaussian=2.5'
