

How to release
===

- First release all SNAPSHOT-dependencies
- Some modules don't compile yet without manual adding of propriatary
  jars. The are commented out in de parent pom. Uncomment them and
  check that in.

- Checkout the mvn-repo in ../mvn-repo
- mvn -Darguments="-DskipTests"  release:prepare
  It will ask for the version of the release (of every module). And do a
  suggestion. E.g. '5'. Accept the suggestions.
  It will also ask for the name of the tag, and suggest
  e.g. REL-5. That's fine.
  It will also ask for new development version and proposes something
  e.g. '6-SNAPSHOT'.


- If everything sucessfull:
  mvn -Darguments="-DskipTests"  release:perform
  This will build again, and now deploy to your (local) repository ../mvn-repo
- Add the new files  in the mvn-repo and commit that
- Comment out the propriatary modules again in de parent pom
  (otherwise it won't easily build)
