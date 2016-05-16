#!/bin/bash
#usage: distributed-stats host-file username password options samples run-id

hosts=$(cat $1)
numHosts=$(grep -c -x ".*" $1)
numSamples=$5
remoteTreePath=~/TreeFile_Astrid_new
popSize=1000
generations=40
runID=$6
timeout=$((24 * 60 * 60))
sleepTime=60

options=$4

username=$2
password=$3

sshwrap=$(cd $(dirname $0); pwd)"/sshwrap.sh"

echo "loaded " $numHosts " hosts in " $1
echo "working..."

cd ~/.distributed-jane

if [ -e "data/${runID}" ]
then
	echo "Error: There is already a run with ID ${runID}. If you wish to overwrite the existing run, you must remove the folder manually."
	echo "Terminating..."
fi

mkdir data/${runID}

mkdir data/${runID}/runs

echo $options > data/${runID}/options.txt;
echo $popSize > data/${runID}/popsize.txt
echo $generations > data/${runID}/generations.txt
cp $remoteTreePath data/${runID}/tree;
echo $remoteTreePath > data/${runID}/treepath.txt



# initialize some variables
h=0 #id of host
for host in $hosts
do
    timeLastJobStarted[$h]="-1"
    startNewRun[$h]="true"
    runsGivenTo[$h]="0"
    ((h++))
done


completed=0
pending=0

#until we have all the samples completed
while [ $completed -lt $numSamples ]
do
    h=0 #id of host
    for host in $hosts
    do
        #if we don't have a reason to start a new run on the host, see if we can find one
        if ! ${startNewRun[$h]}
        then

            #if it timed out, that's a good reason to start a new one
            if [ $(date +%s) -gt $((${timeLastJobStarted[$h]} + $timeout)) ]
            then
                echo "Run timed out on host " $host "."
                ((pending--))
                startNewRun[$h]="true"
            fi

            #if it finished, that's another good reason
            if [ -e "data/${runID}/runs/${h}_${runsGivenTo[$h]}.csv" ]
            then
                ((pending--))
                ((completed++))
                startNewRun[$h]="true"
                echo $completed " / " $numSamples " done"
            fi
        fi


        #if the host can be given another run and we need more runs done, and it is up, give it a run
        if ${startNewRun[$h]} && [ $(($pending+$completed)) -lt $numSamples ] && ping -c 1 $host > /dev/null 2> /dev/null
        then
            ((runsGivenTo[$h]++))
            ((pending++))

            cmd="cd ~/.distributed-jane; nohup nice -n 19 bin/jane-cli.sh -silent -p $popSize -i $generations -stats 1 $options -o data/${runID}/runs/${h}_${runsGivenTo[$h]}.csv $remoteTreePath > /dev/null 2> /dev/null < /dev/null &"

            ($sshwrap $host $username $password "$cmd") > /dev/null 2> /dev/null &

            timeLastJobStarted[$h]=$(date +%s)
            startNewRun[$h]="false"
        fi

        ((h++))
    done

    sleep $sleepTime
done

cd data/${runID}/runs

cat `ls` > ../${runID}.csv

cd ~/.distributed-jane; nohup nice -n 19 bin/jane-cli.sh -silent -p 1000 -i 40 -stats 1 -o data/superg/runs/c_0.csv ~/gopher_louse.tree > /dev/null 2> /dev/null < /dev/null &