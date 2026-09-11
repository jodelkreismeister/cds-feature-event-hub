package com.sap.cds.feature.messaging.eventhub.service;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sap.cds.feature.messaging.eventhub.utils.EventHubErrorStatuses;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.impl.ContextualizedServiceException;
import com.sap.cds.services.impl.environment.SimplePropertiesProvider;
import com.sap.cds.services.messaging.utils.CloudEventUtils;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.utils.ErrorStatusException;

class EventHubMessagingServiceTest {
	private EventHubMessagingService svc;
	private CdsRuntime runtime;

	@BeforeEach 
	public void setUp() throws Exception {
		CdsProperties properties = new CdsProperties();
		CdsProperties.Messaging.MessagingServiceConfig config = new CdsProperties.Messaging.MessagingServiceConfig("cfg");
		config.setBinding("eb-mt-tests-eb");
		config.getOutbox().setEnabled(false);
		properties.getMessaging().getServices().put(config.getName(), config);
	
		CdsRuntimeConfigurer configurer = CdsRuntimeConfigurer.create(new SimplePropertiesProvider(properties));
		configurer.environmentConfigurations();
		configurer.serviceConfigurations();
		configurer.eventHandlerConfigurations();
		runtime = configurer.complete();
		svc = runtime.getServiceCatalog().getServices(EventHubMessagingService.class).findFirst().get();
	}

	@Test
	void testEmit_TenantNotSupported() {
		ContextualizedServiceException e = Assertions.assertThrows(ContextualizedServiceException.class, () -> emitMessage(runtime));
		assertEquals(EventHubErrorStatuses.EVENT_HUB_TENANT_CONTEXT_MISSING, e.getErrorStatus());
	}

	private void emitMessage(CdsRuntime runtime) {
		Map<String, Object> data = new HashMap<>();
		data.put("msg", "my msg 1");
		Map<String, Object> headers = new HashMap<>();
		headers.put("msg_header", "my header 1");
		svc.emit("sap.cdscpoc.myobject.myoperation.v1", data, headers);
	}

	@Test 
	void testSetCeSourceWithDefault() {
		final Map<String, Object> headers = new HashMap<>();
		svc.fillCeSource(headers, "someTenantId");
		assertEquals("someTenantId", headers.get(CloudEventUtils.KEY_SOURCE));
	}

	@Test 
	void testSetCeSourceKeepsFormerValue() {
		final Map<String, Object> headers = new HashMap<>();
		final String existingValue = svc.ceSource +'/'+ "existingSource";
		headers.put(CloudEventUtils.KEY_SOURCE, existingValue);
		svc.fillCeSource(headers, "someTenantId");
		assertEquals(existingValue, headers.get(CloudEventUtils.KEY_SOURCE));
	}

	@Test 
	void testSetCeSourceInvalidFormerValue() {
		final Map<String, Object> headers = new HashMap<>();
		headers.put(CloudEventUtils.KEY_SOURCE, "garbage");
		ErrorStatusException e = Assertions.assertThrows(ErrorStatusException.class, () -> svc.fillCeSource(headers, "someTenantId"));
		assertEquals(EventHubErrorStatuses.EVENT_HUB_EMIT_INVALID_CE_SOURCE, e.getErrorStatus());
	}
}
