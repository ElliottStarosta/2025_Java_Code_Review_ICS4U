package com.virtualvet.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.vaadin.flow.theme.lumo.LumoUtility;

// @Push(transport = Transport.WEBSOCKET_XHR)
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
    }

    private void createHeader() {
        H1 logo = new H1("🐾 Virtual Vet");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.MEDIUM);

        // Emergency contact button
        Button emergencyBtn = new Button("🚨 Emergency", new Icon(VaadinIcon.PHONE));
        emergencyBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
         emergencyBtn.addClickListener(e -> {
            // This will trigger the emergency functionality in the current view
            triggerEmergencyFunctionality();
        });

        var header = new HorizontalLayout(logo, emergencyBtn);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void triggerEmergencyFunctionality() {
        // Get the current content (which should be ChatView)
        Component content = getContent();

        if (content instanceof ChatView) {
            ChatView chatView = (ChatView) content;
            // Call the emergency method directly
            chatView.findNearbyEmergencyVets();
        } else {
            // If not in ChatView, navigate to it and then trigger emergency
            getUI().ifPresent(ui -> {
                ui.navigate(ChatView.class);
                // Use a small delay to ensure navigation completes
                ui.getPage().executeJs("setTimeout(() => {"
                        + "const chatView = document.querySelector('chat-view');"
                        + "if (chatView && chatView.$server) {"
                        + "  chatView.$server.triggerEmergency();"
                        + "}"
                        + "}, 100);");
            });
        }
    }

}