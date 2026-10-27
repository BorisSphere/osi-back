package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.security.MailGraphProperties;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/** Envío de correo transaccional usando OAuth2 client credentials y Microsoft Graph sendMail. */
@Component
public class MicrosoftGraphMailSender {

    private static final String GRAPH_URL = "https://graph.microsoft.com/v1.0";

    private final MailGraphProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public MicrosoftGraphMailSender(MailGraphProperties properties) {
        this.properties = properties;
    }

    public void enviar(String destinatario, String asunto, String html) {
        MailGraphProperties.Graph graph = properties.graph();
        if (isBlank(graph.tenantId()) || isBlank(graph.clientId()) || isBlank(graph.clientSecret())
                || isBlank(properties.from())) {
            throw new IllegalStateException(
                    "Falta configurar MAIL_TENANT_ID, MAIL_CLIENT_ID, MAIL_CLIENT_SECRET o MAIL_FROM");
        }

        String token = obtenerToken(graph);
        GraphMessage mensaje = new GraphMessage(
                asunto,
                new GraphBody("HTML", html),
                List.of(new GraphRecipient(new GraphEmailAddress(destinatario))));
        try {
            restClient.post()
                    .uri(GRAPH_URL + "/users/" + UriUtils.encodePathSegment(properties.from(), StandardCharsets.UTF_8)
                            + "/sendMail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(token))
                    .body(new SendMailRequest(mensaje, true))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Microsoft Graph rechazó el envío (HTTP "
                    + e.getStatusCode().value() + ")", e);
        }
    }

    private String obtenerToken(MailGraphProperties.Graph graph) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", graph.clientId());
        form.add("client_secret", graph.clientSecret());
        form.add("scope", "https://graph.microsoft.com/.default");
        form.add("grant_type", "client_credentials");
        TokenResponse response = restClient.post()
                .uri("https://login.microsoftonline.com/" + graph.tenantId() + "/oauth2/v2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || isBlank(response.accessToken())) {
            throw new IllegalStateException("Microsoft Entra ID no devolvió un access token para Graph");
        }
        return response.accessToken();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record TokenResponse(
            @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken) {
    }

    private record SendMailRequest(GraphMessage message, boolean saveToSentItems) {
    }

    private record GraphMessage(String subject, GraphBody body, List<GraphRecipient> toRecipients) {
    }

    private record GraphBody(String contentType, String content) {
    }

    private record GraphRecipient(GraphEmailAddress emailAddress) {
    }

    private record GraphEmailAddress(String address) {
    }
}
