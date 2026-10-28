package com.osinergmin.osinergmin.alfresco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.osinergmin.osinergmin.alfresco.AlfrescoNodo.Nodo;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Llamadas a la API de Alfresco contra un servidor simulado (no necesita Alfresco instalado). */
class AlfrescoClientTest {

    private static final String API = "http://alf:8081/alfresco/api/-default-/public/alfresco/versions/1";
    private static final String BASIC = "Basic " + Base64.getEncoder().encodeToString("osi:clave".getBytes(StandardCharsets.UTF_8));

    private MockRestServiceServer servidor;
    private AlfrescoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        client = new AlfrescoClient(new AlfrescoProperties("http://alf:8081/alfresco/", "osi", "clave", null), builder);
    }

    private static String entrada(String json) {
        return "{\"entry\":" + json + "}";
    }

    @Test
    void ubicaLaCarpetaDeReportesDentroDeCompartidoConLasCredenciales() {
        servidor.expect(requestTo(API + "/nodes/-shared-?relativePath=Reportes"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", BASIC))
                .andRespond(withSuccess(entrada("{\"id\":\"R1\",\"name\":\"Reportes\",\"isFolder\":true}"), MediaType.APPLICATION_JSON));

        assertThat(client.carpetaReportes()).isEqualTo("R1");
        servidor.verify();
    }

    @Test
    void siLaCarpetaDeReportesNoExisteDa503ConMensajeClaro() {
        servidor.expect(requestTo(API + "/nodes/-shared-?relativePath=Reportes")).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.carpetaReportes())
                .isInstanceOf(AlfrescoNoDisponibleException.class)
                .hasMessageContaining("Reportes");
    }

    @Test
    void creaLaSubcarpetaSiNoExiste() {
        servidor.expect(requestTo(API + "/nodes/R1?relativePath=2026")).andRespond(withResourceNotFound());
        servidor.expect(requestTo(API + "/nodes/R1/children"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.name").value("2026"))
                .andExpect(jsonPath("$.nodeType").value("cm:folder"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(entrada("{\"id\":\"A1\",\"name\":\"2026\",\"isFolder\":true}")));

        assertThat(client.obtenerOCrearCarpeta("R1", "2026")).isEqualTo("A1");
        servidor.verify();
    }

    @Test
    void reutilizaLaSubcarpetaSiYaExiste() {
        servidor.expect(requestTo(API + "/nodes/R1?relativePath=2026"))
                .andRespond(withSuccess(entrada("{\"id\":\"A1\",\"name\":\"2026\",\"isFolder\":true}"), MediaType.APPLICATION_JSON));

        assertThat(client.obtenerOCrearCarpeta("R1", "2026")).isEqualTo("A1");
        servidor.verify(); // no hubo POST
    }

    @Test
    void subeElArchivoComoMultipartYLeeLaRespuesta() {
        servidor.expect(requestTo(API + "/nodes/M1/children"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(entrada("""
                        {"id":"D1","name":"seguimiento.pdf","isFile":true,"createdAt":"2026-10-27T21:34:07.210+0000",
                         "content":{"mimeType":"application/pdf","sizeInBytes":1234},
                         "properties":{"cm:title":"Reporte","cm:description":"Filtros"},"otroCampo":1}""")));

        Nodo nodo = client.subir("M1", "seguimiento.pdf", new byte[] {1, 2, 3}, MediaType.APPLICATION_PDF, "Reporte", "Filtros");

        assertThat(nodo.id()).isEqualTo("D1");
        assertThat(nodo.creado()).isEqualTo(Instant.parse("2026-10-27T21:34:07.210Z"));
        assertThat(nodo.content().sizeInBytes()).isEqualTo(1234L);
        assertThat(nodo.propiedad("cm:title")).isEqualTo("Reporte");
        servidor.verify();
    }

    @Test
    void credencialesRechazadasDan503() {
        servidor.expect(requestTo(API + "/nodes/-shared-?relativePath=Reportes")).andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.carpetaReportes())
                .isInstanceOf(AlfrescoNoDisponibleException.class)
                .hasMessageContaining("credenciales");
    }

    @Test
    void sinConfiguracionNoLlamaAAlfrescoYDa503() {
        AlfrescoClient sinConfigurar = new AlfrescoClient(new AlfrescoProperties("", null, null, null), RestClient.builder());

        assertThat(sinConfigurar.configurado()).isFalse();
        assertThatThrownBy(sinConfigurar::carpetaReportes)
                .isInstanceOf(AlfrescoNoDisponibleException.class)
                .hasMessageContaining("APP_ALFRESCO_URL");
    }
}
