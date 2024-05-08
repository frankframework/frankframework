#!/bin/bash

# name of the module that war file will depend on
moduleName="module.frank-framework"

# list of jar files
path_to_jars="$JBOSS_HOME/standalone/lib/ext/"
jarList=$(ls $path_to_jars)

# resources that the module will be created upon
resources=""
resourceDelimiter=":"

for jarFile in $jarList; do
    resources+="$path_to_jars$jarFile$resourceDelimiter"
done

resources=${resources%?}

command="module add --name=$moduleName --resources=$resources"

echo $command

$JBOSS_HOME/bin/jboss-cli.sh --command="$command"
