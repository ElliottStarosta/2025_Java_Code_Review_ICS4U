package com.virtualvet.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;
import org.springframework.stereotype.Component;

@Component
@Push(transport = Transport.WEBSOCKET_XHR)
public class AppShellConfig implements AppShellConfigurator {

}
