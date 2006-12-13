
curl -m 900  -o allWestCoast.csv.new 'http://www.seis.sc.edu/ears/stationLatLonBox.txt?minLat=30&maxLat=50&minLon=-126&maxLon=-114'
rm  allWestCoast.csv
mv  allWestCoast.csv.new  allWestCoast.csv

curl -m 900 -o TAresult.csv.new 'http://www.seis.sc.edu/ears_dev/stationList.txt?netdbid=62&gaussian=2.5'

rm TAresult.csv
mv TAresult.csv.new TAresult.csv
