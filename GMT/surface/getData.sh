
curl  -o allWestCoast.csv.new 'http://www.seis.sc.edu/ears/stationLatLonBox.txt?minLat=30&maxLat=50&minLon=-126&maxLon=-114'
mv allWestCoast.csv.new allWestCoast.csv

curl -o TAresult.csv.new 'http://www.seis.sc.edu/ears_dev/stationList.txt?netdbid=62&gaussian=2.5'
mv TAresult.csv.new TAresult.csv

curl  -o allUS.csv.new 'http://www.seis.sc.edu/ears/stationLatLonBox.txt?minLat=23&maxLat=50&minLon=-126&maxLon=-60'
mv  allUS.csv.new allUS.csv

#cat allWestCoast.csv ../SNEP/SNEP.csv > tmp
#mv tmp allWestCoast.csv

