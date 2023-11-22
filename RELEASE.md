Release on Windows 7 behind a proxy
===================================

- In Eclipse: change property application.version in file AppConstants.properties to X.Y (commit with "Prepare release X.Y" and push)
- In case a candidate release needs to be used:
    - In Eclipse:
        - Change versions in pom.xml files from X.Y-SNAPSHOT to X.Y-RC1-SNAPSHOT
        - Commit ("Prepare release X.Y") and Push
- Download and install http://git-scm.com/download/win (1.8.3 used at time of
  writing) in e.g. D:\Software\Installed\Git
- Download and install http://maven.apache.org/download.cgi (3.0.5 used at time
 of writing) in e.g. D:\Software\Installed\Maven\apache-maven-3.0.5
- Adjust D:\Software\Installed\Maven\apache-maven-3.0.5\conf\settings.xml with
  your proxy settings.
- Start cmd.exe
- Adjust Screen Buffer Size (Width: 140, Height: 9999) and Window Size
  (Width: 140, Height: 75)
- set JAVA_HOME=D:\Software\Installed\Java\jdk1.6.0_22 (from IAF v6.1-RC2, before JDK 1.5 was used)
- set PATH=D:\Software\Installed\Git\bin\;D:\Software\Installed\Maven\apache-maven-3.0.5\bin
- set MAVEN_OPTS=-Xmx512m -XX:MaxPermSize=128m
- git config --list
- Check and when needed set the following settings:
    - git config --global user.name "[first-name] [last-name]"
    - git config --global user.email [first-name].[last-name]@ibissource.org
    - git config --global http.proxy http://[proxy-username]:[proxy-password]@[proxy-host]:[proxy-port]
    - git config --global https.proxy http://[proxy-username]:[proxy-password]@[proxy-host]:[proxy-port]
    - git config --global core.autocrlf true
- cd D:\Temp (or any other folder)
- git clone https://github.com/frankframework/frankframework.git
- git clone https://github.com/ibissource/mvn-repo.git
- git clone https://[user]@bitbucket.org/ibissource/mvn-repo-proprietary.git
- cd iaf
- In case a branch needs to be created on a release X.Y:
    - git checkout -b bX.Y vX.Y
    - git push -u origin bX.Y
    - In Eclipse:
        - Team, Switch To, Other..., Remote Tracking, origin/bX.Y, Checkout...,
          Checkout as New Local Branch
        - Change versions in pom.xml files from X.Y-SNAPSHOT to X.Y.1-SNAPSHOT
        - Commit ("Prepare release X.Y.1") and Push
    - git pull
- mvn release:prepare -DpushChanges=false
- git push
- git push --tags
- mkdir target
- cd target
- git clone https://github.com/ibissource/mvn-repo.git
- git clone https://[user]@bitbucket.org/ibissource/mvn-repo-proprietary.git
- cd ..
- mvn release:perform
- cd target
- cd mvn-repo
- git add .
- git status
- git commit
    - i, Release ibis-adapterframework-parent-[version], esc, :, wq, enter
- git status
- git push
- In Eclipse:
  - Team, Pull
  - Wait for Building workspace to finish
  - Commit ("Finalise release X.Y") and Push the two changed org.eclipse.wst.common.component files
