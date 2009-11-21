import net.bull.javamelody.MonitoringFilter
import net.bull.javamelody.SessionListener

class GrailsMelodyGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def author = "Liu Chao"
    def authorEmail = "liuchao@goal98.com"
    def title = "Grails Java Melody Plugin"
    def description = '''\\
Integrate Java Melody Monitor into grails application.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/GrailsMelody+Plugin"

    def doWithSpring = {
        //TODO: monitoring of grails services
        /*monitoringAdvisor(MonitoringSpringAdvisor){
            pointcut = {MonitoredWithAnnotationPointcut bean->}
        }

        defaultAdvisorAutoProxyCreator(DefaultAdvisorAutoProxyCreator)*/

    }

    def doWithApplicationContext = {applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def doWithWebDescriptor = {xml ->

        def contextParam = xml.'context-param'

        contextParam[contextParam.size() - 1] + {
            'filter' {
                'filter-name'('monitoring')
                'filter-class'(MonitoringFilter.name)
            }
        }

        findMappingLocation.delegate = delegate
        def mappingLocation = findMappingLocation(xml)
        mappingLocation + {
            'filter-mapping' {
                'filter-name'('monitoring')
                'url-pattern'('/*')
            }
        }


        def filterMapping = xml.'filter-mapping'
        filterMapping[filterMapping.size() - 1] + {

            'listener' {
                'listener-class'(SessionListener.name)
            }
        }

    }

    private def findMappingLocation = {xml ->

        // find the location to insert the filter-mapping; needs to be after the 'charEncodingFilter'
        // which may not exist. should also be before the sitemesh filter.
        // thanks to the JSecurity plugin for the logic.

        def mappingLocation = xml.'filter-mapping'.find { it.'filter-name'.text() == 'charEncodingFilter' }
        if (mappingLocation) {
            return mappingLocation
        }

        // no 'charEncodingFilter'; try to put it before sitemesh
        int i = 0
        int siteMeshIndex = -1
        xml.'filter-mapping'.each {
            if (it.'filter-name'.text().equalsIgnoreCase('sitemesh')) {
                siteMeshIndex = i
            }
            i++
        }
        if (siteMeshIndex > 0) {
            return xml.'filter-mapping'[siteMeshIndex - 1]
        }

        if (siteMeshIndex == 0 || xml.'filter-mapping'.size() == 0) {
            def filters = xml.'filter'
            return filters[filters.size() - 1]
        }

        // neither filter found
        def filters = xml.'filter'
        return filters[filters.size() - 1]
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
