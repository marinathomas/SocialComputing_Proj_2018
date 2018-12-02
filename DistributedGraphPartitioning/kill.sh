for i in `ps -ef|grep "java .* Initiator" | awk '{print $2}'`; do
    kill -9 "$i"
done

for i in `ps -ef|grep "java .* Node" | awk '{print $2}'`; do
    kill -9 "$i"
done

