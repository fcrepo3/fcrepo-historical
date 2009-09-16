#!/bin/sh

# Install libraries not in maven2 central repository

mvn install:install-file -Dfile=lib/activemq-all-5.1.0.jar -DgroupId=duraspace -DartifactId=activemq-all-5.1.0 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/akubra-core-0.1.jar -DgroupId=org.fedorarepo -DartifactId=akubra-core -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/akubra-fs-0.1.jar -DgroupId=org.fedorarepo -DartifactId=akubra-fs -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/akubra-map-0.1.jar -DgroupId=org.fedorarepo -DartifactId=akubra-map -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/akubra-mem-0.1.jar -DgroupId=org.fedorarepo -DartifactId=akubra-mem -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/axis-ant.jar -DgroupId=duraspace -DartifactId=axis-ant -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/axis.jar -DgroupId=org.fedorarepo -DartifactId=axis -Dversion=1.3-PATCHED -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/connector-1_5.jar -DgroupId=duraspace -DartifactId=connector-1_5 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/emory-util-2.1-remote.jar -DgroupId=org.fedorarep -DartifactId=emory-util-remote -Dversion=2.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/fop-hyph.jar -DgroupId=offo.hyphenation -DartifactId=fop-hyph -Dversion=1.2 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/google-collections-1.0-rc1.jar -DgroupId=duraspace -DartifactId=google-collections-1.0-rc1 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/GroboTestingJUnit-1.2.1-core.jar -DgroupId=duraspace -DartifactId=GroboTestingJUnit-1.2.1-core -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/howl-logger-0.1.11.jar -DgroupId=duraspace -DartifactId=howl-logger-0.1.11 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/ij.jar -DgroupId=duraspace -DartifactId=ij -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/jai_codec.jar -DgroupId=com.sun.media -DartifactId=jai_codec -Dversion=1.1.2_01 -Dpackaging=jar -DgeneratePom=false
mvn install:install-file -Dfile=lib/jai_core.jar -DgroupId=javax.media -DartifactId=jai_core -Dversion=1.1.2_01 -Dpackaging=jar -DgeneratePom=false
mvn install:install-file -Dfile=lib/jakarta-oro-2.0.5.jar -DgroupId=duraspace -DartifactId=jakarta-oro-2.0.5 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/jersey-bundle-1.0.1.jar -DgroupId=duraspace -DartifactId=jersey-bundle-1.0.1 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/jmx.jar -DgroupId=duraspace -DartifactId=jmx -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/joda-time-1.5.2.jar -DgroupId=duraspace -DartifactId=joda-time-1.5.2 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/jotm_jrmp_stubs-2.0.10.jar -DgroupId=duraspace -DartifactId=jotm_jrmp_stubs-2.0.10 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/jotm-2.0.10.jar -DgroupId=duraspace -DartifactId=jotm-2.0.10 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/jsp-api.jar -DgroupId=duraspace -DartifactId=jsp-api -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/jta-spec1_0_1.jar -DgroupId=duraspace -DartifactId=jta-spec1_0_1 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/junit-4.5.jar -DgroupId=duraspace -DartifactId=junit-4.5 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/lucene-core-2.4.0.jar -DgroupId=duraspace -DartifactId=lucene-core-2.4.0 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/md5.jar -DgroupId=duraspace -DartifactId=md5 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/mkjdbc.jar -DgroupId=duraspace -DartifactId=mkjdbc -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/mptstore-0.9.1.jar -DgroupId=duraspace -DartifactId=mptstore-0.9.1 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/mulgara-core-2.1.1.jar -DgroupId=org.fedorarepo -DartifactId=mulgara-core -Dversion=2.1.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/mysql-connector-java-5.1.6-bin.jar -DgroupId=duraspace -DartifactId=mysql-connector-java-5.1.6-bin -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/one-jar-boot-0.96.jar -DgroupId=org.fedorarepo -DartifactId=one-jar-boot -Dversion=0.96 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/openrdf-sesame-2.2.1-onejar.jar -DgroupId=duraspace -DartifactId=openrdf-sesame-2.2.1-onejar -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/postgresql-8.3-603.jdbc3.jar -DgroupId=duraspace -DartifactId=postgresql-8.3-603.jdbc3 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/saxon9-dom.jar -DgroupId=duraspace -DartifactId=saxon9-dom -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/saxon9.jar -DgroupId=duraspace -DartifactId=saxon9 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/servlet-api.jar -DgroupId=duraspace -DartifactId=servlet-api -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/sjsxp.jar -DgroupId=duraspace -DartifactId=sjsxp -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/sunxacml-patched.jar -DgroupId=duraspace -DartifactId=sunxacml-patched -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/Tidy.jar -DgroupId=duraspace -DartifactId=Tidy -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/trippi-1.4.1-core.jar -DgroupId=org.fedorarepo -DartifactId=trippi-core -Dversion=1.4.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/trippi-1.4.1-mptstore.jar -DgroupId=org.fedorarepo -DartifactId=trippi-mptstore -Dversion=1.4.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/trippi-1.4.1-mulgara.jar -DgroupId=org.fedorarepo -DartifactId=trippi-mulgara -Dversion=1.4.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/tt-bytecode.jar -DgroupId=duraspace -DartifactId=tt-bytecode -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=lib/xmlunit-1.2.jar -DgroupId=duraspace -DartifactId=xmlunit-1.2 -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
