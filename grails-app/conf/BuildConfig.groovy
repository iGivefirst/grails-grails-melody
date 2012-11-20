grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.dependency.resolution = {
	inherits( "global" )
	log "warn"
	repositories {
		grailsPlugins()
		grailsHome()
		mavenCentral()
	}
	dependencies {
		// change the javamelody version here to upgrade
		compile "net.bull.javamelody:javamelody-core:1.41.0"
		compile ("com.lowagie:itext:2.1.7") {excludes "bcmail-jdk14", "bcprov-jdk14", "bctsp-jdk14"}
		compile "org.jrobin:jrobin:1.5.9"
    compile "commons-codec:commons-codec:1.5"
    compile "commons-beanutils:commons-beanutils:1.8.3"
    compile "commons-lang:commons-lang:2.6"
	}
}
