

How to release
==============

- First resolve all SNAPSHOT-dependencies if there are any. You may
  explore that with mvn dependency:tree. Otherwise the release plugin
  will warn and refuse proceeding.

- Some modules don't compile yet without manual adding of propriatary
  jars. The are therefore commented out in de parent pom. Uncomment them and
  check that in.

- Checkout the mvn-repo in ../mvn-repo if you haven't already done so.
  (cd .. ; git clone git@github.com:ibissource/mvn-repo.git)

- mvn -Darguments="-DskipTests"  release:prepare
  It will ask for the version of the release (of every module). And do a
  suggestion. E.g. '5'. Accept the suggestions.
  It will also ask for the name of the tag, and suggest
  e.g. v5.0. That's fine.
  It will also ask for new development version and proposes one
  version higher e.g. '6.0-SNAPSHOT'.


- If everything sucessfull:
  mvn -Darguments="-DskipTests"  release:perform
  This will build again, and now deploy to your (local) repository
  ../mvn-repo

- Add the new files  in the mvn-repo and commit that
- Comment out the propriatary modules again in de parent pom
  (otherwise it won't easily build)
