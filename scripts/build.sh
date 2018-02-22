cd play
rm -rf svc/
mkdir src
sbt dist
unzip -d svc target/universal/arxiv-1.0.zip
mv svc/*/* svc/
rm svc/bin/*.bat
mv svc/bin/* svc/bin/start
cd ..
