/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.tmrk.enterprisecloud.xml;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import org.jclouds.crypto.Crypto;
import org.jclouds.date.internal.SimpleDateFormatDateService;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpResponse;
import org.jclouds.http.functions.ParseSax;
import org.jclouds.http.functions.ParseXMLWithJAXB;
import org.jclouds.logging.config.NullLoggingModule;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.rest.BaseRestClientTest;
import org.jclouds.rest.RestContextSpec;
import org.jclouds.rest.internal.RestAnnotationProcessor;
import org.jclouds.tmrk.enterprisecloud.domain.*;
import org.jclouds.tmrk.enterprisecloud.domain.VirtualMachine.VirtualMachineStatus;
import org.jclouds.tmrk.enterprisecloud.features.VirtualMachineAsyncClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Named;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Set;

import static org.jclouds.io.Payloads.newInputStreamPayload;
import static org.jclouds.rest.RestContextFactory.contextSpec;
import static org.jclouds.rest.RestContextFactory.createContextBuilder;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Tests behavior of JAXB parsing for AssignedIpAddresses
 * 
 * @author Jason King
 */
@Test(groups = "unit", testName = "AssignedIpAddressesJAXBParsingTest")
public class AssignedIpAddressesJAXBParsingTest extends BaseRestClientTest {
   private SimpleDateFormatDateService dateService;

  @BeforeMethod
  public void setUp() {
     dateService = new SimpleDateFormatDateService();
  }

   @BeforeClass
   void setupFactory() {
   RestContextSpec<String, Integer> contextSpec = contextSpec("test", "http://localhost:9999", "1", "", "userfoo",
        "credentialFoo", String.class, Integer.class,
        ImmutableSet.<Module> of(new MockModule(), new NullLoggingModule(), new AbstractModule() {

            @Override
            protected void configure() {}

            @SuppressWarnings("unused")
            @Provides
            @Named("exception")
            Set<String> exception() {
                throw new AuthorizationException();
            }

        }));

      injector = createContextBuilder(contextSpec).buildInjector();
      parserFactory = injector.getInstance(ParseSax.Factory.class);
      crypto = injector.getInstance(Crypto.class);
   }

   @Test
   public void testParseAssignedIpAddressesWithJAXB() throws Exception {

      Method method = VirtualMachineAsyncClient.class.getMethod("getAssignedIpAddresses", URI.class);
      HttpRequest request = factory(VirtualMachineAsyncClient.class).createRequest(method,new URI("/1"));
      assertResponseParserClassEquals(method, request, ParseXMLWithJAXB.class);

      Function<HttpResponse, AssignedIpAddresses> parser = (Function<HttpResponse, AssignedIpAddresses>) RestAnnotationProcessor
            .createResponseParser(parserFactory, injector, method, request);

      InputStream is = getClass().getResourceAsStream("/assignedIpAddresses.xml");
      AssignedIpAddresses addresses = parser.apply(new HttpResponse(200, "ok", newInputStreamPayload(is)));

      Assert.assertNotNull(addresses);
      Set<DeviceNetwork> deviceNetworks = addresses.getNetworks().getDeviceNetworks();
      assertEquals(1,deviceNetworks.size());
      DeviceNetwork network = Iterables.getOnlyElement(deviceNetworks);
      Set<String> ips = network.getIpAddresses().getIpAddresses();
      assertEquals(2,ips.size());
      assertTrue(ips.contains("10.146.204.98"));
      assertTrue(ips.contains("10.146.204.99"));
   }
}
