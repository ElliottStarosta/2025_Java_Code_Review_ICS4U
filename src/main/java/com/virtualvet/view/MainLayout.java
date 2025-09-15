package com.virtualvet.view;

import com.virtualvet.view.ChatView;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

// @Push(transport = Transport.WEBSOCKET_XHR)
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
    }

    private void createHeader() {
        // Create logo container with better styling
        HorizontalLayout logoContainer = createLogo();
        
        // Create navigation/action buttons
        HorizontalLayout actionButtons = createActionButtons();
        
        // Main header layout
        HorizontalLayout header = new HorizontalLayout(logoContainer, actionButtons);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.Padding.Horizontal.LARGE,
                LumoUtility.Background.PRIMARY_10,
                LumoUtility.BoxShadow.SMALL
        );

        // Add subtle border bottom
        header.getStyle()
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("background", "linear-gradient(90deg, var(--lumo-primary-color-10pct), var(--lumo-success-color-10pct))")
                .set("backdrop-filter", "blur(10px)");

        addToNavbar(header);
    }

    private HorizontalLayout createLogo() {
        // App icon/logo
        Image logoIcon = new Image("images/icon-full.png","logo");
        
        logoIcon.getStyle()
            .set("margin-right", "1rem")
            .set("filter", "drop-shadow(0 2px 4px rgba(0,0,0,0.1))")
            .set("width", "150px")
            .set("height", "80px")
            .set("object-fit", "contain") 
            .set("object-position", "center");


        // App title
        // H1 title = new H1("Novavet");
        // title.addClassNames(
        //         LumoUtility.FontSize.XLARGE,
        //         LumoUtility.FontWeight.BOLD,
        //         LumoUtility.TextColor.PRIMARY,
        //         LumoUtility.Margin.NONE
        // );
        // title.getStyle()
        //         .set("font-family", "var(--lumo-font-family)")
        //         .set("letter-spacing", "-0.5px")
        //         .set("text-shadow", "0 1px 2px rgba(0,0,0,0.1)");

        // // Subtitle
        // Span subtitle = new Span("AI-Powered Pet Care Assistant");
        // subtitle.addClassNames(
        //         LumoUtility.FontSize.SMALL,
        //         LumoUtility.TextColor.SECONDARY,
        //         LumoUtility.FontWeight.MEDIUM
        // );
        // subtitle.getStyle().set("margin-left", "0.25rem");

        // Logo content layout
        // VerticalLayout logoContent = new VerticalLayout();
        // logoContent.setPadding(false);
        // logoContent.setSpacing(false);
        // logoContent.add(title, subtitle);

        // HorizontalLayout logoContainer = new HorizontalLayout(logoIcon, logoContent);
        HorizontalLayout logoContainer = new HorizontalLayout(logoIcon);
        logoContainer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        logoContainer.setSpacing(false);

        return logoContainer;
    }

    private HorizontalLayout createActionButtons() {
        // Help/Info button
        Button helpBtn = new Button(new Icon(VaadinIcon.QUESTION_CIRCLE));
        helpBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        helpBtn.setTooltipText("Help & Information");
        helpBtn.addClickListener(e -> showHelpDialog());
        helpBtn.getStyle()
                .set("margin-right", "0.5rem")
                .set("color", "var(--lumo-secondary-text-color)");

        // Emergency contact button with enhanced styling
        Icon emergencyIcon = new Icon(VaadinIcon.PHONE);
        emergencyIcon.getStyle().set("margin-right", "0.25rem");
        
        Button emergencyBtn = new Button("Emergency", emergencyIcon);
        emergencyBtn.addThemeVariants(
                ButtonVariant.LUMO_ERROR, 
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_SMALL
        );
        emergencyBtn.addClickListener(e -> triggerEmergencyFunctionality());
        emergencyBtn.setTooltipText("Find nearby emergency veterinarians");
        
        // Enhanced emergency button styling
        emergencyBtn.getStyle()
                .set("background", "linear-gradient(135deg, var(--lumo-error-color), var(--lumo-error-color-50pct))")
                .set("box-shadow", "0 2px 8px rgba(214, 51, 108, 0.3)")
                .set("border", "none")
                .set("font-weight", "600")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px")
                .set("transition", "all 0.2s ease");

        // Add hover effects
        emergencyBtn.getElement().addEventListener("mouseenter", e -> {
            emergencyBtn.getStyle()
                    .set("transform", "translateY(-1px)")
                    .set("box-shadow", "0 4px 12px rgba(214, 51, 108, 0.4)");
        });

        emergencyBtn.getElement().addEventListener("mouseleave", e -> {
            emergencyBtn.getStyle()
                    .set("transform", "translateY(0)")
                    .set("box-shadow", "0 2px 8px rgba(214, 51, 108, 0.3)");
        });

        HorizontalLayout actionButtons = new HorizontalLayout(helpBtn, emergencyBtn);
        actionButtons.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        actionButtons.setSpacing(false);
        
        return actionButtons;
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

    private void showHelpDialog() {
        Dialog helpDialog = new Dialog();
        helpDialog.setHeaderTitle("Novavet - Help & Information");
        helpDialog.setWidth("500px");
        helpDialog.setMaxWidth("90vw");

        // App icon and title
        HorizontalLayout titleLayout = new HorizontalLayout();
        Span appIcon = new Span("🐾");
        appIcon.addClassNames(LumoUtility.FontSize.XLARGE);
        H3 appTitle = new H3("Novavet");
        appTitle.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.NONE);
        titleLayout.add(appIcon, appTitle);
        titleLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        // Version and subtitle
        Span version = new Span("Version 1.0 - AI-Powered Pet Care Assistant");
        version.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.FontWeight.MEDIUM
        );

        // Features section
        H4 featuresTitle = new H4("What I Can Help With:");
        featuresTitle.addClassNames(LumoUtility.TextColor.PRIMARY);

        VerticalLayout featuresList = new VerticalLayout();
        featuresList.setPadding(false);
        featuresList.setSpacing(false);

        String[] features = {
                "🩺 Pet health concerns and symptom analysis",
                "📸 Image analysis of pet conditions (wounds, skin issues, etc.)",
                "🚨 Emergency triage and guidance",
                "📍 Find nearby emergency veterinary clinics",
                "💊 General veterinary advice and care tips",
                "🐕 Breed-specific health information",
                "🐈 Cat and dog behavior guidance"
        };

        for (String feature : features) {
            Div featureItem = new Div(new Span(feature));
            featureItem.getStyle()
                    .set("margin", "4px 0")
                    .set("padding-left", "8px");
            featuresList.add(featureItem);
        }

        // Important disclaimer
        Div disclaimer = new Div();
        disclaimer.getStyle()
                .set("background", "var(--lumo-error-color-10pct)")
                .set("border-left", "4px solid var(--lumo-error-color)")
                .set("padding", "12px")
                .set("border-radius", "4px")
                .set("margin", "16px 0");

        H5 disclaimerTitle = new H5("⚠️ Important Disclaimer");
        disclaimerTitle.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.Margin.NONE);
        
        Paragraph disclaimerText = new Paragraph(
                "Novavet is an AI assistant designed to provide general information and guidance. " +
                "It is NOT a replacement for professional veterinary care. For emergencies, serious " +
                "symptoms, or any concerns about your pet's health, please contact a licensed veterinarian immediately."
        );
        disclaimerText.getStyle().set("margin", "8px 0 0 0");
        
        disclaimer.add(disclaimerTitle, disclaimerText);

        // How to use section
        H4 howToTitle = new H4("How to Use:");
        howToTitle.addClassNames(LumoUtility.TextColor.PRIMARY);

        VerticalLayout usageList = new VerticalLayout();
        usageList.setPadding(false);
        usageList.setSpacing(false);

        String[] usageSteps = {
                "💬 Type your question or describe your pet's symptoms",
                "📤 Upload images if you have visible concerns",
                "🔍 Review the AI's analysis and recommendations",
                "🆘 Use the Emergency button for urgent situations",
                "📞 Contact a real veterinarian for serious issues"
        };

        for (String step : usageSteps) {
            Div stepItem = new Div(new Span(step));
            stepItem.getStyle()
                    .set("margin", "4px 0")
                    .set("padding-left", "8px");
            usageList.add(stepItem);
        }

        // Contact and support
        H4 supportTitle = new H4("Need More Help?");
        supportTitle.addClassNames(LumoUtility.TextColor.PRIMARY);

        Paragraph supportText = new Paragraph(
                "If you're experiencing technical issues or need additional support, " +
                "please ensure you have a stable internet connection and try refreshing the page."
        );

        // Main content layout
        VerticalLayout content = new VerticalLayout();
        content.add(
                titleLayout,
                version,
                new Hr(),
                featuresTitle,
                featuresList,
                disclaimer,
                howToTitle,
                usageList,
                supportTitle,
                supportText
        );
        content.setPadding(false);
        content.setSpacing(true);

        // Close button
        Button closeButton = new Button("Got it!", VaadinIcon.CHECK.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addClickListener(e -> helpDialog.close());

        // Add content and footer
        helpDialog.add(content);
        helpDialog.getFooter().add(closeButton);

        // Open the dialog
        helpDialog.open();
    }
}