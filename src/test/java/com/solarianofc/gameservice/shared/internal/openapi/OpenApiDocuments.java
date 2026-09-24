package com.solarianofc.gameservice.shared.internal.openapi;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/** HTTP access to the main port and the generated OpenAPI document for the tests of this package. */
final class OpenApiDocuments {

    private OpenApiDocuments() {}

    static RestTestClient client(int serverPort) {
        return RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + serverPort)
                .build();
    }

    static DocumentContext fetch(int serverPort) {
        String body = client(serverPort)
                .get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        return JsonPath.parse(body);
    }
}
