import net.bull.javamelody.MonitoringFilter
import net.bull.javamelody.SessionListener
import net.bull.javamelody.ThreadInformations
import net.bull.javamelody.Parameter
import net.bull.javamelody.Parameters
import net.bull.javamelody.MonitoringProxy

class GrailsMelodyGrailsPlugin {
    // the plugin version
    def version = "0.2"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Liu Chao"
    def authorEmail = "liuchao@goal98.com"
    def title = "Grails Java Melody Plugin"
    def description = '''\\
Integrate Java Melody Monitor into grails application.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/GrailsMelody+Plugin"


    def doWithSpring = {
        //Wrap grails datasource with java melody JdbcWapper
        'grailsDataSourceBeanPostProcessor'(GrailsDataSourceBeanPostProcessor)

    }

    def doWithApplicationContext = {applicationContext ->

    }

    def doWithWebDescriptor = {xml ->

        def contextParam = xml.'context-param'

        contextParam[contextParam.size() - 1] + {
            'filter' {
                'filter-name'('monitoring')
                'filter-class'(MonitoringFilter.name)
                //load configuration from GrailsMelodyConfig.groovy
                def conf = GrailsMelodyUtil.grailsMelodyConfig?.javamelody
                conf?.each {
                    String name = it.key
                    String value = it.value
                    log.debug "Grails Melody Param: $name = $value"
                    'init-param' {
                        'param-name'(name)
                        'param-value'(value)
                    }
                }
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

    def doWithDynamicMethods = {ctx ->
        //Enable groovy meta programming
        ExpandoMetaClass.enableGlobally()
        //For each service class in Grails, the plugin use groovy meta programming (invokeMethod)
        //to 'intercept' method call and collect infomation for monitoring purpose.
        //The code below mimics 'MonitoringSpringInterceptor.invoke()'
        //TODO: Refactor the following codes to remove code duplication.
        application.serviceClasses.each {serviceArtifactClass ->
            def serviceClass = serviceArtifactClass.getClazz()
            serviceClass.metaClass.invokeMethod = {String name, args ->

                def SPRING_COUNTER = MonitoringProxy.getSpringCounter();
                final boolean DISABLED = Boolean.parseBoolean(Parameters.getParameter(Parameter.DISABLED));

                def metaMethod = delegate.metaClass.getMetaMethod(name, args)

                if (metaMethod) {

                    if (DISABLED || !SPRING_COUNTER.isDisplayed()) {
                        return metaMethod.doMethodInvoke(delegate, args)
                    }

                    final long start = System.currentTimeMillis();
                    final long startCpuTime = ThreadInformations.getCurrentThreadCpuTime();
                    final String requestName = "${serviceClass.name}.${name}";

                    boolean systemError = false;
                    try {
                        SPRING_COUNTER.bindContext(requestName, requestName, null, startCpuTime);
                        return metaMethod.doMethodInvoke(delegate, args)
                    } catch (final Error e) {
                        systemError = true;
                        throw e;
                    } finally {
                        final long duration = Math.max(System.currentTimeMillis() - start, 0);
                        final long cpuUsedMillis = (ThreadInformations.getCurrentThreadCpuTime() - startCpuTime) / 1000000;

                        SPRING_COUNTER.addRequest(requestName, duration, cpuUsedMillis, systemError, -1);
                    }

                } else {
                    throw new MissingMethodException(name, delegate.class, args)
                }

            }
        }
    }

    def onChange = {event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = {event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
