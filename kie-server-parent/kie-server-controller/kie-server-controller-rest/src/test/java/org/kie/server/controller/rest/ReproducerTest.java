package org.kie.server.controller.rest;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.kie.server.controller.api.model.runtime.ServerInstanceKey;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.server.controller.api.storage.KieServerTemplateStorage;
import org.kie.server.controller.impl.storage.InMemoryKieServerTemplateStorage;

import static org.junit.Assert.fail;
import static org.kie.server.controller.rest.ControllerUtils.marshal;

public class ReproducerTest {

    private static final String serverTemplateId = "id";
    private KieServerTemplateStorage templateStorage = InMemoryKieServerTemplateStorage.getInstance();
    private CountDownLatch serverUp = new CountDownLatch(1);

    @Before
    public void setUp() {
        ServerTemplate serverTemplate = new ServerTemplate(serverTemplateId, "name");
        IntStream.range(0, 1000)
                .boxed()
                .map(i -> Integer.toString(i))
                .map(s -> new ServerInstanceKey(s, s, s, ""))
                .forEach(instance -> serverTemplate.addServerInstance(instance));
        templateStorage.store(serverTemplate);
    }

    @Test
    public void testConcurrency() {
        final ServerTemplate serverTemplate = templateStorage.load(serverTemplateId);
        marshal("application/xml", serverTemplate);

        Thread disconnect = new Thread(() -> disconnect());
        disconnect.start();

        // Sync before marshalling
        marshal("application/xml", serverTemplate);
    }

    public void disconnect() {
        ServerTemplate serverTemplate = templateStorage.load(serverTemplateId);

        try {
            for (ServerInstanceKey instanceKey : serverTemplate.getServerInstanceKeys()) {
                serverTemplate.deleteServerInstance(instanceKey.getServerInstanceId());
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
