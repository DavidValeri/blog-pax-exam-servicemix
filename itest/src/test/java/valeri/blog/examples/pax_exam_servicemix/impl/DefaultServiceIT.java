/*
 * Copyright 2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package valeri.blog.examples.pax_exam_servicemix.impl;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

import valeri.blog.examples.pax_exam_servicemix.Service;

/**
 * A basic integration test that executes {@link DefaultServiceIT} in Apache ServiceMix.
 *
 * @author David Valeri
 */
@RunWith(PaxExam.class)
public class DefaultServiceIT {
    
    @Inject
    private BundleContext bundleContext;
    
    @Inject
    private ConfigurationAdmin configurationAdmin;

    @Configuration
    public Option[] config() {
        return new Option[] { 
                // Provision and launch a container based on a distribution of Karaf (Apache ServiceMix).
                karafDistributionConfiguration()
                    .frameworkUrl(
                             maven()
                                     .groupId("org.apache.servicemix")
                                     .artifactId("apache-servicemix")
                                     .type("zip")
                                     .version("4.5.2"))
                    .karafVersion("2.2.11")
                    .name("Apache ServiceMix")
                    .unpackDirectory(new File("target/pax"))
                    .useDeployFolder(false),
                // It is really nice if the container sticks around after the test so you can check the contents
                // of the data directory when things go wrong.
                keepRuntimeFolder(),
                // Don't bother with local console output as it just ends up cluttering the logs
                configureConsole().ignoreLocalConsole(),
                // Force the log level to INFO so we have more details during the test.  It defaults to WARN.
                logLevel(LogLevel.INFO),
                // Provision the example feature exercised by this test
                features(
                        "mvn:valeri.blog.examples.pax-exam-servicemix/pax-exam-servicemix-features/1.0.0-SNAPSHOT/xml/features",
                        "pax-exam-servicemix"),
                // Remember that the test executes in another process.  If you want to debug it, you need
                // to tell Pax Exam to launch that process with debugging enabled.  Launching the test class itself with
                // debugging enabled (for example in Eclipse) will not get you the desired results.
                //debugConfiguration("5000", true),
                };
    }

    @Test
    public void test() throws Exception {
        
        // Since PAX Exam doesn't provide any sort of proxying to the service reference injected using
        // @Inject, we do it ourselves here as the reconfiguration may result in a change to the service.
        // When using Blueprint w/ the configuration management service integration from Aries, this will
        // happen.
        
        ServiceReference serviceReference = bundleContext.getServiceReference(Service.class.getName());
        assertNotNull(serviceReference);
        
        Service service = (Service) bundleContext.getService(serviceReference);
        assertNotNull(service);
        try {
            assertEquals("Hello Bob.", service.sayHello("Bob"));
        } finally {
            bundleContext.ungetService(serviceReference);
        }
        
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(
                "valeri.blog.example.pax-exam-servicemix", null);
        
        assertNull(configuration.getProperties());
        
        Dictionary<String, Object> dict = new Hashtable<String, Object>();
        dict.put("hello", "Hola");
        
        configuration.update(dict);
        
        // Wait a little because the configuration event is asynchronous.
        Thread.sleep(2000l);
        
        
        serviceReference = bundleContext.getServiceReference(Service.class.getName());
        assertNotNull(serviceReference);
        
        service = (Service) bundleContext.getService(serviceReference);
        assertNotNull(service);
        try {
            assertEquals("Hola Bob.", service.sayHello("Bob"));
        } finally {
            bundleContext.ungetService(serviceReference);
        }
    }
}
