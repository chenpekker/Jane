#!/bin/bash
#usage: grid-stats options samples run-id
#for use on Sun Grid Engine clusters (eg. Prof. Bush's Altanius)
#this works, but don't use it if anyone else needs to run jobs on the cluster

options=$1
numSamples=$2
remoteTreePath=~/TreeFile_Astrid_new
runID=$3

$sleepTime=15
$maxPending=4

echo "submitting tasks to Sun Grid Engine..."
cd ~/.distributed-jane/data;
mkdir $runID
cd $runID;
echo $options > options.txt;
cp $remoteTreePath tree;
echo $remoteTreePath > treepath.txt

mkdir runs;
cd runs;

i=0

#until we have all the samples submitted
while [ $i -lt $numSamples ]
do
qsub -cwd -o /dev/null -e /dev/null -V -b y -p -1000 bash nice -n 19 ~/.distributed-jane/bin/jane-cli.sh -stats 1 $options -o ${runID}_${i}.csv $remoteTreePath
((i++))
done

echo "all done"





startNewRun="true"

completed=0
pending=0

#until we have all the samples completed
while [ $completed -lt $numSamples ]
do
    #if we don't have a reason to start a new run on the host, see if we can find one
    if ! ${startNewRun}
    then

        #if it finished, that's a good reason
        if [ -e "${runID}_.csv" ]
        then
            ((pending--))
            ((completed++))
            startNewRun="true"
            echo $completed " / " $numSamples " done"
        fi
    fi


    #if the host can be given another run and we need more runs done, and it is up, give it a run
    if ${startNewRun[$h]} && [ $(($pending+$completed)) -lt $numSamples ] && ping -c 1 $host > /dev/null 2> /dev/null
    then
        ((runsGivenTo[$h]++))
        ((pending++))

        cmd="cd ~/.distributed-jane; nohup nice -n 19 bin/jane-cli.sh -silent -p $popSize -i $generations -stats 1 -c -1 0 0 0 -T -B -1 -o data/${runID}/${h}_${runsGivenTo[$h]}.csv $remoteTreePath > /dev/null 2> /dev/null < /dev/null &"

        ($sshwrap $host $username $password "$cmd") > /dev/null 2> /dev/null &

        timeLastJobStarted[$h]=$(date +%s)
        startNewRun[$h]="false"
    fi

    sleep $sleepTime
done

cd data/${runID}

cat `ls` > ../${runID}.csv