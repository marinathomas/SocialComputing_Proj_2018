Source code: DistributedGraphPartitioning/src
Compiled class file: DistributedGraphPartitioning/bin
Example input data: DistributedGraphPartitioning/demo
Expected output folder: DistributedGraphPartitioning/output
On Linux bash command line, sh DistributedGraphPartitioning/run.sh will start the processes with the input files specified in the script.
When the output has stopped, sh kill.sh to stop all the processes from listening for more messages.

in DistributedGraphPartitioning/demo, run 
python createInput.py to convert output files to json format. run
python -m SimpleHTTPServer 18000 to start local web server to host the d3.js graph render script.

in any browser go to 
localhost:18000/graph0.html to display the network graph in initial state;
localhost:18000/graph1.html to display the network graph after the first round, and so on.

