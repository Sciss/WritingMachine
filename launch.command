#!/bin/sh

# cool trick found in 'treebolic' ; DON'T USE FUNCTION
followlink()
{
	prg="$1"
	while [ -h "$prg" ] ; do
		ls=`ls -ld "$prg"`
		link=`expr "$ls" : '.*-> \(.*\)$'`
		if expr "$link" : '.*/.*' > /dev/null; then
			prg="$link"
		else
			prg=`dirname "$prg"`/"$link"
		fi
	done
	echo $prg
}

absdir() 
{ 
	[ -n "$1" ] && ( cd "$1" 2> /dev/null && pwd ; ) 
}

where=`followlink $0`
where=`dirname ${where}`
where=`absdir ${where}`
cd ${where}

echo "Launching WritingMachine..."
sleep 10
lastResult=1
while [ $lastResult -ge 1 ]
do
  sleep 4
  killall scsynth
  WritingMachine.app/Contents/MacOS/JavaApplicationStub
  lastResult=$?
done
# osascript -e 'tell application "Finder" to shut down'
