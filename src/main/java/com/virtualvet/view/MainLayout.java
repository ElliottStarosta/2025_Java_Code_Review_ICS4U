package com.virtualvet.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;


// @Push(transport = Transport.WEBSOCKET_XHR)
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("🐾 Virtual Vet");
        logo.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.MEDIUM
        );

        // Emergency contact button
        Button emergencyBtn = new Button("🚨 Emergency", new Icon(VaadinIcon.PHONE));
        emergencyBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        // emergencyBtn.addClickListener(e -> {
        //     getUI().ifPresent(ui -> ui.navigate(EmergencyView.class));
        // });

        var header = new HorizontalLayout(new DrawerToggle(), logo, emergencyBtn);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(
            LumoUtility.Padding.Vertical.NONE,
            LumoUtility.Padding.Horizontal.MEDIUM
        );

        addToNavbar(header);
    }

    private void createDrawer() {
        VerticalLayout drawerContent = new VerticalLayout();
        drawerContent.setSizeFull();
        drawerContent.setPadding(false);
        drawerContent.setSpacing(false);

        // App branding
        Div brandingSection = new Div();
        brandingSection.add(
            new H2("🐾 Virtual Vet"),
            new Paragraph("AI-Powered Pet Care Assistance")
        );
        brandingSection.addClassName("app-branding");
        brandingSection.getStyle()
            .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
            .set("color", "white")
            .set("padding", "2rem")
            .set("margin-bottom", "1rem")
            .set("border-radius", "0 0 1rem 0");

        // Navigation menu
        VerticalLayout navigation = new VerticalLayout();
        navigation.setPadding(false);
        navigation.setSpacing(false);
        
        // navigation.add(
        //     createNavItem(VaadinIcon.CHAT, "Chat", ChatView.class),
        //     createNavItem(VaadinIcon.USER_CARD, "Pet Profile", ProfileView.class),
        //     createNavItem(VaadinIcon.CAMERA, "Image Analysis", ImageAnalysisView.class),
        //     createNavItem(VaadinIcon.HOSPITAL, "Emergency", EmergencyView.class),
        //     createNavItem(VaadinIcon.CLOCK, "History", HistoryView.class)
        // );

        // Info section
        Div infoSection = new Div();
        infoSection.add(
            new H4("24/7 Support"),
            new Paragraph("Get instant veterinary guidance for your beloved pets."),
            new Paragraph("⚠️ This is not a replacement for professional veterinary care.")
        );
        infoSection.getStyle()
            .set("padding", "1rem")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-radius", "0.5rem")
            .set("margin", "1rem")
            .set("font-size", "0.875rem");

        drawerContent.add(brandingSection, navigation, infoSection);
        addToDrawer(drawerContent);
    }

    private RouterLink createNavItem(VaadinIcon iconType, String text, Class<? extends Component> navigationTarget) {
        Icon icon = new Icon(iconType);
        icon.getStyle().set("margin-inline-end", "0.5rem");
        
        RouterLink link = new RouterLink();
        link.add(icon, new Span(text));
        link.setRoute(navigationTarget);
        link.setHighlightCondition(HighlightConditions.sameLocation());
        
        link.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("padding", "0.75rem 1rem")
            .set("text-decoration", "none")
            .set("color", "var(--lumo-body-text-color)")
            .set("border-radius", "0.5rem")
            .set("margin", "0.25rem 1rem")
            .set("transition", "all 0.2s ease");
        
        link.getElement().addEventListener("mouseenter", e -> {
            link.getStyle()
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("transform", "translateX(0.25rem)");
        });
        
        link.getElement().addEventListener("mouseleave", e -> {
            link.getStyle()
                .set("background", "transparent")
                .set("transform", "translateX(0)");
        });
        
        return link;
    }
}