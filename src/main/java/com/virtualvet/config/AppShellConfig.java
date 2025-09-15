package com.virtualvet.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.shared.ui.Transport;
import org.springframework.stereotype.Component;

@Component
@Push(transport = Transport.WEBSOCKET_XHR)
@PWA(
    name = "NovaVet - AI Pet Care Assistant",
    shortName = "NovaVet",
    description = "AI-powered veterinary assistance and emergency pet care locator",
    backgroundColor = "#e8f5e8",
    themeColor = "#2e7d32",
    startPath = "/",
    display = "standalone",
    offlinePath = "offline.html"
)
public class AppShellConfig implements AppShellConfigurator {
    
}