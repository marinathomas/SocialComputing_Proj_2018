#!/usr/bin/env bash
INPUT_PATH=demo/input_barbell
OUTPUT_PATH=demo/output

mkdir $OUTPUT_PATH

for i in `ps -ef|grep "java .* Initiator" | awk '{print $2}'`; do
    kill -9 "$i"
done
sleep 2
for i in `ps -ef|grep "java .* Node" | awk '{print $2}'`; do
    kill -9 "$i"
done
sleep 2
FILES=$(ls $INPUT_PATH | grep \\d.txt | sed -e 's/\.txt$//')
for i in $FILES
do
    java -cp bin Node $i $INPUT_PATH > ${OUTPUT_PATH}/${i}.out &
#    sleep 1
done
sleep 2
java -cp bin Initiator $INPUT_PATH $OUTPUT_PATH